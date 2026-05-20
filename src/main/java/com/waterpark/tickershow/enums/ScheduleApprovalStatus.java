package com.waterpark.tickershow.enums;

public enum ScheduleApprovalStatus {
    PENDING_APPROVAL,  // Operator tạo, chờ Manager duyệt
    APPROVED,          // Manager duyệt
    REJECTED           // Manager từ chối
}
