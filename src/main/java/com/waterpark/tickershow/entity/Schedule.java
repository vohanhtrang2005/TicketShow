package com.waterpark.tickershow.entity;

import com.waterpark.tickershow.enums.ScheduleApprovalStatus;
import com.waterpark.tickershow.enums.ScheduleStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "schedules",
        indexes = {
                @Index(name = "idx_schedule_venue_time", columnList = "venue_id, start_time, end_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: nhiều schedule thuộc một show
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    // ManyToOne: nhiều schedule dùng một venue
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Ghi chú thêm cho lịch trình (tùy chọn)
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ─── Trạng thái duyệt (Manager duyệt từng schedule riêng) ────────────────

    /**
     * Vòng đời duyệt:
     * [Operator tạo] → PENDING_APPROVAL
     * → Manager duyệt → APPROVED
     * → Manager từ chối → REJECTED
     * Mỗi schedule được duyệt độc lập, không ảnh hưởng các schedule khác.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ScheduleApprovalStatus approvalStatus = ScheduleApprovalStatus.PENDING_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ─── Trạng thái vận hành (tự động cập nhật theo thời gian) ───────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.UPCOMING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Zone prices per schedule (operator có thể set giá riêng) ────────────
    @JsonIgnore
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScheduleZonePrice> zonePrices = new ArrayList<>();

    // Back-references
    @JsonIgnore
    @OneToMany(mappedBy = "schedule")
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "schedule")
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "schedule")
    @Builder.Default
    private List<StaffAssignment> staffAssignments = new ArrayList<>();

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
