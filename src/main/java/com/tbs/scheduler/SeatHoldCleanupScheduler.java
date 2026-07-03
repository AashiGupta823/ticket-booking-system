package com.tbs.scheduler;

import com.tbs.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs on a fixed interval (app.seat-hold.cleanup-interval-ms, default 30s)
 * and releases any seat hold whose TTL has passed without being converted
 * into a booking. This is the mechanism behind "held seats auto-release on
 * checkout abandonment" - no action from the customer or frontend is needed.
 */
@Component
@RequiredArgsConstructor
public class SeatHoldCleanupScheduler {

    private final SeatHoldService seatHoldService;

    @Scheduled(fixedRateString = "${app.seat-hold.cleanup-interval-ms}")
    public void releaseExpiredHolds() {
        seatHoldService.releaseExpiredHolds();
    }
}
