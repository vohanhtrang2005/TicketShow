package com.waterpark.tickershow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.waterpark.tickershow.enums.ShowStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Vòng đời trạng thái show:
     *
     * [Operator tạo bản nháp]
     *       ↓
     *     DRAFT  ──(submit)──→  PENDING_APPROVAL
     *                                ↓           ↘
     *                    (Manager duyệt)    (Manager từ chối + rejectionReason)
     *                                ↓                    ↓
     *                           APPROVED          REVISION_REQUIRED
     *                                ↓                    ↓ (operator chỉnh sửa & submit lại)
     *                       (Manager publish)      PENDING_APPROVAL
     *                                ↓
     *                           PUBLISHED  ← khách hàng thấy show
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShowStatus status = ShowStatus.DRAFT;

    // ManyToOne: loại show (FREE_NO_REGISTRATION / FREE_WITH_REGISTRATION / PAID)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "show_type_id", nullable = false)
    private ShowType showType;

    // ─── Operator tạo show ───────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ─── Manager xét duyệt show ──────────────────────────────────────────────

    // Manager thực hiện review (null = chưa có ai review)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // Lý do Manager từ chối (bắt buộc điền khi status = REJECTED)
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Thời điểm Manager phê duyệt hoặc từ chối
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ─── Timestamps ──────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Back-references (tránh JSON recursion) ───────────────────────────────

    @JsonIgnore
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShowImage> images = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();

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

