package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Booking;
import com.waterpark.tickershow.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ─── Capacity Check (BR7) ─────────────────────────────────────────────────
    /**
     * Tính tổng số vé đã đặt trong một zone của một schedule.
     * Chỉ tính booking CONFIRMED (đã xác nhận), loại trừ CANCELLED.
     *
     * Dùng để kiểm tra: zone.capacity - totalBooked >= requestedQuantity
     */
    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b
            WHERE b.schedule.id = :scheduleId
              AND b.zone.id     = :zoneId
              AND b.status      = 'CONFIRMED'
            """)
    Integer sumConfirmedQuantityByScheduleAndZone(
            @Param("scheduleId") Long scheduleId,
            @Param("zoneId")     Long zoneId
    );

    // ─── Customer: xem lịch sử đặt vé của mình ───────────────────────────────

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByCustomerIdAndStatus(Long customerId, BookingStatus status);

    // ─── Operator / Manager: thống kê booking theo schedule ──────────────────

    List<Booking> findByScheduleId(Long scheduleId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.schedule.id = :scheduleId
              AND b.status = 'CONFIRMED'
            """)
    Long countConfirmedBySchedule(@Param("scheduleId") Long scheduleId);

    // ─── Kiểm tra customer đã đặt vé cho schedule này chưa ───────────────────

    boolean existsByCustomerIdAndScheduleIdAndStatusNot(
            Long customerId,
            Long scheduleId,
            BookingStatus status
    );
}
