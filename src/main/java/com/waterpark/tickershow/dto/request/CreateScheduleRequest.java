package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateScheduleRequest {

    @NotNull(message = "Show ID không được để trống")
    private Long showId;

    @NotNull(message = "Venue ID không được để trống")
    private Long venueId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    private String notes;

    /**
     * Giá vé cho từng zone trong lịch trình này.
     * Nếu không cung cấp → dùng giá mặc định của zone.
     * Với show miễn phí → hệ thống tự set = 0, bỏ qua giá trị này.
     */
    private List<ZonePriceEntry> zonePrices;

    @Data
    public static class ZonePriceEntry {
        @NotNull(message = "Zone ID không được để trống")
        private Long zoneId;

        @DecimalMin(value = "0.0", message = "Giá không được âm")
        private BigDecimal price;
    }
}
