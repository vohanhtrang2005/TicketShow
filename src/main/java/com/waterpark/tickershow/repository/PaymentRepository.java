package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Payment;
import com.waterpark.tickershow.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Tìm payment theo booking (1 booking có thể có nhiều lần thử thanh toán)
    List<Payment> findByBookingId(Long bookingId);

    // Tìm payment SUCCESS của một booking
    Optional<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    // Tìm theo mã giao dịch (callback từ MOMO/VNPAY)
    Optional<Payment> findByTransactionCode(String transactionCode);

    // ─── Manager: Báo cáo doanh thu ──────────────────────────────────────────

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND p.booking.schedule.show.id = :showId
            """)
    java.math.BigDecimal sumRevenueByShow(@Param("showId") Long showId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND p.booking.schedule.id = :scheduleId
            """)
    java.math.BigDecimal sumRevenueBySchedule(@Param("scheduleId") Long scheduleId);
}
