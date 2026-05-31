package com.waterpark.tickershow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waterpark.tickershow.dto.request.BookingRequest;
import com.waterpark.tickershow.dto.response.BookingResponse;
import com.waterpark.tickershow.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
public class BookingController {
     private final BookingService bookingService;
 
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.ok(response);
    }
       @GetMapping("/{bookingId}/status")
    public ResponseEntity<?> checkStatus(@PathVariable Long bookingId) {
        BookingResponse response = this.bookingService.findBooking(bookingId);
       
        return ResponseEntity.ok(response);
    }

}
