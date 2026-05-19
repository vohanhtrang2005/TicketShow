package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Ticket;
import com.waterpark.tickershow.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Tìm vé bằng QR code / ticketCode (dùng khi staff quét QR - BR13)
    Optional<Ticket> findByTicketCode(String ticketCode);

    // Lấy tất cả vé của một booking
    List<Ticket> findByBookingId(Long bookingId);

    // Lấy vé theo schedule (thống kê tham dự)
    List<Ticket> findByScheduleId(Long scheduleId);

    // Lấy vé theo schedule và trạng thái (đếm USED cho attendance report)
    List<Ticket> findByScheduleIdAndStatus(Long scheduleId, TicketStatus status);

    // Kiểm tra ticketCode đã tồn tại chưa (đảm bảo unique khi tạo)
    boolean existsByTicketCode(String ticketCode);
}
