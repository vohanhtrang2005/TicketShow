package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Schedule;
import com.waterpark.tickershow.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // ─── Conflict Detection (BR8 / BR17) ─────────────────────────────────────
    /**
     * Kiểm tra xung đột địa điểm:
     * Một venue không thể có 2 schedule chồng lên nhau về thời gian.
     *
     * Điều kiện chồng: newStart < existing.end AND newEnd > existing.start
     *
     * @param venueId   Venue cần kiểm tra
     * @param startTime Thời gian bắt đầu của schedule mới
     * @param endTime   Thời gian kết thúc của schedule mới
     * @param excludeId ID của schedule đang edit (để loại trừ chính nó), dùng 0L khi tạo mới
     */
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.venue.id = :venueId
              AND s.startTime < :endTime
              AND s.endTime   > :startTime
              AND s.id       != :excludeId
              AND s.status NOT IN ('CANCELLED')
            """)
    List<Schedule> findConflictingSchedules(
            @Param("venueId")    Long venueId,
            @Param("startTime")  LocalDateTime startTime,
            @Param("endTime")    LocalDateTime endTime,
            @Param("excludeId")  Long excludeId
    );

    // ─── Cron job: tự động cập nhật trạng thái theo thời gian ───────────────

    // Tìm schedule UPCOMING đã đến giờ bắt đầu → chuyển sang ONGOING
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.status = 'UPCOMING'
              AND s.startTime <= :now
            """)
    List<Schedule> findSchedulesToMarkOngoing(@Param("now") LocalDateTime now);

    // Tìm schedule ONGOING đã qua giờ kết thúc → chuyển sang FINISHED
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.status = 'ONGOING'
              AND s.endTime <= :now
            """)
    List<Schedule> findSchedulesToMarkFinished(@Param("now") LocalDateTime now);

    // ─── Operator / Manager: xem schedule theo show ───────────────────────────

    List<Schedule> findByShowId(Long showId);

    List<Schedule> findByShowIdAndStatus(Long showId, ScheduleStatus status);

    // ─── Customer: xem schedule khả dụng của một show ────────────────────────

    @Query("""
            SELECT s FROM Schedule s
            WHERE s.show.id = :showId
              AND s.status IN ('UPCOMING', 'ONGOING')
            ORDER BY s.startTime ASC
            """)
    List<Schedule> findAvailableSchedulesByShow(@Param("showId") Long showId);

    // ─── Venue: xem toàn bộ schedule của một địa điểm ────────────────────────

    List<Schedule> findByVenueId(Long venueId);
}
