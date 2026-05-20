package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateScheduleRequest {

    private Long venueId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;

    // Cập nhật giá zone (chỉ những zone được cung cấp mới được cập nhật)
    private List<ZonePriceEntry> zonePrices;

    @Data
    public static class ZonePriceEntry {
        private Long zoneId;

        @DecimalMin(value = "0.0", message = "Giá không được âm")
        private BigDecimal price;
    }
}
