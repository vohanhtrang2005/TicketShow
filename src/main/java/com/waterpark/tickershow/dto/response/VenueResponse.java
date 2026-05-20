package com.waterpark.tickershow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VenueResponse {
    private Long id;
    private String name;
    private String location;
    private String description;
    private Integer capacity;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ZoneResponse> zones;
}
