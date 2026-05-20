package com.waterpark.tickershow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ZoneResponse {
    private Long id;
    private Long venueId;
    private String venueName;
    private String name;
    private Integer capacity;
    private BigDecimal defaultPrice;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
