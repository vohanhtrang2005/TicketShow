package com.waterpark.tickershow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.waterpark.tickershow.dto.response.ScheduleResponse.ZonePriceInfo;

@Data
@Builder
public class ShowResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private ShowTypeInfo showType;
    private UserInfo createdBy;
    private UserInfo reviewedBy;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private List<ScheduleSummary> schedules;

    @Data
    @Builder
    public static class ShowTypeInfo {
        private Long id;
        private String name;
        private String description;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
    }

     @Data
    @Builder
    public static class ZonePriceInfo {
        private Long zoneId;
        private String zoneName;
        private Double price;
        private Integer availableCapacity;
    }
    @Data
    @Builder
    public static class ScheduleSummary {
        private Long id;
        private String venueName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String approvalStatus;
        private List<ZonePriceInfo> zones;
    }
}
