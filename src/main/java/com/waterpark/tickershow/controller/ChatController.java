package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.SendMessageRequest;
import com.waterpark.tickershow.dto.request.StartConversationRequest;
import com.waterpark.tickershow.dto.response.ChatMessageResponse;
import com.waterpark.tickershow.dto.response.ConversationResponse;
import com.waterpark.tickershow.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations/start")
    public ResponseEntity<ChatMessageResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.startConversation(request, authentication.getName()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(Authentication authentication) {
        return ResponseEntity.ok(chatService.getMyConversations(authentication.getName()));
    }

    @GetMapping("/conversations/open")
    public ResponseEntity<List<ConversationResponse>> getOpenConversations(Authentication authentication) {
        return ResponseEntity.ok(chatService.getOpenConversations(authentication.getName()));
    }

    @GetMapping("/conversations/assigned-to-me")
    public ResponseEntity<List<ConversationResponse>> getAssignedToMe(Authentication authentication) {
        return ResponseEntity.ok(chatService.getAssignedToMe(authentication.getName()));
    }

    @PatchMapping("/conversations/{conversationId}/assign")
    public ResponseEntity<ConversationResponse> assignConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.assignConversation(conversationId, authentication.getName()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, page, size, authentication.getName()));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, request, authentication.getName()));
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatService.markAsRead(conversationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/conversations/{conversationId}/close")
    public ResponseEntity<ConversationResponse> closeConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.closeConversation(conversationId, authentication.getName()));
    }
}