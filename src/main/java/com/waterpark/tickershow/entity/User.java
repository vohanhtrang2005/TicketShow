package com.waterpark.tickershow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.waterpark.tickershow.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // ManyToOne: nhiều user thuộc một role
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ======= Back-references (tránh JSON recursion với @JsonIgnore) =======

    // Show do user này tạo (với tư cách Operator)
    @JsonIgnore
    @OneToMany(mappedBy = "createdBy")
    @Builder.Default
    private List<Show> createdShows = new ArrayList<>();

    // Show do user này xét duyệt (với tư cách Manager)
    @JsonIgnore
    @OneToMany(mappedBy = "reviewedBy")
    @Builder.Default
    private List<Show> reviewedShows = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "staff")
    @Builder.Default
    private List<StaffAssignment> assignments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "staff")
    @Builder.Default
    private List<CheckIn> checkIns = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "staff")
    @Builder.Default
    private List<CheckInHistory> checkInHistories = new ArrayList<>();

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
