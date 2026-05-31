package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateShowRequest;
import com.waterpark.tickershow.dto.request.ReviewShowRequest;
import com.waterpark.tickershow.dto.request.UpdateShowRequest;
import com.waterpark.tickershow.dto.response.ShowResponse;
import com.waterpark.tickershow.entity.Schedule;
import com.waterpark.tickershow.entity.Show;
import com.waterpark.tickershow.entity.ShowImage;
import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.enums.ScheduleApprovalStatus;
import com.waterpark.tickershow.enums.ShowStatus;
import com.waterpark.tickershow.repository.ShowImageRepository;
import com.waterpark.tickershow.repository.ShowRepository;
import com.waterpark.tickershow.repository.ShowTypeRepository;
import com.waterpark.tickershow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowTypeRepository showTypeRepository;
    private final ShowImageRepository showImageRepository;
    private final UserRepository userRepository;

    public List<ShowResponse> getPublicShows() {
        return showRepository.findByStatusIn(List.of(ShowStatus.APPROVED, ShowStatus.PUBLISHED))
                .stream().map(s -> toResponse(s, false)).collect(Collectors.toList());
    }

    public ShowResponse getShowById(Long id) {
        Show show = findById(id);
        return toResponse(show, true);
    }

    public List<ShowResponse> searchShows(String keyword) {
        return showRepository.searchByNameAndStatuses(keyword,
                        List.of(ShowStatus.APPROVED, ShowStatus.PUBLISHED))
                .stream().map(s -> toResponse(s, false)).collect(Collectors.toList());
    }

    public Page<ShowResponse> getMyShows(Specification<Show> spec, Pageable pageable) {
        Long userId = getCurrentUserId();

        Specification<Show> ownerSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("createdBy").get("id"), userId);

        Specification<Show> finalSpec = Specification.where(ownerSpec).and(spec);

        return showRepository.findAll(finalSpec, pageable)
                .map(show -> toResponse(show, false));
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest req) {
        User operator = getCurrentUser();
        ShowType showType = findShowType(req.getShowTypeId());

        Show show = Show.builder()
                .name(req.getName())
                .description(req.getDescription())
                .showType(showType)
                .createdBy(operator)
                .status(ShowStatus.DRAFT)
                .build();

        show = showRepository.save(show);

        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            saveImages(show, req.getImageUrls());
        }

        return toResponse(show, true);
    }

    @Transactional
    public ShowResponse updateShow(Long id, UpdateShowRequest req) {
        Show show = findById(id);
        validateOperatorOwnsShow(show);
        validateShowEditable(show);

        if (req.getName() != null) show.setName(req.getName());
        if (req.getDescription() != null) show.setDescription(req.getDescription());
        if (req.getShowTypeId() != null) show.setShowType(findShowType(req.getShowTypeId()));

        if (req.getImageUrls() != null) {
            showImageRepository.deleteByShowId(id);
            saveImages(show, req.getImageUrls());
        }

        return toResponse(showRepository.save(show), true);
    }

    @Transactional
    public ShowResponse submitForApproval(Long id) {
        Show show = findById(id);
        validateOperatorOwnsShow(show);

        if (show.getStatus() != ShowStatus.DRAFT && show.getStatus() != ShowStatus.REVISION_REQUIRED) {
            throw new RuntimeException("Chi co the submit show o trang thai DRAFT hoac REVISION_REQUIRED");
        }
        if (show.getSchedules().isEmpty()) {
            throw new RuntimeException("Show phai co it nhat mot lich trinh truoc khi gui duyet");
        }
        if (show.getSchedules().stream().anyMatch(s -> s.getStatus().name().equals("CANCELLED"))) {
            throw new RuntimeException("Show khong the gui duyet neu con lich trinh da huy");
        }

        show.getSchedules().forEach(s -> {
            if (s.getApprovalStatus() != ScheduleApprovalStatus.APPROVED) {
                s.setApprovalStatus(ScheduleApprovalStatus.PENDING_APPROVAL);
                s.setApprovedBy(null);
                s.setApprovedAt(null);
                s.setApprovalNote(null);
            }
        });
        show.setStatus(ShowStatus.PENDING_APPROVAL);
        show.setRejectionReason(null);

        return toResponse(showRepository.save(show), true);
    }

    @Transactional
    public ShowResponse saveDraft(Long id, UpdateShowRequest req) {
        Show show = findById(id);
        validateOperatorOwnsShow(show);

        if (show.getStatus() != ShowStatus.DRAFT && show.getStatus() != ShowStatus.REVISION_REQUIRED) {
            throw new RuntimeException("Chi co the luu nhap show o trang thai DRAFT hoac REVISION_REQUIRED");
        }

        if (req.getName() != null) show.setName(req.getName());
        if (req.getDescription() != null) show.setDescription(req.getDescription());
        if (req.getShowTypeId() != null) show.setShowType(findShowType(req.getShowTypeId()));

        if (req.getImageUrls() != null) {
            showImageRepository.deleteByShowId(id);
            saveImages(show, req.getImageUrls());
        }

        show.setStatus(ShowStatus.DRAFT);
        show.getSchedules().forEach(s -> {
            if (s.getApprovalStatus() != ScheduleApprovalStatus.APPROVED) {
                s.setApprovalStatus(ScheduleApprovalStatus.DRAFT);
                s.setApprovedBy(null);
                s.setApprovedAt(null);
                s.setApprovalNote(null);
            }
        });

        return toResponse(showRepository.save(show), true);
    }

    public List<ShowResponse> getPendingApprovalShows() {
        return showRepository.findByStatus(ShowStatus.PENDING_APPROVAL)
                .stream().map(s -> toResponse(s, true)).collect(Collectors.toList());
    }

    public List<ShowResponse> getAllShowsForManager() {
        return showRepository.findAll()
                .stream().map(s -> toResponse(s, false)).collect(Collectors.toList());
    }

    @Transactional
    public ShowResponse reviewShow(Long id, ReviewShowRequest req) {
        Show show = findById(id);
        User manager = getCurrentUser();

        if (show.getStatus() != ShowStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Show khong o trang thai cho duyet (PENDING_APPROVAL)");
        }
        if (show.getSchedules().isEmpty()) {
            throw new RuntimeException("Khong the duyet show chua co lich trinh");
        }

        show.setReviewedBy(manager);
        show.setReviewedAt(LocalDateTime.now());

        if (req.isApproved()) {
            show.setStatus(ShowStatus.APPROVED);
            show.setRejectionReason(null);
            show.getSchedules().forEach(s -> approvePackageSchedule(s, manager));
        } else {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new RuntimeException("Phai cung cap ly do tu choi");
            }

            show.setRejectionReason(req.getRejectionReason());
            if (req.isRejected()) {
                show.setStatus(ShowStatus.REJECTED);
                show.getSchedules().forEach(s -> rejectPackageSchedule(s, manager, req.getRejectionReason()));
            } else {
                show.setStatus(ShowStatus.REVISION_REQUIRED);
                show.getSchedules().forEach(s -> movePackageScheduleBackToDraft(s, req.getRejectionReason()));
            }
        }

        return toResponse(showRepository.save(show), true);
    }

    @Transactional
    public ShowResponse publishShow(Long id) {
        Show show = findById(id);

        if (show.getStatus() != ShowStatus.APPROVED) {
            throw new RuntimeException("Chi co the publish show da duoc APPROVED");
        }
        if (show.getSchedules().isEmpty()) {
            throw new RuntimeException("Show phai co it nhat mot lich trinh truoc khi publish");
        }
        boolean hasUnapprovedSchedule = show.getSchedules().stream()
                .anyMatch(s -> s.getApprovalStatus() != ScheduleApprovalStatus.APPROVED);
        if (hasUnapprovedSchedule) {
            throw new RuntimeException("Tat ca lich trinh cua show phai duoc duyet truoc khi publish");
        }

        show.setStatus(ShowStatus.PUBLISHED);
        return toResponse(showRepository.save(show), true);
    }

    public Show findById(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay show voi ID: " + id));
    }

    private void approvePackageSchedule(Schedule schedule, User manager) {
        schedule.setApprovalStatus(ScheduleApprovalStatus.APPROVED);
        schedule.setApprovedBy(manager);
        schedule.setApprovedAt(LocalDateTime.now());
        schedule.setApprovalNote("Approved with show package");
    }

    private void rejectPackageSchedule(Schedule schedule, User manager, String reason) {
        schedule.setApprovalStatus(ScheduleApprovalStatus.REJECTED);
        schedule.setApprovedBy(manager);
        schedule.setApprovedAt(LocalDateTime.now());
        schedule.setApprovalNote(reason);
    }

    private void movePackageScheduleBackToDraft(Schedule schedule, String reason) {
        schedule.setApprovalStatus(ScheduleApprovalStatus.DRAFT);
        schedule.setApprovedBy(null);
        schedule.setApprovedAt(null);
        schedule.setApprovalNote(reason);
    }

    private void validateOperatorOwnsShow(Show show) {
        Long userId = getCurrentUserId();
        if (!show.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Ban khong co quyen chinh sua show nay");
        }
    }

    private void validateShowEditable(Show show) {
        if (show.getStatus() != ShowStatus.DRAFT && show.getStatus() != ShowStatus.REVISION_REQUIRED) {
            throw new RuntimeException("Show chi co the chinh sua o trang thai DRAFT hoac REVISION_REQUIRED");
        }
    }

    private void saveImages(Show show, List<String> urls) {
        for (String url : urls) {
            ShowImage img = ShowImage.builder()
                    .show(show)
                    .imageUrl(url)
                    .build();
            showImageRepository.save(img);
        }
    }

    private ShowType findShowType(Long id) {
        return showTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay loai show voi ID: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));
    }

    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public ShowResponse toResponse(Show s, boolean includeSchedules) {
        List<String> imageUrls = showImageRepository.findByShowId(s.getId())
                .stream().map(ShowImage::getImageUrl).collect(Collectors.toList());

        List<ShowResponse.ScheduleSummary> scheduleSummaries = null;
                if (includeSchedules) {
            scheduleSummaries = s.getSchedules().stream()
                    .map(sc -> {
                        // 1. Duyệt qua từng dòng giá (ScheduleZonePrice) của lịch chiếu hiện tại
                        List<ShowResponse.ZonePriceInfo> zoneInfos = sc.getZonePrices().stream()
                                .map(zp -> ShowResponse.ZonePriceInfo.builder()
                                        .zoneId(zp.getZone().getId())
                                        .zoneName(zp.getZone().getName())
                                        .price(zp.getPrice() != null ? zp.getPrice().doubleValue() : 0.0)
                                        .availableCapacity(zp.getZone().getCapacity()) // Tạm lấy max capacity của Zone
                                        .build())
                                .collect(Collectors.toList());

                        // 2. Trả về ScheduleSummary đã nhét thêm danh sách giá (zoneInfos)
                        return ShowResponse.ScheduleSummary.builder()
                                .id(sc.getId())
                                .venueName(sc.getVenue() != null ? sc.getVenue().getName() : null)
                                .startTime(sc.getStartTime())
                                .endTime(sc.getEndTime())
                                .status(sc.getStatus().name())
                                .approvalStatus(sc.getApprovalStatus().name())
                                .zones(zoneInfos) // <-- Gán danh sách giá vào DTO
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        ShowResponse.UserInfo createdByInfo = s.getCreatedBy() != null
                ? ShowResponse.UserInfo.builder()
                .id(s.getCreatedBy().getId())
                .fullName(s.getCreatedBy().getFullName())
                .email(s.getCreatedBy().getEmail())
                .build()
                : null;

        ShowResponse.UserInfo reviewedByInfo = s.getReviewedBy() != null
                ? ShowResponse.UserInfo.builder()
                .id(s.getReviewedBy().getId())
                .fullName(s.getReviewedBy().getFullName())
                .email(s.getReviewedBy().getEmail())
                .build()
                : null;

        return ShowResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .status(s.getStatus().name())
                .showType(ShowResponse.ShowTypeInfo.builder()
                        .id(s.getShowType().getId())
                        .name(s.getShowType().getName().name())
                        .description(s.getShowType().getDescription())
                        .build())
                .createdBy(createdByInfo)
                .reviewedBy(reviewedByInfo)
                .rejectionReason(s.getRejectionReason())
                .reviewedAt(s.getReviewedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .imageUrls(imageUrls)
                .schedules(scheduleSummaries)
                .build();
    }
}
