package com.waterpark.tickershow.service;

import com.waterpark.tickershow.dto.request.CreateShowRequest;
import com.waterpark.tickershow.dto.request.ReviewShowRequest;
import com.waterpark.tickershow.dto.request.UpdateShowRequest;
import com.waterpark.tickershow.dto.response.ShowResponse;
import com.waterpark.tickershow.entity.Show;
import com.waterpark.tickershow.entity.ShowImage;
import com.waterpark.tickershow.entity.ShowType;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.enums.ShowStatus;
import com.waterpark.tickershow.enums.ShowTypeName;
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
import java.util.ArrayList;
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

    // ─── Public: customer sees published shows ────────────────────────────────

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

    // ─── Operator: manage own shows ───────────────────────────────────────────

public Page<ShowResponse> getMyShows(Specification<Show> spec, Pageable pageable) {
    Long userId = getCurrentUserId();

    Specification<Show> ownerSpec = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("createdBy").get("id"), userId);

    //Specification<Show> finalSpec = ownerSpec.and(spec);
    Specification<Show> finalSpec = Specification.where(ownerSpec).and(spec);

    return showRepository.findAll(finalSpec, pageable)
            .map(show -> toResponse(show, false));
}

    @Transactional
    public ShowResponse createShow(CreateShowRequest req) {
        User operator = getCurrentUser();
        ShowType showType = findShowType(req.getShowTypeId());

        ShowStatus status = req.isSaveDraft() ? ShowStatus.DRAFT : ShowStatus.PENDING_APPROVAL;

        Show show = Show.builder()
                .name(req.getName())
                .description(req.getDescription())
                .showType(showType)
                .createdBy(operator)
                .status(status)
                .build();

        show = showRepository.save(show);

        // Save images
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

        if (req.getName() != null)        show.setName(req.getName());
        if (req.getDescription() != null) show.setDescription(req.getDescription());
        if (req.getShowTypeId() != null)  show.setShowType(findShowType(req.getShowTypeId()));

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
            throw new RuntimeException("Chỉ có thể submit show ở trạng thái DRAFT hoặc REVISION_REQUIRED");
        }

        show.setStatus(ShowStatus.PENDING_APPROVAL);
        return toResponse(showRepository.save(show), true);
    }

    @Transactional
    public ShowResponse saveDraft(Long id, UpdateShowRequest req) {
        Show show = findById(id);
        validateOperatorOwnsShow(show);

        if (show.getStatus() != ShowStatus.DRAFT && show.getStatus() != ShowStatus.REVISION_REQUIRED) {
            throw new RuntimeException("Chỉ có thể lưu nháp show ở trạng thái DRAFT hoặc REVISION_REQUIRED");
        }

        if (req.getName() != null)        show.setName(req.getName());
        if (req.getDescription() != null) show.setDescription(req.getDescription());
        if (req.getShowTypeId() != null)  show.setShowType(findShowType(req.getShowTypeId()));

        if (req.getImageUrls() != null) {
            showImageRepository.deleteByShowId(id);
            saveImages(show, req.getImageUrls());
        }

        show.setStatus(ShowStatus.DRAFT);
        return toResponse(showRepository.save(show), true);
    }

    // ─── Manager: approve / reject / publish ─────────────────────────────────

    public List<ShowResponse> getPendingApprovalShows() {
        return showRepository.findByStatus(ShowStatus.PENDING_APPROVAL)
                .stream().map(s -> toResponse(s, false)).collect(Collectors.toList());
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
            throw new RuntimeException("Show không ở trạng thái chờ duyệt (PENDING_APPROVAL)");
        }

        show.setReviewedBy(manager);
        show.setReviewedAt(LocalDateTime.now());

        if (req.isApproved()) {
            show.setStatus(ShowStatus.APPROVED);
            show.setRejectionReason(null);
        }
        else {

             if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new RuntimeException("Phải cung cấp lý do từ chối");
            }
                show.setRejectionReason(req.getRejectionReason());
        // reject vĩnh viễn
    if (req.isRejected()) {

        show.setStatus(ShowStatus.REJECTED);}

        else {
           
            show.setStatus(ShowStatus.REVISION_REQUIRED);
        
        } }

        return toResponse(showRepository.save(show), true);
    }

    @Transactional
    public ShowResponse publishShow(Long id) {
        Show show = findById(id);

        if (show.getStatus() != ShowStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể publish show đã được APPROVED");
        }
        if (show.getSchedules().isEmpty()) {
            throw new RuntimeException("Show phải có ít nhất một lịch trình trước khi publish (BR09)");
        }

        show.setStatus(ShowStatus.PUBLISHED);
        return toResponse(showRepository.save(show), true);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public Show findById(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy show với ID: " + id));
    }

    private void validateOperatorOwnsShow(Show show) {
        Long userId = getCurrentUserId();
        if (!show.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa show này");
        }
    }

    private void validateShowEditable(Show show) {
        if (show.getStatus() != ShowStatus.DRAFT && show.getStatus() != ShowStatus.REVISION_REQUIRED) {
            throw new RuntimeException("Show chỉ có thể chỉnh sửa ở trạng thái DRAFT hoặc REVISION_REQUIRED");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại show với ID: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
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
                    .map(sc -> ShowResponse.ScheduleSummary.builder()
                            .id(sc.getId())
                            .venueName(sc.getVenue() != null ? sc.getVenue().getName() : null)
                            .startTime(sc.getStartTime())
                            .endTime(sc.getEndTime())
                            .status(sc.getStatus().name())
                            .approvalStatus(sc.getApprovalStatus().name())
                            .build())
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
