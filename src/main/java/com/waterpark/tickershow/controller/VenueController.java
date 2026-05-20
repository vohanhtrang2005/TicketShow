package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.CreateVenueRequest;
import com.waterpark.tickershow.dto.request.UpdateVenueRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.dto.response.VenueResponse;
import com.waterpark.tickershow.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    /** Public: list active venues (with zones) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getActiveVenues() {
        return ResponseEntity.ok(ApiResponse.success(venueService.getAllActiveVenues()));
    }

    /** Manager/Admin: list ALL venues including inactive */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        return ResponseEntity.ok(ApiResponse.success(venueService.getAllVenues()));
    }

    /** Public: get venue detail */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(venueService.getVenueById(id)));
    }

    /** Manager/Admin: create venue */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody CreateVenueRequest req) {
        VenueResponse response = venueService.createVenue(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /** Manager/Admin: update venue */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVenueRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa điểm thành công",
                venueService.updateVenue(id, req)));
    }

    /** Manager/Admin: toggle venue active/inactive */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
        venueService.toggleVenueStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái địa điểm thành công", null));
    }
}
