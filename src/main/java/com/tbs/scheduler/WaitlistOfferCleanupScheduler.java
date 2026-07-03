package com.tbs.scheduler;

import com.tbs.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs on the same interval as the seat-hold cleanup. Expires any
 * waitlist offer whose time-limited window has passed, and (inside
 * WaitlistService.expireOffer) immediately cascades the seat to the next
 * person in the queue, or releases it to general availability if the
 * queue is empty.
 */
@Component
@RequiredArgsConstructor
public class WaitlistOfferCleanupScheduler {

    private final WaitlistService waitlistService;

    @Scheduled(fixedRateString = "${app.seat-hold.cleanup-interval-ms}")
    public void expireStaleOffers() {
        waitlistService.expireStaleOffers();
    }
}
