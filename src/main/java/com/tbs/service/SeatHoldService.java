package com.tbs.service;

import com.tbs.entity.*;
import com.tbs.exception.ResourceNotFoundException;
import com.tbs.exception.SeatUnavailableException;
import com.tbs.repository.SeatHoldRepository;
import com.tbs.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * All seat hold and release logic lives here. This is the piece that has to
 * be correct under concurrent access: two customers clicking the same seat
 * within milliseconds of each other must never both succeed.
 *
 * How concurrency is prevented:
 *   1. findByIdForUpdate() takes a DB-level row lock (SELECT ... FOR UPDATE)
 *      on the ShowSeat row, inside a single @Transactional method.
 *   2. Whichever request's transaction gets there first holds the lock;
 *      the second request's query blocks at the database level until the
 *      first transaction commits (or rolls back).
 *   3. When the second request's lock is finally granted, it re-reads the
 *      seat's status - which by now reflects the first request's write - so
 *      it sees HELD/BOOKED and is correctly rejected.
 *   4. The @Version field on ShowSeat is a secondary optimistic-lock guard
 *      in case the row is ever touched via a path that skips the pessimistic
 *      lock (defense in depth, not the primary mechanism).
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Value("${app.seat-hold.ttl-minutes}")
    private int holdTtlMinutes;

    @Transactional
    public SeatHold holdSeat(Long showSeatId, User customer) {
        ShowSeat showSeat = showSeatRepository.findByIdForUpdate(showSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

        if (showSeat.getStatus() != ShowSeat.Status.AVAILABLE) {
            throw new SeatUnavailableException(
                    "Seat is no longer available (status: " + showSeat.getStatus() + ")");
        }

        showSeat.setStatus(ShowSeat.Status.HELD);
        showSeatRepository.save(showSeat); // still inside the lock/transaction

        Instant now = Instant.now();
        SeatHold hold = SeatHold.builder()
                .showSeat(showSeat)
                .customer(customer)
                .createdAt(now)
                .expiresAt(now.plus(holdTtlMinutes, ChronoUnit.MINUTES))
                .consumed(false)
                .build();

        return seatHoldRepository.save(hold);
        // Row lock is released automatically when the transaction commits here.
    }

    @Transactional
    public void releaseHold(Long showSeatId) {
        ShowSeat showSeat = showSeatRepository.findByIdForUpdate(showSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

        seatHoldRepository.findByShowSeatId(showSeatId).ifPresent(hold -> {
            if (!hold.isConsumed()) {
                seatHoldRepository.delete(hold);
            }
        });

        if (showSeat.getStatus() == ShowSeat.Status.HELD) {
            showSeat.setStatus(ShowSeat.Status.AVAILABLE);
            showSeatRepository.save(showSeat);
        }
    }

    /**
     * Called on a fixed schedule (see SeatHoldCleanupScheduler). Finds every
     * hold whose TTL has passed and was never converted into a booking, and
     * releases the seat back to AVAILABLE. This is what makes an abandoned
     * checkout self-heal without any action from the customer.
     */
    @Transactional
    public void releaseExpiredHolds() {
        List<SeatHold> expired = seatHoldRepository.findByExpiresAtBeforeAndConsumedFalse(Instant.now());
        for (SeatHold hold : expired) {
            releaseHold(hold.getShowSeat().getId());
        }
    }

    @Transactional
    public void markConsumed(Long showSeatId) {
        seatHoldRepository.findByShowSeatId(showSeatId)
                .ifPresent(hold -> {
                    hold.setConsumed(true);
                    seatHoldRepository.save(hold);
                });
    }
}
