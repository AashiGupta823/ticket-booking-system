package com.tbs.service;

import com.tbs.entity.*;
import com.tbs.exception.OfferExpiredException;
import com.tbs.exception.ResourceNotFoundException;
import com.tbs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Waitlist queue + auto-assignment + time-limited-offer flow.
 *
 * Flow:
 *  1. Customer joins the waitlist for a (show, category) when sold out.
 *  2. On cancellation of a booked seat in that category, offerNextInLine()
 *     is called: it pops the earliest WAITING entry (FIFO by joinedAt),
 *     puts the freed seat into HELD status (reserved, not generally
 *     bookable), creates a WaitlistOffer with its own TTL and a unique
 *     token, and emails the customer a link containing that token.
 *  3. If the customer opens the link and completes booking before the
 *     offer's expiresAt, the offer is ACCEPTED and normal booking proceeds.
 *  4. If the TTL lapses first, the scheduled cleanup job (see
 *     WaitlistOfferCleanupScheduler) marks the offer EXPIRED and
 *     immediately cascades to the next person in the queue by calling
 *     offerNextInLine() again for the same seat. If nobody is left waiting,
 *     the seat is released back to AVAILABLE for general sale.
 */
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final WaitlistOfferRepository waitlistOfferRepository;
    private final ShowSeatRepository showSeatRepository;
    private final EmailService emailService;

    @Value("${app.waitlist.offer-ttl-minutes}")
    private int offerTtlMinutes;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public Waitlist joinWaitlist(Show show, SeatCategory category, User customer) {
        Waitlist entry = Waitlist.builder()
                .show(show)
                .category(category)
                .customer(customer)
                .joinedAt(Instant.now())
                .status(Waitlist.Status.WAITING)
                .build();
        return waitlistRepository.save(entry);
    }

    /**
     * Called when a seat in `category` for `show` becomes free (booking
     * cancellation, or a previous offer expiring). Locks the seat, checks
     * for the next waiting customer, and either creates a new time-limited
     * offer for them or, if the queue is empty, releases the seat back to
     * general availability.
     */
    @Transactional
    public void offerNextInLine(ShowSeat freedShowSeat) {
        ShowSeat showSeat = showSeatRepository.findByIdForUpdate(freedShowSeat.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

        Long showId = showSeat.getShow().getId();
        Long categoryId = showSeat.getSeat().getCategory().getId();

        var next = waitlistRepository.findFirstByShowIdAndCategoryIdAndStatusOrderByJoinedAtAsc(
                showId, categoryId, Waitlist.Status.WAITING);

        if (next.isEmpty()) {
            showSeat.setStatus(ShowSeat.Status.AVAILABLE);
            showSeatRepository.save(showSeat);
            return;
        }

        Waitlist entry = next.get();
        entry.setStatus(Waitlist.Status.OFFERED);
        waitlistRepository.save(entry);

        showSeat.setStatus(ShowSeat.Status.HELD);
        showSeatRepository.save(showSeat);

        Instant now = Instant.now();
        WaitlistOffer offer = WaitlistOffer.builder()
                .waitlistEntry(entry)
                .showSeat(showSeat)
                .offerToken(UUID.randomUUID().toString())
                .createdAt(now)
                .expiresAt(now.plus(offerTtlMinutes, ChronoUnit.MINUTES))
                .status(WaitlistOffer.Status.PENDING)
                .build();
        waitlistOfferRepository.save(offer);

        String offerUrl = frontendBaseUrl + "/waitlist-offer/" + offer.getOfferToken();
        emailService.sendWaitlistOffer(
                entry.getCustomer().getEmail(),
                entry.getCustomer().getName(),
                showSeat.getShow().getTitle(),
                offerUrl,
                offerTtlMinutes
        );
    }

    /**
     * Validates an offer token at "accept" time. Returns the still-valid
     * offer, or throws if it's already expired/used - the booking
     * controller uses this before letting the customer proceed to confirm.
     */
    @Transactional
    public WaitlistOffer validateOffer(String token) {
        WaitlistOffer offer = waitlistOfferRepository.findByOfferToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getStatus() != WaitlistOffer.Status.PENDING) {
            throw new OfferExpiredException("This offer is no longer valid");
        }
        if (offer.getExpiresAt().isBefore(Instant.now())) {
            expireOffer(offer);
            throw new OfferExpiredException("This offer has expired");
        }
        return offer;
    }

    @Transactional
    public void acceptOffer(WaitlistOffer offer) {
        offer.setStatus(WaitlistOffer.Status.ACCEPTED);
        waitlistOfferRepository.save(offer);

        Waitlist entry = offer.getWaitlistEntry();
        entry.setStatus(Waitlist.Status.BOOKED);
        waitlistRepository.save(entry);
    }

    @Transactional
    public void expireOffer(WaitlistOffer offer) {
        offer.setStatus(WaitlistOffer.Status.EXPIRED);
        waitlistOfferRepository.save(offer);

        Waitlist entry = offer.getWaitlistEntry();
        entry.setStatus(Waitlist.Status.EXPIRED);
        waitlistRepository.save(entry);

        // Cascade: offer the same seat to whoever is now next in line.
        offerNextInLine(offer.getShowSeat());
    }

    /** Picked up by the scheduler on a fixed interval. */
    @Transactional
    public void expireStaleOffers() {
        List<WaitlistOffer> stale = waitlistOfferRepository.findByExpiresAtBeforeAndStatus(
                Instant.now(), WaitlistOffer.Status.PENDING);
        for (WaitlistOffer offer : stale) {
            expireOffer(offer);
        }
    }
}
