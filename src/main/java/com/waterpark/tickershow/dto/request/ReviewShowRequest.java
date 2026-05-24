package com.waterpark.tickershow.dto.request;

import lombok.Data;

@Data
public class ReviewShowRequest {

    // true = approve, false = reject
    private boolean approved;


    private boolean rejected;
    // Bắt buộc khi approved = false
    private String rejectionReason;
}
 
