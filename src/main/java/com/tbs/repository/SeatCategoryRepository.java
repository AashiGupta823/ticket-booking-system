package com.tbs.repository;

import com.tbs.entity.SeatCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatCategoryRepository extends JpaRepository<SeatCategory, Long> {
    List<SeatCategory> findByVenueId(Long venueId);
}
