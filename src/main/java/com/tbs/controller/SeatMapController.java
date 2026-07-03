package com.tbs.controller;

import com.tbs.dto.SeatMapDtos.*;
import com.tbs.entity.*;
import com.tbs.repository.ShowSeatRepository;
import com.tbs.service.BookingService;
import com.tbs.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Customer: visual seat map with real-time status, seat hold, checkout.
@RestController
@RequestMapping("/api/shows/{showId}/seats")
@RequiredArgsConstructor
public class SeatMapController {

    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldService seatHoldService;
    private final BookingService bookingService;

    // Poll this endpoint to render/refresh the seat map (available / held / booked).
    @GetMapping
    public List<SeatStatusDto> seatMap(@PathVariable Long showId) {
        return showSeatRepository.findByShowId(showId).stream()
                .map(ss -> new SeatStatusDto(
                        ss.getId(),
                        ss.getSeat().getId(),
                        ss.getSeat().getRowLabel(),
                        ss.getSeat().getSeatNumber(),
                        ss.getSeat().getCategory().getName(),
                        ss.getSeat().getCategory().getId(),
                        ss.getStatus().name()
                ))
                .toList();
    }

    @PostMapping("/hold")
    public ResponseEntity<?> holdSeat(@PathVariable Long showId, @RequestBody HoldRequest req,
                                       @AuthenticationPrincipal User customer) {
        SeatHold hold = seatHoldService.holdSeat(req.showSeatId(), customer);
        return ResponseEntity.ok(java.util.Map.of(
                "seatHoldId", hold.getId(),
                "showSeatId", req.showSeatId(),
                "expiresAt", hold.getExpiresAt().toString()
        ));
    }

    @PostMapping("/release")
    public ResponseEntity<?> releaseSeat(@PathVariable Long showId, @RequestBody HoldRequest req) {
        seatHoldService.releaseHold(req.showSeatId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long showId,
                                                    @RequestBody ConfirmBookingRequest req,
                                                    @AuthenticationPrincipal User customer) {
        Booking booking = bookingService.confirmBookingFromHolds(req.showSeatIds(), customer);
        return ResponseEntity.ok(booking);
    }
}
