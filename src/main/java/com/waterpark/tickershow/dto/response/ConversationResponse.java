package com.waterpark.tickershow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ConversationResponse {

    private Long id;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastMessageAt;

    private List<ParticipantResponse> participants;
    private Long assignedStaffId;


    private LocalDateTime closedAt;
    private Long closedById;
}