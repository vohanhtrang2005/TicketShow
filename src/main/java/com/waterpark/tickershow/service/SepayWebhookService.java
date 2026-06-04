package com.waterpark.tickershow.service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.waterpark.tickershow.dto.request.SepayWebhookRequest;
import com.waterpark.tickershow.dto.response.ApiResponse;
import com.waterpark.tickershow.entity.Booking;
import com.waterpark.tickershow.entity.Payment;
import com.waterpark.tickershow.enums.BookingStatus;
import com.waterpark.tickershow.enums.PaymentMethod;
import com.waterpark.tickershow.enums.PaymentStatus;
import com.waterpark.tickershow.repository.BookingRepository;
import com.waterpark.tickershow.repository.PaymentRepository;
import com.waterpark.tickershow.entity.Zone;
import com.waterpark.tickershow.repository.ZoneRepository;
import jakarta.transaction.Transactional;

@Service
public class SepayWebhookService {
private final BookingService bookingService;
private final ZoneRepository zoneRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public SepayWebhookService(ZoneRepository zoneRepository, BookingRepository bookingRepository, PaymentRepository paymentRepository, BookingService bookingService) {
        this.zoneRepository = zoneRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    private Long extractBookingId(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("TS(\\d+)");
        Matcher matcher = pattern.matcher(content.toUpperCase());

        if (!matcher.find()) {
            return null;
        }

        return Long.valueOf(matcher.group(1));
    }

    @Transactional
    public ApiResponse<Void> handleWebhook(SepayWebhookRequest request) {
        System.out.println("Received SePay webhook: content="
                + request.getContent()
                + ", amount="
                + request.getTransferAmount()
                + ", type="
                + request.getTransferType()
                + ", referenceCode="
                + request.getReferenceCode());

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            System.out.println("Ignore non-incoming transfer: " + request.getTransferType());
            return ApiResponse.error(400, "Khong phai giao dich tien vao");
        }

        if (request.getReferenceCode() != null
                && paymentRepository.findByTransactionCode(request.getReferenceCode()).isPresent()) {
            System.out.println("Duplicate transaction: " + request.getReferenceCode());
            return ApiResponse.error(409, "Giao dich da duoc xu ly truoc do");
        }

        Long bookingId = extractBookingId(request.getContent());
        if (bookingId == null) {
            return ApiResponse.error(400, "Khong tim thay ma booking trong noi dung chuyen khoan");
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ApiResponse.error(404, "Khong tim thay booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            return ApiResponse.error(400, "Booking khong o trang thai PENDING");
        }

        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            return ApiResponse.error(400, "Thanh toan khong con o trang thai PENDING");
        }

        System.out.println("Extracted booking ID: " + bookingId);

        if (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setPaymentStatus(PaymentStatus.FAILED);
             Zone zone = booking.getZone();
    if (zone != null) {
        zone.setCapacity(zone.getCapacity() + booking.getQuantity());
        zoneRepository.save(zone);
    }
            bookingRepository.save(booking);

            return ApiResponse.error(400, "Booking da het han");
        }

        if (request.getTransferAmount() == null
                || request.getTransferAmount().compareTo(booking.getTotalAmount()) != 0) {
            System.out.println("Payment amount mismatch. Expected: "
                    + booking.getTotalAmount()
                    + ", actual: "
                    + request.getTransferAmount());
            return ApiResponse.error(400, "So tien chuyen khoan khong khop");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);
        bookingService.generateTickets(booking);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getTransferAmount());
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionCode(request.getReferenceCode());
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        System.out.println("Payment confirmed for booking ID: " + bookingId);
        return ApiResponse.success("Thanh toan thanh cong", null);
    }
}
