package com.tbs.repository;

import com.tbs.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    Optional<SeatHold> findByShowSeatId(Long showSeatId);

    // Picked up by the scheduled cleanup job: anything expired and not yet
    // consumed by a booking gets released.
    List<SeatHold> findByExpiresAtBeforeAndConsumedFalse(Instant cutoff);

    List<SeatHold> findByCustomerIdAndConsumedFalse(Long customerId);
}
