package com.waterpark.tickershow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String senderName;

    private String senderRole;

    private String content;

    private LocalDateTime createdAt;
}