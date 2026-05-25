package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateScheduleRequest;
import com.waterpark.tickershow.dto.request.ReviewScheduleRequest;
import com.waterpark.tickershow.dto.request.UpdateScheduleRequest;
import com.waterpark.tickershow.dto.response.ScheduleResponse;
import com.waterpark.tickershow.entity.Schedule;
import com.waterpark.tickershow.entity.ScheduleZonePrice;
import com.waterpark.tickershow.entity.Show;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.entity.Venue;
import com.waterpark.tickershow.entity.Zone;
import com.waterpark.tickershow.enums.ScheduleApprovalStatus;
import com.waterpark.tickershow.enums.ScheduleStatus;
import com.waterpark.tickershow.enums.ShowStatus;
import com.waterpark.tickershow.enums.ShowTypeName;
import com.waterpark.tickershow.repository.ScheduleRepository;
import com.waterpark.tickershow.repository.ScheduleZonePriceRepository;
import com.waterpark.tickershow.repository.ShowRepository;
import com.waterpark.tickershow.repository.UserRepository;
import com.waterpark.tickershow.repository.VenueRepository;
import com.waterpark.tickershow.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest req) {
        Show show = findShow(req.getShowId());
        validateOperatorOwnsShow(show);
        validateCanCreateSchedule(show);

        Venue venue = findVenue(req.getVenueId());
        validateTimeRange(req.getStartTime(), req.getEndTime());
        checkVenueConflict(venue.getId(), req.getStartTime(), req.getEndTime(), 0L);

        ScheduleApprovalStatus approvalStatus = isShowPackageEditable(show)
                ? ScheduleApprovalStatus.DRAFT
                : ScheduleApprovalStatus.PENDING_APPROVAL;

        Schedule schedule = Schedule.builder()
                .show(show)
                .venue(venue)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .notes(req.getNotes())
                .approvalStatus(approvalStatus)
                .status(ScheduleStatus.UPCOMING)
                .build();

        schedule = scheduleRepository.save(schedule);
        setZonePrices(schedule, show, req.getZonePrices(), venue);

        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest req) {
        Schedule schedule = findById(id);
        validateOperatorOwnsShow(schedule.getShow());

        if (schedule.getApprovalStatus() == ScheduleApprovalStatus.APPROVED) {
            throw new RuntimeException("Khong the chinh sua schedule da duoc duyet. Hay huy va tao moi.");
        }
        if (schedule.getStatus() != ScheduleStatus.UPCOMING) {
            throw new RuntimeException("Chi co the chinh sua schedule UPCOMING");
        }
        if (schedule.getShow().getStatus() == ShowStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Khong the chinh sua schedule khi show dang cho duyet");
        }

        Venue venue = schedule.getVenue();
        if (req.getVenueId() != null) {
            venue = findVenue(req.getVenueId());
            schedule.setVenue(venue);
        }

        LocalDateTime start = req.getStartTime() != null ? req.getStartTime() : schedule.getStartTime();
        LocalDateTime end = req.getEndTime() != null ? req.getEndTime() : schedule.getEndTime();

        if (req.getStartTime() != null || req.getEndTime() != null || req.getVenueId() != null) {
            validateTimeRange(start, end);
            checkVenueConflict(venue.getId(), start, end, schedule.getId());
            schedule.setStartTime(start);
            schedule.setEndTime(end);
        }

        if (req.getNotes() != null) schedule.setNotes(req.getNotes());

        schedule.setApprovalStatus(isShowPackageEditable(schedule.getShow())
                ? ScheduleApprovalStatus.DRAFT
                : ScheduleApprovalStatus.PENDING_APPROVAL);
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
            throw new RuntimeException("Khong the huy schedule da ket thuc");
        }
        schedule.setStatus(ScheduleStatus.CANCELLED);
        return toResponse(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> getPendingSchedules() {
        return scheduleRepository.findAll().stream()
                .filter(s -> s.getApprovalStatus() == ScheduleApprovalStatus.PENDING_APPROVAL)
                .filter(s -> s.getShow().getStatus() == ShowStatus.APPROVED
                        || s.getShow().getStatus() == ShowStatus.PUBLISHED)
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
            throw new RuntimeException("Schedule khong o trang thai PENDING_APPROVAL");
        }
        if (schedule.getShow().getStatus() != ShowStatus.APPROVED
                && schedule.getShow().getStatus() != ShowStatus.PUBLISHED) {
            throw new RuntimeException("Schedule moi chi duoc duyet rieng sau khi show da duoc duyet");
        }

        schedule.setApprovedBy(manager);
        schedule.setApprovedAt(LocalDateTime.now());
        schedule.setApprovalNote(req.getNote());

        if (req.isApproved()) {
            schedule.setApprovalStatus(ScheduleApprovalStatus.APPROVED);
        } else {
            if (req.getNote() == null || req.getNote().isBlank()) {
                throw new RuntimeException("Phai cung cap ly do tu choi schedule");
            }
            schedule.setApprovalStatus(ScheduleApprovalStatus.REJECTED);
        }

        return toResponse(scheduleRepository.save(schedule));
    }

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

    private void setZonePrices(Schedule schedule, Show show,
                               List<CreateScheduleRequest.ZonePriceEntry> entries, Venue venue) {
        boolean isFree = show.getShowType().getName() != ShowTypeName.PAID_WITH_REGISTRATION;
        List<Zone> venueZones = zoneRepository.findByVenueId(venue.getId());

        if (entries != null && !entries.isEmpty() && !isFree) {
            for (CreateScheduleRequest.ZonePriceEntry entry : entries) {
                Zone zone = zoneRepository.findById(entry.getZoneId())
                        .orElseThrow(() -> new RuntimeException("Zone khong ton tai: " + entry.getZoneId()));
                if (!zone.getVenue().getId().equals(venue.getId())) {
                    throw new RuntimeException("Zone " + entry.getZoneId() + " khong thuoc venue nay");
                }
                ScheduleZonePrice szp = ScheduleZonePrice.builder()
                        .schedule(schedule)
                        .zone(zone)
                        .price(entry.getPrice() != null ? entry.getPrice() : zone.getDefaultPrice())
                        .build();
                scheduleZonePriceRepository.save(szp);
            }
        } else {
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

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new RuntimeException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thoi gian bat dau phai trong tuong lai");
        }
    }

    private void checkVenueConflict(Long venueId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Schedule> conflicts = scheduleRepository.findConflictingSchedules(venueId, start, end, excludeId);
        if (!conflicts.isEmpty()) {
            Schedule conflict = conflicts.get(0);
            throw new RuntimeException(
                    "Dia diem da co lich trinh xung dot: [" + conflict.getStartTime()
                            + " -> " + conflict.getEndTime() + "] (BR10)");
        }
    }

    public Schedule findById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay lich trinh voi ID: " + id));
    }

    private Show findShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay show voi ID: " + id));
    }

    private Venue findVenue(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dia diem voi ID: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));
    }

    private void validateOperatorOwnsShow(Show show) {
        User currentUser = getCurrentUser();
        if (show.getCreatedBy() == null || !show.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Ban khong co quyen thao tac lich trinh cua show nay");
        }
    }

    private void validateCanCreateSchedule(Show show) {
        if (show.getStatus() == ShowStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Khong the them lich trinh khi show dang cho duyet");
        }
        if (show.getStatus() == ShowStatus.REJECTED) {
            throw new RuntimeException("Khong the them lich trinh cho show da bi tu choi");
        }
    }

    private boolean isShowPackageEditable(Show show) {
        return show.getStatus() == ShowStatus.DRAFT || show.getStatus() == ShowStatus.REVISION_REQUIRED;
    }

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
