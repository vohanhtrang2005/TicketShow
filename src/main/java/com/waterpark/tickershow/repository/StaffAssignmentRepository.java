package com.waterpark.tickershow.repository;

import com.waterpark.tickershow.entity.StaffAssignment;
import com.waterpark.tickershow.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    // ─── Staff: xem các lịch trình mình được phân công ───────────────────────

    List<StaffAssignment> findByStaffId(Long staffId);

    List<StaffAssignment> findByStaffIdAndStatus(Long staffId, AssignmentStatus status);

    // Lịch trình staff được phân công (chỉ ACCEPTED để lên lịch làm việc)
    @Query("""
            SELECT sa FROM StaffAssignment sa
            WHERE sa.staff.id = :staffId
              AND sa.status = 'ACCEPTED'
            ORDER BY sa.schedule.startTime ASC
            """)
    List<StaffAssignment> findAcceptedAssignmentsByStaff(@Param("staffId") Long staffId);

    // ─── Operator: xem phân công do mình đề xuất ─────────────────────────────

    List<StaffAssignment> findByAssignedById(Long operatorId);

    List<StaffAssignment> findByAssignedByIdAndStatus(Long operatorId, AssignmentStatus status);

    // ─── Manager: xem phân công chờ duyệt ────────────────────────────────────

    List<StaffAssignment> findByStatus(AssignmentStatus status);

    // ─── Manager: xem phân công theo schedule ────────────────────────────────

    List<StaffAssignment> findByScheduleId(Long scheduleId);

    @Query("""
            SELECT sa FROM StaffAssignment sa
            WHERE sa.schedule.id = :scheduleId
              AND sa.status IN ('PENDING_APPROVAL', 'PENDING', 'ACCEPTED')
            """)
    List<StaffAssignment> findActiveAssignmentsBySchedule(@Param("scheduleId") Long scheduleId);

    // ─── Kiểm tra staff đã được phân công cho schedule này chưa ──────────────

    boolean existsByStaffIdAndScheduleIdAndStatusNot(
            Long staffId,
            Long scheduleId,
            AssignmentStatus status
    );
}
