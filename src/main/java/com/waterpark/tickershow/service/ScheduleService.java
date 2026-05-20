package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateScheduleRequest;
import com.waterpark.tickershow.dto.request.ReviewScheduleRequest;
import com.waterpark.tickershow.dto.request.UpdateScheduleRequest;
import com.waterpark.tickershow.dto.response.ScheduleResponse;
import com.waterpark.tickershow.entity.*;
import com.waterpark.tickershow.enums.ScheduleApprovalStatus;
import com.waterpark.tickershow.enums.ScheduleStatus;
import com.waterpark.tickershow.enums.ShowTypeName;
import com.waterpark.tickershow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleZonePriceRepository scheduleZonePriceRepository;
    private final ShowRepository showRepository;
    private final VenueRepository venueRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;

    // ─── Public: view available schedules ────────────────────────────────────

    public List<ScheduleResponse> getSchedulesByShow(Long showId) {
        return scheduleRepository.findByShowId(showId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ScheduleResponse> getAvailableSchedulesByShow(Long showId) {
        return scheduleRepository.findAvailableSchedulesByShow(showId)
                .stream()
                .filter(s -> s.getApprovalStatus() == ScheduleApprovalStatus.APPROVED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ScheduleResponse getScheduleById(Long id) {
        return toResponse(findById(id));
    }

    // ─── Operator: create / update / cancel schedules ────────────────────────

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest req) {
        Show show = findShow(req.getShowId());
        Venue venue = findVenue(req.getVenueId());
        validateTimeRange(req.getStartTime(), req.getEndTime());
        checkVenueConflict(venue.getId(), req.getStartTime(), req.getEndTime(), 0L);

        Schedule schedule = Schedule.builder()
                .show(show)
                .venue(venue)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .notes(req.getNotes())
                .approvalStatus(ScheduleApprovalStatus.PENDING_APPROVAL)
                .status(ScheduleStatus.UPCOMING)
                .build();

        schedule = scheduleRepository.save(schedule);

        // Set zone prices
        setZonePrices(schedule, show, req.getZonePrices(), venue);

        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest req) {
        Schedule schedule = findById(id);

        if (schedule.getApprovalStatus() == ScheduleApprovalStatus.APPROVED) {
            throw new RuntimeException("Không thể chỉnh sửa schedule đã được duyệt. Hãy hủy và tạo mới.");
        }
        if (schedule.getStatus() != ScheduleStatus.UPCOMING) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa schedule UPCOMING");
        }

        Venue venue = schedule.getVenue();
        if (req.getVenueId() != null) {
            venue = findVenue(req.getVenueId());
            schedule.setVenue(venue);
        }

        LocalDateTime start = req.getStartTime() != null ? req.getStartTime() : schedule.getStartTime();
        LocalDateTime end   = req.getEndTime()   != null ? req.getEndTime()   : schedule.getEndTime();

        if (req.getStartTime() != null || req.getEndTime() != null) {
            validateTimeRange(start, end);
            checkVenueConflict(venue.getId(), start, end, schedule.getId());
            schedule.setStartTime(start);
            schedule.setEndTime(end);
        }

        if (req.getNotes() != null) schedule.setNotes(req.getNotes());

        // Reset to pending when modified
        schedule.setApprovalStatus(ScheduleApprovalStatus.PENDING_APPROVAL);
        schedule.setApprovedBy(null);
        schedule.setApprovedAt(null);
        schedule.setApprovalNote(null);
        schedule = scheduleRepository.save(schedule);

        if (req.getZonePrices() != null) {
            updateZonePrices(schedule, req.getZonePrices());
        }

        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse cancelSchedule(Long id) {
        Schedule schedule = findById(id);
        if (schedule.getStatus() == ScheduleStatus.FINISHED) {
            throw new RuntimeException("Không thể hủy schedule đã kết thúc");
        }
        schedule.setStatus(ScheduleStatus.CANCELLED);
        return toResponse(scheduleRepository.save(schedule));
    }

    // ─── Manager: approve / reject individual schedules ──────────────────────

    public List<ScheduleResponse> getPendingSchedules() {
        return scheduleRepository.findAll().stream()
                .filter(s -> s.getApprovalStatus() == ScheduleApprovalStatus.PENDING_APPROVAL)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getSchedulesPendingForShow(Long showId) {
        return scheduleRepository.findByShowId(showId).stream()
                .filter(s -> s.getApprovalStatus() == ScheduleApprovalStatus.PENDING_APPROVAL)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleResponse reviewSchedule(Long id, ReviewScheduleRequest req) {
        Schedule schedule = findById(id);
        User manager = getCurrentUser();

        if (schedule.getApprovalStatus() != ScheduleApprovalStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Schedule không ở trạng thái PENDING_APPROVAL");
        }

        schedule.setApprovedBy(manager);
        schedule.setApprovedAt(LocalDateTime.now());
        schedule.setApprovalNote(req.getNote());

        if (req.isApproved()) {
            schedule.setApprovalStatus(ScheduleApprovalStatus.APPROVED);
        } else {
            if (req.getNote() == null || req.getNote().isBlank()) {
                throw new RuntimeException("Phải cung cấp lý do từ chối schedule");
            }
            schedule.setApprovalStatus(ScheduleApprovalStatus.REJECTED);
        }

        return toResponse(scheduleRepository.save(schedule));
    }

    // ─── Auto status update (called by scheduler) ─────────────────────────────

    @Transactional
    public void autoUpdateScheduleStatuses() {
        LocalDateTime now = LocalDateTime.now();

        List<Schedule> toOngoing = scheduleRepository.findSchedulesToMarkOngoing(now);
        toOngoing.forEach(s -> s.setStatus(ScheduleStatus.ONGOING));
        scheduleRepository.saveAll(toOngoing);

        List<Schedule> toFinished = scheduleRepository.findSchedulesToMarkFinished(now);
        toFinished.forEach(s -> s.setStatus(ScheduleStatus.FINISHED));
        scheduleRepository.saveAll(toFinished);
    }

    // ─── Zone price helpers ───────────────────────────────────────────────────

    private void setZonePrices(Schedule schedule, Show show,
                               List<CreateScheduleRequest.ZonePriceEntry> entries, Venue venue) {
        boolean isFree = show.getShowType().getName() != ShowTypeName.PAID_WITH_REGISTRATION;
        List<Zone> venueZones = zoneRepository.findByVenueId(venue.getId());

        if (entries != null && !entries.isEmpty() && !isFree) {
            for (CreateScheduleRequest.ZonePriceEntry entry : entries) {
                Zone zone = zoneRepository.findById(entry.getZoneId())
                        .orElseThrow(() -> new RuntimeException("Zone không tồn tại: " + entry.getZoneId()));
                if (!zone.getVenue().getId().equals(venue.getId())) {
                    throw new RuntimeException("Zone " + entry.getZoneId() + " không thuộc venue này");
                }
                ScheduleZonePrice szp = ScheduleZonePrice.builder()
                        .schedule(schedule)
                        .zone(zone)
                        .price(entry.getPrice() != null ? entry.getPrice() : zone.getDefaultPrice())
                        .build();
                scheduleZonePriceRepository.save(szp);
            }
        } else {
            // Auto-create zone prices for all zones in venue
            for (Zone zone : venueZones) {
                ScheduleZonePrice szp = ScheduleZonePrice.builder()
                        .schedule(schedule)
                        .zone(zone)
                        .price(isFree ? BigDecimal.ZERO : zone.getDefaultPrice())
                        .build();
                scheduleZonePriceRepository.save(szp);
            }
        }
    }

    private void updateZonePrices(Schedule schedule, List<UpdateScheduleRequest.ZonePriceEntry> entries) {
        for (UpdateScheduleRequest.ZonePriceEntry entry : entries) {
            scheduleZonePriceRepository.findByScheduleIdAndZoneId(schedule.getId(), entry.getZoneId())
                    .ifPresent(szp -> {
                        if (entry.getPrice() != null) szp.setPrice(entry.getPrice());
                        scheduleZonePriceRepository.save(szp);
                    });
        }
    }

    // ─── Validators ──────────────────────────────────────────────────────────

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian bắt đầu phải trong tương lai");
        }
    }

    private void checkVenueConflict(Long venueId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Schedule> conflicts = scheduleRepository.findConflictingSchedules(venueId, start, end, excludeId);
        if (!conflicts.isEmpty()) {
            Schedule conflict = conflicts.get(0);
            throw new RuntimeException(
                    "Địa điểm đã có lịch trình xung đột: [" + conflict.getStartTime()
                            + " → " + conflict.getEndTime() + "] (BR10)");
        }
    }

    // ─── Finders ─────────────────────────────────────────────────────────────

    public Schedule findById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch trình với ID: " + id));
    }

    private Show findShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy show với ID: " + id));
    }

    private Venue findVenue(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa điểm với ID: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    // ─── Response mapper ─────────────────────────────────────────────────────

    public ScheduleResponse toResponse(Schedule s) {
        Venue v = s.getVenue();
        ScheduleResponse.VenueInfo venueInfo = v != null ? ScheduleResponse.VenueInfo.builder()
                .id(v.getId())
                .name(v.getName())
                .location(v.getLocation())
                .capacity(v.getCapacity())
                .build() : null;

        ScheduleResponse.UserInfo approverInfo = s.getApprovedBy() != null
                ? ScheduleResponse.UserInfo.builder()
                        .id(s.getApprovedBy().getId())
                        .fullName(s.getApprovedBy().getFullName())
                        .build()
                : null;

        List<ScheduleZonePrice> zonePrices = scheduleZonePriceRepository.findByScheduleId(s.getId());
        List<ScheduleResponse.ZonePriceInfo> zonePriceInfos = zonePrices.stream()
                .map(szp -> {
                    Integer booked = scheduleZonePriceRepository.sumBookedQuantityByScheduleAndZone(
                            s.getId(), szp.getZone().getId());
                    int cap = szp.getZone().getCapacity();
                    return ScheduleResponse.ZonePriceInfo.builder()
                            .zoneId(szp.getZone().getId())
                            .zoneName(szp.getZone().getName())
                            .zoneCapacity(cap)
                            .price(szp.getPrice())
                            .bookedCount(booked)
                            .availableCount(cap - booked)
                            .build();
                }).collect(Collectors.toList());

        return ScheduleResponse.builder()
                .id(s.getId())
                .showId(s.getShow() != null ? s.getShow().getId() : null)
                .showName(s.getShow() != null ? s.getShow().getName() : null)
                .showType(s.getShow() != null ? s.getShow().getShowType().getName().name() : null)
                .venue(venueInfo)
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .notes(s.getNotes())
                .status(s.getStatus().name())
                .approvalStatus(s.getApprovalStatus().name())
                .approvalNote(s.getApprovalNote())
                .approvedBy(approverInfo)
                .approvedAt(s.getApprovedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .zonePrices(zonePriceInfos)
                .build();
    }
}
