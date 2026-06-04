package com.waterpark.tickershow.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TicketResponse {
    private Long ticketId;
    private String ticketCode;
    private String qrCode;
    private String zoneName;
}