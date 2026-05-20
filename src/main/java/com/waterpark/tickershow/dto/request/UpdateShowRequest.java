package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateShowRequest {

    @Size(max = 200, message = "Tên show tối đa 200 ký tự")
    private String name;

    private String description;

    private Long showTypeId;

    // Danh sách URL ảnh show (thay thế toàn bộ ảnh cũ nếu có)
    private List<String> imageUrls;
}
