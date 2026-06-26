package com.waterpark.tickershow.enums;

public enum ChatEventType {
    CONVERSATION_CREATED, // Khi bắt đầu tạo cuộc trò chuyện mới
    CONVERSATION_ASSIGNED, // Khi staff tiếp nhận cuộc trò chuyện
    CONVERSATION_CLOSED,   // Khi kết thúc/đóng cuộc trò chuyện
    MESSAGE_CREATED        // Khi có tin nhắn mới được gửi
}