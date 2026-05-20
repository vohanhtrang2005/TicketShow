package com.waterpark.tickershow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ScheduleResponse {
    private Long id;
    private Long showId;
    private String showName;
    private String showType;
    private VenueInfo venue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private String status;           // UPCOMING / ONGOING / FINISHED / CANCELLED
    private String approvalStatus;   // PENDING_APPROVAL / APPROVED / REJECTED
    private String approvalNote;
    private UserInfo approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ZonePriceInfo> zonePrices;

    @Data
    @Builder
    public static class VenueInfo {
        private Long id;
        private String name;
        private String location;
        private Integer capacity;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    public static class ZonePriceInfo {
        private Long zoneId;
        private String zoneName;
        private Integer zoneCapacity;
        private BigDecimal price;
        private Integer bookedCount;       // Số vé đã đặt
        private Integer availableCount;    // Số vé còn lại
    }
}
