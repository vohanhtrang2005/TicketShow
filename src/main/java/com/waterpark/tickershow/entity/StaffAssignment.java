package com.waterpark.tickershow.entity;

import com.waterpark.tickershow.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Thông tin phân công ──────────────────────────────────────────────────

    // Nhân viên được phân công hỗ trợ show
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    // Schedule (lịch trình cụ thể) mà staff được phân công hỗ trợ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    // Nhiệm vụ cụ thể được giao
    // VD: "Hướng dẫn viên", "Kiểm soát cổng vào", "Kỹ thuật âm thanh", "Y tế"
    @Column(name = "support_role", nullable = false, length = 150)
    private String supportRole;

    // ─── Phase 1: Operator đề xuất phân công ─────────────────────────────────

    // Operator (người tạo phân công này)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    // ─── Phase 2: Manager xét duyệt ─────────────────────────────────────────

    // Manager thực hiện review (null = chưa duyệt)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Ghi chú của Manager (lý do không duyệt, hoặc lưu ý khi duyệt)
    @Column(name = "manager_note", columnDefinition = "TEXT")
    private String managerNote;

    // Thời điểm Manager xử lý (duyệt hoặc từ chối)
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ─── Phase 3: Staff phản hồi ─────────────────────────────────────────────

    // Lý do Staff từ chối (VD: "Ốm", "Bận việc gia đình", "Trùng lịch")
    // Null nếu staff chưa phản hồi hoặc đã chấp nhận
    @Column(name = "staff_note", columnDefinition = "TEXT")
    private String staffNote;

    // Thời điểm Staff phản hồi (chấp nhận hoặc từ chối)
    @Column(name = "staff_responded_at")
    private LocalDateTime staffRespondedAt;

    // ─── Trạng thái tổng quan ─────────────────────────────────────────────────

    /**
     * Vòng đời trạng thái phân công:
     *
     * [Operator tạo: assignedBy, supportRole]
     * ↓
     * PENDING_APPROVAL
     * ↓ ↘
     * (Manager duyệt) (Manager từ chối + managerNote)
     * ↓ ↓
     * PENDING DISAPPROVED
     * ↙ ↘
     * (Staff nhận) (Staff từ chối + staffNote)
     * ↓ ↓
     * ACCEPTED REJECTED
     * ↓
     * (Show kết thúc)
     * ↓
     * COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING_APPROVAL;

    // ─── Timestamps ───────────────────────────────────────────────────────────

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
