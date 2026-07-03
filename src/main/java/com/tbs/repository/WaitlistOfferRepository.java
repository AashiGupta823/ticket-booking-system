package com.tbs.repository;

import com.tbs.entity.WaitlistOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WaitlistOfferRepository extends JpaRepository<WaitlistOffer, Long> {
    Optional<WaitlistOffer> findByOfferToken(String offerToken);
    List<WaitlistOffer> findByExpiresAtBeforeAndStatus(Instant cutoff, WaitlistOffer.Status status);
}
