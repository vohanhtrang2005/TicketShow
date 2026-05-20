package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.request.CreateZoneRequest;
import com.waterpark.tickershow.dto.request.UpdateZoneRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.dto.response.ZoneResponse;
import com.waterpark.tickershow.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues/{venueId}/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    /** Public: get all zones for a venue */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones(@PathVariable Long venueId) {
        return ResponseEntity.ok(ApiResponse.success(zoneService.getZonesByVenue(venueId)));
    }

    /** Public: get single zone detail */
    @GetMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneResponse>> getZone(@PathVariable Long venueId,
                                                             @PathVariable Long zoneId) {
        return ResponseEntity.ok(ApiResponse.success(zoneService.getZoneById(zoneId)));
    }

    /** Manager/Admin: create zone in venue */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
            @PathVariable Long venueId,
            @Valid @RequestBody CreateZoneRequest req) {
        ZoneResponse response = zoneService.createZone(venueId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Manager/Admin: update zone */
    @PutMapping("/{zoneId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ZoneResponse>> updateZone(
            @PathVariable Long venueId,
            @PathVariable Long zoneId,
            @Valid @RequestBody UpdateZoneRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật zone thành công",
                zoneService.updateZone(zoneId, req)));
    }

    /** Manager/Admin: delete zone (only if no bookings) */
    @DeleteMapping("/{zoneId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable Long venueId,
                                                        @PathVariable Long zoneId) {
        zoneService.deleteZone(zoneId);
        return ResponseEntity.ok(ApiResponse.success("Xóa zone thành công", null));
    }
}
