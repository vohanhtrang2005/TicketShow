package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateShowRequest {

    @NotBlank(message = "Tên show không được để trống")
    @Size(max = 200, message = "Tên show tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Loại show không được để trống")
    private Long showTypeId;

    // Danh sách URL ảnh show
    private List<String> imageUrls;

    // true = lưu bản nháp (DRAFT), false = submit để duyệt (PENDING_APPROVAL)
    private boolean saveDraft = true;
}
