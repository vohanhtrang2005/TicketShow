package com.waterpark.tickershow.service;
import com.waterpark.tickershow.dto.request.SendMessageRequest;
import com.waterpark.tickershow.dto.request.StartConversationRequest;
import com.waterpark.tickershow.dto.response.ChatEventResponse;
import com.waterpark.tickershow.dto.response.ChatMessageResponse;
import com.waterpark.tickershow.dto.response.ConversationResponse;
import com.waterpark.tickershow.dto.response.ParticipantResponse;
import com.waterpark.tickershow.entity.ChatMessage;
import com.waterpark.tickershow.entity.Conversation;
import com.waterpark.tickershow.entity.ConversationParticipant;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.enums.ChatEventType;
import com.waterpark.tickershow.enums.ConversationStatus;
import com.waterpark.tickershow.enums.RoleName;
import com.waterpark.tickershow.repository.ChatMessageRepository;
import com.waterpark.tickershow.repository.ConversationParticipantRepository;
import com.waterpark.tickershow.repository.ConversationRepository;
import com.waterpark.tickershow.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    

    public ChatService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            ChatMessageRepository messageRepository, 
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay user"));
    }

    private RoleName getRoleName(User user) {
        // Chuyển RoleName thành String rồi mới dùng replace()
String role = user.getRole().getName().name().replace("ROLE_", "");
        return RoleName.valueOf(role);
    }

    

    private boolean isStaff(User user) {
        RoleName roleName = getRoleName(user);
        return roleName == RoleName.STAFF
                || roleName == RoleName.MANAGER
                || roleName == RoleName.ADMIN;
    }

    private boolean isCustomer(User user) {
        return getRoleName(user) == RoleName.CUSTOMER;
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        List<ParticipantResponse> participants = participantRepository.findByConversationId(conversation.getId())
                .stream()
                .map(participant -> ParticipantResponse.builder()
                        .userId(participant.getUser().getId())
                        .fullName(participant.getUser().getFullName())
                        .roleName(participant.getRoleName().name())
                        .build())
                .toList();

        User assignedStaff = conversation.getAssignedStaff();
        User closedBy = conversation.getClosedBy();

        return ConversationResponse.builder()
                .id(conversation.getId())
                .status(conversation.getStatus().name())
                .assignedStaffId(assignedStaff != null ? assignedStaff.getId() : null)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .closedAt(conversation.getClosedAt())
                .closedById(closedBy != null ? closedBy.getId() : null)
                .participants(participants)
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        User sender = message.getSender();

        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderRole(sender.getRole().getName().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void ensureParticipant(Conversation conversation, User user, RoleName roleName) {
        boolean exists = participantRepository.existsByConversationIdAndUserId(
                conversation.getId(),
                user.getId()
        );

        if (!exists) {
            ConversationParticipant participant = ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(user)
                    .roleName(roleName)
                    .build();
            participantRepository.save(participant);
        }
    }

    private void ensureCanRead(Conversation conversation, User user) {
        if (isStaff(user)) {
            if (conversation.getStatus() == ConversationStatus.OPEN) {
                return;
            }

            if (conversation.getAssignedStaff() != null
                    && conversation.getAssignedStaff().getId().equals(user.getId())) {
                return;
            }
        }

        boolean participant = participantRepository.existsByConversationIdAndUserId(
                conversation.getId(),
                user.getId()
        );

        if (!participant) {
            throw new RuntimeException("Ban khong co quyen xem cuoc tro chuyen nay");
        }
    }

    private void ensureCanSend(Conversation conversation, User user) {
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new RuntimeException("Cuoc tro chuyen da ket thuc");
        }

        if (isStaff(user)) {
            if (conversation.getStatus() != ConversationStatus.ASSIGNED
                    || conversation.getAssignedStaff() == null
                    || !conversation.getAssignedStaff().getId().equals(user.getId())) {
                throw new RuntimeException("Staff can tiep nhan truoc khi tra loi");
            }
            return;
        }

        boolean participant = participantRepository.existsByConversationIdAndUserId(
                conversation.getId(),
                user.getId()
        );

        if (!participant) {
            throw new RuntimeException("Ban khong co quyen gui tin nhan");
        }
    }


    @Transactional
    public ChatMessageResponse startConversation(StartConversationRequest request, String email) {
        User currentUser = getCurrentUser(email);

        if (!isCustomer(currentUser)) {
            throw new RuntimeException("Chi customer moi duoc tao yeu cau ho tro");
        }

        // 1. Tao conversation OPEN
        Conversation conversation = Conversation.builder()
                .status(ConversationStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        conversation = conversationRepository.save(conversation);

        // 2. Them participant customer
        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(conversation)
                .user(currentUser)
                .roleName(RoleName.CUSTOMER)
                .build();
        participantRepository.save(participant);

        // 3. Tao message dau tien
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(request.getFirstMessageContent())
                .createdAt(LocalDateTime.now())
                .build();
        message = messageRepository.save(message);

  ConversationResponse convResponse = toConversationResponse(conversation);
    ChatMessageResponse msgResponse = toMessageResponse(message);

// 1. Phát sự kiện tạo phòng cho Staff thấy ở queue chung
    publishToOpenQueue(ChatEventResponse.builder()
            .type(ChatEventType.CONVERSATION_CREATED)
            .conversationId(conversation.getId())
            .conversation(convResponse)
            .actorId(currentUser.getId())
            .build());
    // 2. Phát tin nhắn đầu tiên vào phòng chat riêng
    publishToConversation(conversation.getId(), ChatEventResponse.builder()
            .type(ChatEventType.MESSAGE_CREATED)
            .conversationId(conversation.getId())
            .message(msgResponse)
            .actorId(currentUser.getId())
            .build());
    return msgResponse;
    }

    @Transactional
    public List<ConversationResponse> getMyConversations(String email) {
        User currentUser = getCurrentUser(email);

        List<Conversation> conversations = participantRepository
                .findConversationsByUserIdOrderByLastMessageAtDesc(currentUser.getId());

        return conversations.stream()
                .sorted(Comparator
                        .comparing((Conversation c) -> c.getStatus() == ConversationStatus.ASSIGNED ? 0 : 1)
                        .thenComparing(c -> c.getStatus() == ConversationStatus.OPEN ? 0 : 1)
                        .thenComparing(
                                Conversation::getLastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public List<ConversationResponse> getOpenConversations(String email) {
        User currentUser = getCurrentUser(email);
        if (!isStaff(currentUser)) {
            throw new RuntimeException("Chi staff moi duoc xem yeu cau moi");
        }

        return conversationRepository.findByStatusOrderByLastMessageAtDesc(ConversationStatus.OPEN)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public List<ConversationResponse> getAssignedToMe(String email) {
        User currentUser = getCurrentUser(email);
        if (!isStaff(currentUser)) {
            throw new RuntimeException("Chi staff moi duoc xem danh sach dang ho tro");
        }

        return conversationRepository
                .findByAssignedStaffIdAndStatusOrderByLastMessageAtDesc(
                        currentUser.getId(),
                        ConversationStatus.ASSIGNED
                )
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public ConversationResponse assignConversation(Long conversationId, String email) {
        User currentUser = getCurrentUser(email);
        if (!isStaff(currentUser)) {
            throw new RuntimeException("Chi staff moi duoc tiep nhan");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay conversation"));

        if (conversation.getStatus() != ConversationStatus.OPEN) {
            throw new RuntimeException("Conversation da duoc tiep nhan hoac da dong");
        }

        conversation.setStatus(ConversationStatus.ASSIGNED);
        conversation.setAssignedStaff(currentUser);
        conversationRepository.save(conversation);

        ensureParticipant(conversation, currentUser, getRoleName(currentUser));

           ConversationResponse response = toConversationResponse(conversation);

    ChatEventResponse event = ChatEventResponse.builder()
            .type(ChatEventType.CONVERSATION_ASSIGNED)
            .conversationId(conversation.getId())
            .conversation(response)
            .actorId(currentUser.getId())
            .build();

    // Phát cho các Staff khác để xóa phòng này khỏi danh sách "Yêu cầu mới"
    publishToOpenQueue(event);
    // Phát vào phòng chat riêng để báo cho Customer biết đã có Staff tiếp nhận
    publishToConversation(conversation.getId(), event);

    return response;
    }

    @Transactional
    public Page<ChatMessageResponse> getMessages(Long conversationId, int page, int size, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay conversation"));

        ensureCanRead(conversation, currentUser);

        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long conversationId, SendMessageRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay conversation"));

        ensureCanSend(conversation, currentUser);

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(request.getContent())
                .build();
        message = messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);
    ChatMessageResponse response = toMessageResponse(message);

    // Phát tin nhắn mới vào phòng chat riêng để bên kia nhận được ngay lập tức
    publishToConversation(conversationId, ChatEventResponse.builder()
            .type(ChatEventType.MESSAGE_CREATED)
            .conversationId(conversationId)
            .message(response)
            .actorId(currentUser.getId())
            .build());

    return response;
    }

    @Transactional
    public void markAsRead(Long conversationId, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay conversation"));

        ensureCanRead(conversation, currentUser);
    }

    @Transactional
    public ConversationResponse closeConversation(Long conversationId, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay conversation"));

        ensureCanRead(conversation, currentUser);

        if (conversation.getStatus() != ConversationStatus.CLOSED) {
            conversation.setStatus(ConversationStatus.CLOSED);
            conversation.setClosedAt(LocalDateTime.now());
            conversation.setClosedBy(currentUser);
            conversationRepository.save(conversation);
        }
    ConversationResponse response = toConversationResponse(conversation);

    ChatEventResponse event = ChatEventResponse.builder()
            .type(ChatEventType.CONVERSATION_CLOSED)
            .conversationId(conversation.getId())
            .conversation(response)
            .actorId(currentUser.getId())
            .build();

    // Phát đi để Staff page và Customer widget tự đóng giao diện chat
    publishToOpenQueue(event);
    publishToConversation(conversation.getId(), event);

    return response;
    }

    private void publishToOpenQueue(ChatEventResponse event) {
    messagingTemplate.convertAndSend("/topic/chat/open", event);
}

private void publishToConversation(Long conversationId, ChatEventResponse event) {
    messagingTemplate.convertAndSend("/topic/chat/conversations/" + conversationId, event);
}
}