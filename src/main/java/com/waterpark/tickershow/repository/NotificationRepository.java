package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy toàn bộ thông báo của user, mới nhất trước
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Chỉ lấy thông báo chưa đọc
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Đếm số thông báo chưa đọc (hiển thị badge trên UI)
    long countByUserIdAndIsReadFalse(Long userId);

    // Đánh dấu tất cả thông báo của user là đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}
