package com.waterpark.tickershow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Giá vé của từng zone theo từng schedule.
 * Operator có thể set giá khác nhau cho mỗi schedule tại mỗi zone.
 * Nếu không set → dùng zone.price (giá mặc định của zone).
 * Với show miễn phí → price = 0 (tự động set khi tạo booking).
 */
@Entity
@Table(
        name = "schedule_zone_prices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_zone",
                        columnNames = {"schedule_id", "zone_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleZonePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    /**
     * Giá vé do operator set cho zone này trong schedule này.
     * - FREE_NO_REGISTRATION / FREE_WITH_REGISTRATION: 0
     * - PAID_WITH_REGISTRATION: do operator định giá
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // Số lượng vé còn lại (tính từ zone.capacity - booked)
    // Không lưu trữ - tính động trong service layer

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
