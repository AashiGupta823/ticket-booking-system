package com.tbs.controller;

import com.tbs.dto.WaitlistDtos.*;
import com.tbs.entity.*;
import com.tbs.exception.ResourceNotFoundException;
import com.tbs.repository.SeatCategoryRepository;
import com.tbs.repository.ShowRepository;
import com.tbs.repository.WaitlistRepository;
import com.tbs.service.BookingService;
import com.tbs.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final WaitlistRepository waitlistRepository;
    private final ShowRepository showRepository;
    private final SeatCategoryRepository seatCategoryRepository;
    private final BookingService bookingService;

    // Customer joins the waitlist for a sold-out category.
    @PostMapping("/join")
    public ResponseEntity<Waitlist> join(@RequestBody JoinWaitlistRequest req,
                                          @AuthenticationPrincipal User customer) {
        Show show = showRepository.findById(req.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        SeatCategory category = seatCategoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        Waitlist entry = waitlistService.joinWaitlist(show, category, customer);
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/me")
    public List<Waitlist> myWaitlist(@AuthenticationPrincipal User customer) {
        return waitlistRepository.findByCustomerIdOrderByJoinedAtDesc(customer.getId());
    }

    // Public: the emailed offer link lands here first so the customer can see
    // what they're accepting before authenticating.
    @GetMapping("/offer/{token}")
    public ResponseEntity<?> viewOffer(@PathVariable String token) {
        WaitlistOffer offer = waitlistService.validateOffer(token);
        return ResponseEntity.ok(java.util.Map.of(
                "showTitle", offer.getShowSeat().getShow().getTitle(),
                "seat", offer.getShowSeat().getSeat().getRowLabel() + offer.getShowSeat().getSeat().getSeatNumber(),
                "expiresAt", offer.getExpiresAt().toString()
        ));
    }

    // Authenticated: customer confirms the offer -> becomes a real booking.
    @PostMapping("/offer/{token}/accept")
    public ResponseEntity<Booking> acceptOffer(@PathVariable String token,
                                                @AuthenticationPrincipal User customer) {
        WaitlistOffer offer = waitlistService.validateOffer(token);
        if (!offer.getWaitlistEntry().getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("This offer was not issued to you");
        }
        Booking booking = bookingService.confirmBookingFromWaitlistOffer(offer, customer);
        return ResponseEntity.ok(booking);
    }
}
