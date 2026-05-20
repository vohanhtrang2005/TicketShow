package com.waterpark.tickershow.enums;

public enum ShowStatus {
    DRAFT,               // Operator saves as draft
    PENDING_APPROVAL,    // Operator submits for review
    APPROVED,            // Manager approves
    REVISION_REQUIRED,   // Manager rejects → operator must revise
    PUBLISHED            // Manager publishes (visible to customers)
}
