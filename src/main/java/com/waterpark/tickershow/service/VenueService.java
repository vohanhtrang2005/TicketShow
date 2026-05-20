package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateVenueRequest;
import com.waterpark.tickershow.dto.request.UpdateVenueRequest;
import com.waterpark.tickershow.dto.response.VenueResponse;
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
public class VenueService {

    private final VenueRepository venueRepository;
    private final ZoneRepository zoneRepository;

    // ─── Public ───────────────────────────────────────────────────────────────

    public List<VenueResponse> getAllActiveVenues() {
        return venueRepository.findByActiveTrue().stream()
                .map(v -> toResponse(v, true))
                .collect(Collectors.toList());
    }

    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(v -> toResponse(v, true))
                .collect(Collectors.toList());
    }

    public VenueResponse getVenueById(Long id) {
        Venue venue = findById(id);
        return toResponse(venue, true);
    }

    // ─── Manager: CRUD ────────────────────────────────────────────────────────

    @Transactional
    public VenueResponse createVenue(CreateVenueRequest req) {
        if (venueRepository.existsByNameIgnoreCase(req.getName())) {
            throw new RuntimeException("Địa điểm với tên '" + req.getName() + "' đã tồn tại");
        }
        Venue venue = Venue.builder()
                .name(req.getName())
                .location(req.getLocation())
                .description(req.getDescription())
                .capacity(req.getCapacity())
                .active(true)
                .build();
        return toResponse(venueRepository.save(venue), false);
    }

    @Transactional
    public VenueResponse updateVenue(Long id, UpdateVenueRequest req) {
        Venue venue = findById(id);

        if (req.getName() != null && !req.getName().equals(venue.getName())) {
            if (venueRepository.existsByNameIgnoreCase(req.getName())) {
                throw new RuntimeException("Địa điểm với tên '" + req.getName() + "' đã tồn tại");
            }
            venue.setName(req.getName());
        }
        if (req.getLocation() != null)    venue.setLocation(req.getLocation());
        if (req.getDescription() != null) venue.setDescription(req.getDescription());
        if (req.getCapacity() != null)    venue.setCapacity(req.getCapacity());
        if (req.getActive() != null)      venue.setActive(req.getActive());

        return toResponse(venueRepository.save(venue), true);
    }

    @Transactional
    public void toggleVenueStatus(Long id) {
        Venue venue = findById(id);
        venue.setActive(!venue.getActive());
        venueRepository.save(venue);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    public Venue findById(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa điểm với ID: " + id));
    }

    public VenueResponse toResponse(Venue v, boolean includeZones) {
        List<ZoneResponse> zones = null;
        if (includeZones) {
            zones = zoneRepository.findByVenueId(v.getId()).stream()
                    .map(z -> ZoneResponse.builder()
                            .id(z.getId())
                            .venueId(v.getId())
                            .venueName(v.getName())
                            .name(z.getName())
                            .capacity(z.getCapacity())
                            .defaultPrice(z.getDefaultPrice())
                            .description(z.getDescription())
                            .createdAt(z.getCreatedAt())
                            .updatedAt(z.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
        }
        return VenueResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .location(v.getLocation())
                .description(v.getDescription())
                .capacity(v.getCapacity())
                .active(v.getActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .zones(zones)
                .build();
    }
}
