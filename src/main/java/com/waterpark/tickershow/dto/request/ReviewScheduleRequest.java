package com.waterpark.tickershow.dto.request;

import lombok.Data;

@Data
public class ReviewScheduleRequest {

    // true = approve, false = reject
    private boolean approved;

    // Ghi chú của manager (bắt buộc khi từ chối)
    private String note;
}
