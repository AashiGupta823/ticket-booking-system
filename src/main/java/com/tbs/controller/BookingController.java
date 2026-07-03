package com.tbs.controller;

import com.tbs.entity.Booking;
import com.tbs.entity.User;
import com.tbs.repository.BookingRepository;
import com.tbs.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @GetMapping("/me")
    public List<Booking> myBookings(@AuthenticationPrincipal User customer) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id, @AuthenticationPrincipal User customer) {
        bookingService.cancelBooking(id, customer);
        return ResponseEntity.ok().build();
    }
}
