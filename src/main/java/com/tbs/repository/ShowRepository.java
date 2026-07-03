package com.tbs.repository;

import com.tbs.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByOrganiserId(Long organiserId);
    List<Show> findByTitleContainingIgnoreCase(String title);
}
