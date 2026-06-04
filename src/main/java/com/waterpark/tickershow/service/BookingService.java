package com.waterpark.tickershow.service;


import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.waterpark.tickershow.dto.request.BookingRequest;
import com.waterpark.tickershow.dto.response.BookingResponse;
import com.waterpark.tickershow.entity.Booking;
import com.waterpark.tickershow.entity.Schedule;
import com.waterpark.tickershow.entity.ScheduleZonePrice;
import com.waterpark.tickershow.entity.User;
import com.waterpark.tickershow.entity.Zone;
import com.waterpark.tickershow.enums.BookingStatus;
import com.waterpark.tickershow.enums.PaymentStatus;
import com.waterpark.tickershow.repository.BookingRepository;
import com.waterpark.tickershow.repository.ScheduleRepository;
import com.waterpark.tickershow.repository.ScheduleZonePriceRepository;
import com.waterpark.tickershow.repository.UserRepository;
import com.waterpark.tickershow.repository.ZoneRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final ZoneRepository zoneRepository;
private final ScheduleZonePriceRepository scheduleZonePriceRepository;
private final UserRepository userRepository;
    public BookingService(
            BookingRepository bookingRepository,
            ScheduleRepository scheduleRepository,
            ZoneRepository zoneRepository,
            ScheduleZonePriceRepository scheduleZonePriceRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.zoneRepository = zoneRepository;
        this.scheduleZonePriceRepository = scheduleZonePriceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
                 
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        validateRequest(request);

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch diễn"));

        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng vé"));

        if (zone.getCapacity() < request.getQuantity()) {
            throw new RuntimeException("Không đủ vé");
        }

        zone.setCapacity(zone.getCapacity() - request.getQuantity());
        zoneRepository.save(zone);

        ScheduleZonePrice scheduleZonePrice = scheduleZonePriceRepository
        .findByScheduleIdAndZoneId(schedule.getId(), zone.getId())
        .orElseThrow(() -> new RuntimeException("Chưa cấu hình giá vé cho lịch diễn và hạng vé này"));

BigDecimal totalAmount = scheduleZonePrice.getPrice()
        .multiply(BigDecimal.valueOf(request.getQuantity()));

        Booking booking = new Booking();
        booking.setSchedule(schedule);
        booking.setZone(zone);
        booking.setQuantity(request.getQuantity());
       booking.setReceiverName(request.getCustomerName());
booking.setReceiverEmail(request.getCustomerEmail());
booking.setReceiverPhone(request.getCustomerPhone());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(10));



        booking.setCustomer(currentUser);
        booking.setUnitPrice(scheduleZonePrice.getPrice());

        booking = bookingRepository.save(booking);

        String qrCodeUrl = generateQrCodeUrl(booking);

        return new BookingResponse(
                booking.getId(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getExpiredAt(),
                qrCodeUrl
        );
    }

    private void validateRequest(BookingRequest request) {
        if (request.getScheduleId() == null) {
            throw new RuntimeException("Thiếu scheduleId");
        }

        if (request.getZoneId() == null) {
            throw new RuntimeException("Thiếu zoneId");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng vé không hợp lệ");
        }

        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập họ tên");
        }

        if (request.getCustomerEmail() == null || request.getCustomerEmail().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập email");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại");
        }
    }

    private String generateQrCodeUrl(Booking booking) {
        String bankId = "TPB"; 
        String accountNo = "22101011205";
        String accountName = "VO HANH TRANG";

       return "https://qr.sepay.vn/img?"
        + "acc=" + accountNo
        + "&bank=" + bankId
        + "&amount=" + booking.getTotalAmount()
        + "&des=TS" + booking.getId()
        + "&template=compact";
    }



    
@Transactional
public BookingResponse findBooking(Long bookingId) {

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

    if (booking.getStatus() == BookingStatus.PENDING
            && booking.getExpiredAt().isBefore(LocalDateTime.now())) {

        booking.setStatus(BookingStatus.EXPIRED);
    booking.setPaymentStatus(PaymentStatus.FAILED);
    Zone zone = booking.getZone();
       zone.setCapacity(booking.getZone().getCapacity() + booking.getQuantity()); 
zoneRepository.save(zone);
        bookingRepository.save(booking);
    }

    return new BookingResponse(
         booking.getId(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getExpiredAt(),
               generateQrCodeUrl(booking)
    );
}
}   