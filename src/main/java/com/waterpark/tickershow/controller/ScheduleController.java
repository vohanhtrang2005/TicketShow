package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.CreateScheduleRequest;
import com.waterpark.tickershow.dto.request.ReviewScheduleRequest;
import com.waterpark.tickershow.dto.request.UpdateScheduleRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.dto.response.ScheduleResponse;
import com.waterpark.tickershow.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ─── Public ───────────────────────────────────────────────────────────────

    /** Public: get approved+upcoming/ongoing schedules for a show */
    @GetMapping("/show/{showId}/available")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getAvailableSchedules(
            @PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getAvailableSchedulesByShow(showId)));
    }

    /** Public/Internal: get all schedules for a show */
    @GetMapping("/show/{showId}")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByShow(
            @PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getSchedulesByShow(showId)));
    }

    /** Public: get schedule detail */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getScheduleById(id)));
    }

    // ─── Operator ─────────────────────────────────────────────────────────────

    /** Operator: create schedule for a show */
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateScheduleRequest req) {
        ScheduleResponse response = scheduleService.createSchedule(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Operator: update a schedule (only UPCOMING + not yet APPROVED) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @PathVariable Long id,
            @RequestBody UpdateScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch trình thành công",
                scheduleService.updateSchedule(id, req)));
    }

    /** Operator/Manager: cancel a schedule */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> cancelSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã hủy lịch trình",
                scheduleService.cancelSchedule(id)));
    }

    // ─── Manager ─────────────────────────────────────────────────────────────

    /** Manager: list all schedules pending approval (across all shows) */
    @GetMapping("/manage/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getPendingSchedules() {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getPendingSchedules()));
    }

    /** Manager: list pending schedules for a specific show */
    @GetMapping("/show/{showId}/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getPendingSchedulesForShow(
            @PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.getSchedulesPendingForShow(showId)));
    }

    /** Manager: approve or reject individual schedule */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ScheduleResponse>> reviewSchedule(
            @PathVariable Long id,
            @RequestBody ReviewScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Đã xét duyệt lịch trình",
                scheduleService.reviewSchedule(id, req)));
    }
}
