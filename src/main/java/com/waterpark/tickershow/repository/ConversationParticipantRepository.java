package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Conversation;
import com.waterpark.tickershow.entity.ConversationParticipant;
import com.waterpark.tickershow.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    // Lấy toàn bộ người tham gia trong một cuộc trò chuyện
    List<ConversationParticipant> findByConversationId(Long conversationId);

    // Kiểm tra xem một User có thuộc một cuộc trò chuyện cụ thể hay không (Dùng để kiểm tra quyền đọc/ghi chat)
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    // Tìm thông tin participant cụ thể của một User trong một cuộc trò chuyện
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    //Va query theo status co sort:
@Query("""
        SELECT cp.conversation FROM ConversationParticipant cp
        WHERE cp.user.id = :userId
          AND cp.conversation.status = :status
        ORDER BY cp.conversation.lastMessageAt DESC
        """)
List<Conversation> findConversationsByUserIdAndStatusOrderByLastMessageAtDesc(
        @Param("userId") Long userId,
        @Param("status") ConversationStatus status
);

        // hem query lay tat ca conversation cua user, bao gom ca `CLOSED`, sort theo lastMessageAt:
@Query("""
        SELECT cp.conversation FROM ConversationParticipant cp
        WHERE cp.user.id = :userId
        ORDER BY cp.conversation.lastMessageAt DESC
        """)
List<Conversation> findConversationsByUserIdOrderByLastMessageAtDesc(
        @Param("userId") Long userId);
//Neu can query active rieng:
@Query("""
        SELECT cp.conversation FROM ConversationParticipant cp
        WHERE cp.user.id = :userId
          AND cp.conversation.status <> 'CLOSED'
        ORDER BY cp.conversation.lastMessageAt DESC
        """)
List<Conversation> findActiveConversationsByUserId(@Param("userId") Long userId);

}