package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.CheckInHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInHistoryRepository extends JpaRepository<CheckInHistory, Long> {

    // Toàn bộ audit log của một vé (theo thứ tự thời gian)
    List<CheckInHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    // Audit log theo staff (Manager xem hoạt động nhân viên)
    List<CheckInHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    // Audit log theo check-in event
    List<CheckInHistory> findByCheckInId(Long checkInId);
}
