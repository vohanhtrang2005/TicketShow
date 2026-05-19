package com.waterpark.tickershow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: nhiều check-in record cho một ticket (lịch sử quét lại nếu fail)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // ManyToOne: staff thực hiện check-in
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    // VD: "SUCCESS", "FAILED", "ALREADY_USED", "INVALID"
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUCCESS";

    // Back-reference: tránh JSON recursion
    @JsonIgnore
    @OneToMany(mappedBy = "checkIn", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CheckInHistory> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (checkInTime == null) {
            checkInTime = LocalDateTime.now();
        }
    }
}
