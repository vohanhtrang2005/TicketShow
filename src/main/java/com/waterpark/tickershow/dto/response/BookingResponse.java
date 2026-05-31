package com.waterpark.tickershow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.waterpark.tickershow.enums.BookingStatus;
import com.waterpark.tickershow.enums.PaymentStatus;

import lombok.*;
@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long bookingId;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime expiredAt;
    private String qrCodeUrl;




}


