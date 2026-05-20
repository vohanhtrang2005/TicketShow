package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateZoneRequest;
import com.waterpark.tickershow.dto.request.UpdateZoneRequest;
import com.waterpark.tickershow.dto.response.ZoneResponse;
import com.waterpark.tickershow.entity.Venue;
import com.waterpark.tickershow.entity.Zone;
import com.waterpark.tickershow.repository.VenueRepository;
import com.waterpark.tickershow.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final VenueRepository venueRepository;

    public List<ZoneResponse> getZonesByVenue(Long venueId) {
        getVenue(venueId); // verify venue exists
        return zoneRepository.findByVenueId(venueId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ZoneResponse getZoneById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ZoneResponse createZone(Long venueId, CreateZoneRequest req) {
        Venue venue = getVenue(venueId);

        if (zoneRepository.existsByVenueIdAndNameIgnoreCase(venueId, req.getName())) {
            throw new RuntimeException("Zone '" + req.getName() + "' đã tồn tại trong địa điểm này");
        }

        Zone zone = Zone.builder()
                .venue(venue)
                .name(req.getName())
                .capacity(req.getCapacity())
                .defaultPrice(req.getDefaultPrice() != null ? req.getDefaultPrice() : java.math.BigDecimal.ZERO)
                .description(req.getDescription())
                .build();

        return toResponse(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneResponse updateZone(Long id, UpdateZoneRequest req) {
        Zone zone = findById(id);

        if (req.getName() != null && !req.getName().equals(zone.getName())) {
            if (zoneRepository.existsByVenueIdAndNameIgnoreCase(zone.getVenue().getId(), req.getName())) {
                throw new RuntimeException("Zone '" + req.getName() + "' đã tồn tại trong địa điểm này");
            }
            zone.setName(req.getName());
        }
        if (req.getCapacity() != null)     zone.setCapacity(req.getCapacity());
        if (req.getDefaultPrice() != null) zone.setDefaultPrice(req.getDefaultPrice());
        if (req.getDescription() != null)  zone.setDescription(req.getDescription());

        return toResponse(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(Long id) {
        Zone zone = findById(id);
        // Check if zone has any active bookings
        if (!zone.getBookings().isEmpty()) {
            throw new RuntimeException("Không thể xóa zone có lịch sử đặt vé");
        }
        zoneRepository.delete(zone);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public Zone findById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy zone với ID: " + id));
    }

    private Venue getVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa điểm với ID: " + venueId));
    }

    public ZoneResponse toResponse(Zone z) {
        return ZoneResponse.builder()
                .id(z.getId())
                .venueId(z.getVenue().getId())
                .venueName(z.getVenue().getName())
                .name(z.getName())
                .capacity(z.getCapacity())
                .defaultPrice(z.getDefaultPrice())
                .description(z.getDescription())
                .createdAt(z.getCreatedAt())
                .updatedAt(z.getUpdatedAt())
                .build();
    }
}
