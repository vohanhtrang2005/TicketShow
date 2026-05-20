package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.CreateShowRequest;
import com.waterpark.tickershow.dto.request.ReviewShowRequest;
import com.waterpark.tickershow.dto.request.UpdateShowRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.dto.response.ShowResponse;
import com.waterpark.tickershow.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    // ─── Public endpoints ─────────────────────────────────────────────────────

    /** Public: list all approved/published shows */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getPublicShows() {
        return ResponseEntity.ok(ApiResponse.success(showService.getPublicShows()));
    }

    /** Public: get show detail */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowById(id)));
    }

    /** Public: search shows by name */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> searchShows(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(showService.searchShows(keyword)));
    }

    // ─── Operator endpoints ───────────────────────────────────────────────────

    /** Operator: list own shows */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getMyShows() {
        return ResponseEntity.ok(ApiResponse.success(showService.getMyShows()));
    }

    /** Operator: create a new show (draft or submit) */
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(
            @Valid @RequestBody CreateShowRequest req) {
        ShowResponse response = showService.createShow(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Operator: update show info (DRAFT or REVISION_REQUIRED only) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(
            @PathVariable Long id,
            @RequestBody UpdateShowRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật show thành công",
                showService.updateShow(id, req)));
    }

    /** Operator: submit draft show for manager approval */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> submitForApproval(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi show để duyệt",
                showService.submitForApproval(id)));
    }

    /** Operator: save show as draft (keeps DRAFT status) */
    @PatchMapping("/{id}/draft")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> saveDraft(
            @PathVariable Long id,
            @RequestBody UpdateShowRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu bản nháp",
                showService.saveDraft(id, req)));
    }

    // ─── Manager endpoints ────────────────────────────────────────────────────

    /** Manager: view all shows */
    @GetMapping("/manage/all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShowsForManager() {
        return ResponseEntity.ok(ApiResponse.success(showService.getAllShowsForManager()));
    }

    /** Manager: view shows pending approval */
    @GetMapping("/manage/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getPendingShows() {
        return ResponseEntity.ok(ApiResponse.success(showService.getPendingApprovalShows()));
    }

    /** Manager: approve or reject a show */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> reviewShow(
            @PathVariable Long id,
            @RequestBody ReviewShowRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Đã xét duyệt show",
                showService.reviewShow(id, req)));
    }

    /** Manager: publish an approved show */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ShowResponse>> publishShow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã xuất bản show",
                showService.publishShow(id)));
    }
}
