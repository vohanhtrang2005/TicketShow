package com.waterpark.tickershow.dto.response;

import com.waterpark.tickershow.enums.ChatEventType; // Import enum vừa tạo
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ChatEventResponse {

    private ChatEventType type; // 👈 Thay đổi từ String sang ChatEventType ở đây

    private Long conversationId;

    private ConversationResponse conversation;

    private ChatMessageResponse message;

    private Long actorId;
}