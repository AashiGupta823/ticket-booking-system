package com.tbs.service;

import com.tbs.entity.*;
import com.tbs.exception.ResourceNotFoundException;
import com.tbs.exception.SeatUnavailableException;
import com.tbs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;
    private final ShowCategoryPriceRepository showCategoryPriceRepository;
    private final SeatHoldService seatHoldService;
    private final WaitlistService waitlistService;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;

    /**
     * Normal checkout: customer has an active (non-expired) hold on every
     * seat in showSeatIds. Re-validates each hold under lock before
     * confirming, so a hold that expired in the few seconds since the
     * customer clicked "pay" is rejected rather than silently honoured.
     */
    @Transactional
    public Booking confirmBookingFromHolds(List<Long> showSeatIds, User customer) {
        BigDecimal total = BigDecimal.ZERO;
        Booking booking = Booking.builder()
                .bookingReference(generateReference())
                .customer(customer)
                .status(Booking.Status.CONFIRMED)
                .createdAt(Instant.now())
                .build();

        for (Long showSeatId : showSeatIds) {
            ShowSeat showSeat = showSeatRepository.findByIdForUpdate(showSeatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

            SeatHold hold = seatHoldRepository.findByShowSeatId(showSeatId)
                    .orElseThrow(() -> new SeatUnavailableException("No active hold on this seat"));

            if (!hold.getCustomer().getId().equals(customer.getId())) {
                throw new SeatUnavailableException("This seat is held by another customer");
            }
            if (hold.isConsumed() || hold.getExpiresAt().isBefore(Instant.now())) {
                throw new SeatUnavailableException("Your hold on this seat has expired");
            }
            if (showSeat.getStatus() != ShowSeat.Status.HELD) {
                throw new SeatUnavailableException("Seat is no longer held");
            }

            showSeat.setStatus(ShowSeat.Status.BOOKED);
            showSeatRepository.save(showSeat);
            seatHoldService.markConsumed(showSeatId);

            BigDecimal price = showCategoryPriceRepository
                    .findByShowIdAndCategoryId(showSeat.getShow().getId(), showSeat.getSeat().getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Price not set for this category"))
                    .getPrice();

            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .showSeat(showSeat)
                    .price(price)
                    .build();
            booking.getSeats().add(bookingSeat);
            total = total.add(price);

            if (booking.getShow() == null) {
                booking.setShow(showSeat.getShow());
            }
        }

        booking.setTotalAmount(total);
        Booking saved = bookingRepository.save(booking);
        sendConfirmation(saved);
        return saved;
    }

    /**
     * Waitlist path: the seat is already HELD as part of an accepted offer,
     * with no SeatHold row (it went through WaitlistOffer instead). Single
     * seat only, since waitlist offers are per-seat-category, one at a time.
     */
    @Transactional
    public Booking confirmBookingFromWaitlistOffer(WaitlistOffer offer, User customer) {
        ShowSeat showSeat = showSeatRepository.findByIdForUpdate(offer.getShowSeat().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

        if (showSeat.getStatus() != ShowSeat.Status.HELD) {
            throw new SeatUnavailableException("Seat is no longer available");
        }

        showSeat.setStatus(ShowSeat.Status.BOOKED);
        showSeatRepository.save(showSeat);
        waitlistService.acceptOffer(offer);

        BigDecimal price = showCategoryPriceRepository
                .findByShowIdAndCategoryId(showSeat.getShow().getId(), showSeat.getSeat().getCategory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Price not set for this category"))
                .getPrice();

        Booking booking = Booking.builder()
                .bookingReference(generateReference())
                .customer(customer)
                .show(showSeat.getShow())
                .status(Booking.Status.CONFIRMED)
                .totalAmount(price)
                .createdAt(Instant.now())
                .build();

        BookingSeat bookingSeat = BookingSeat.builder()
                .booking(booking)
                .showSeat(showSeat)
                .price(price)
                .build();
        booking.getSeats().add(bookingSeat);

        Booking saved = bookingRepository.save(booking);
        sendConfirmation(saved);
        return saved;
    }

    @Transactional
    public void cancelBooking(Long bookingId, User customer) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new SeatUnavailableException("You cannot cancel someone else's booking");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            return; // idempotent
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        // For each freed seat, hand it to the waitlist (or release it to
        // general availability if nobody is waiting) instead of just
        // flipping it back to AVAILABLE directly.
        for (BookingSeat bs : booking.getSeats()) {
            waitlistService.offerNextInLine(bs.getShowSeat());
        }
    }

    private void sendConfirmation(Booking booking) {
        byte[] qr = qrCodeService.generateQrPng(booking.getBookingReference(), 300);
        emailService.sendBookingConfirmation(
                booking.getCustomer().getEmail(),
                booking.getCustomer().getName(),
                booking.getShow().getTitle(),
                booking.getBookingReference(),
                qr
        );
    }

    private String generateReference() {
        return "TBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
