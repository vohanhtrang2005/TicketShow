package com.waterpark.tickershow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ParticipantResponse {

    private Long userId;

    private String fullName;

    private String roleName;
}