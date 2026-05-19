package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    // Lấy lần check-in thành công của một vé (để validate không check-in lần 2)
    Optional<CheckIn> findByTicketIdAndStatus(Long ticketId, String status);

    // Lịch sử check-in theo vé
    List<CheckIn> findByTicketIdOrderByCheckInTimeDesc(Long ticketId);

    // Check-in theo schedule (thống kê attendance)
    List<CheckIn> findByTicketScheduleId(Long scheduleId);

    // Đếm số lần check-in thành công của một schedule
    long countByTicketScheduleIdAndStatus(Long scheduleId, String status);
}
