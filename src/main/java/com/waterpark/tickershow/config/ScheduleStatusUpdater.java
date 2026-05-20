package com.waterpark.tickershow.config;

import com.waterpark.tickershow.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler tự động cập nhật trạng thái lịch trình theo thời gian thực (BR12):
 * UPCOMING → ONGOING → FINISHED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleStatusUpdater {

    private final ScheduleService scheduleService;

    /**
     * Chạy mỗi phút để cập nhật trạng thái schedule.
     * UPCOMING  → ONGOING  : khi startTime <= now
     * ONGOING   → FINISHED : khi endTime   <= now
     */
    @Scheduled(fixedRate = 60_000) // every 1 minute
    public void updateScheduleStatuses() {
        try {
            scheduleService.autoUpdateScheduleStatuses();
            log.debug("✅ Schedule status auto-update completed");
        } catch (Exception e) {
            log.error("❌ Error during schedule status auto-update: {}", e.getMessage(), e);
        }
    }
}
