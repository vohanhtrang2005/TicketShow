package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Show;
import com.waterpark.tickershow.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    // ─── Customer: Xem danh sách show công khai ──────────────────────────────

    // Chỉ lấy show APPROVED hoặc PUBLISHED (BR5)
    List<Show> findByStatusIn(List<ShowStatus> statuses);

    // ─── Operator: Xem show mình đã tạo ─────────────────────────────────────

    List<Show> findByCreatedById(Long userId);

    List<Show> findByCreatedByIdAndStatus(Long userId, ShowStatus status);

    // ─── Manager: Xem show chờ duyệt ─────────────────────────────────────────

    List<Show> findByStatus(ShowStatus status);

    // ─── Manager: Xem show mình đã duyệt ─────────────────────────────────────

    List<Show> findByReviewedById(Long managerId);

    // ─── Tìm kiếm show theo tên ───────────────────────────────────────────────

    @Query("""
            SELECT s FROM Show s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND s.status IN :statuses
            """)
    List<Show> searchByNameAndStatuses(
            @Param("keyword") String keyword,
            @Param("statuses") List<ShowStatus> statuses
    );
}
