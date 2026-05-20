package com.waterpark.tickershow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewShowRequest {

    // true = approve, false = reject
    private boolean approved;

    // Bắt buộc khi approved = false
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionReason;
}
