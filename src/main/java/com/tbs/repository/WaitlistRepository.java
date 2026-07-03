package com.tbs.repository;

import com.tbs.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    // FIFO: next person in line for this show+category who is still waiting.
    Optional<Waitlist> findFirstByShowIdAndCategoryIdAndStatusOrderByJoinedAtAsc(
            Long showId, Long categoryId, Waitlist.Status status);

    List<Waitlist> findByCustomerIdOrderByJoinedAtDesc(Long customerId);

    long countByShowIdAndCategoryIdAndStatus(Long showId, Long categoryId, Waitlist.Status status);
}
