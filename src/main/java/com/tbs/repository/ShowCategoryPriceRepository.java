package com.tbs.repository;

import com.tbs.entity.ShowCategoryPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowCategoryPriceRepository extends JpaRepository<ShowCategoryPrice, Long> {
    List<ShowCategoryPrice> findByShowId(Long showId);
    Optional<ShowCategoryPrice> findByShowIdAndCategoryId(Long showId, Long categoryId);
}
