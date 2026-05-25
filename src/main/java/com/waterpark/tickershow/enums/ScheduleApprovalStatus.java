package com.waterpark.tickershow.enums;

public enum ScheduleApprovalStatus {
    DRAFT,             // Operator creates inside a show package before submit
    PENDING_APPROVAL,  // Waiting for Manager approval
    APPROVED,          // Manager approved
    REJECTED           // Manager rejected
}
