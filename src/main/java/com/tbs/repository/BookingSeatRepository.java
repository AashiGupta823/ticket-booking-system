package com.tbs.repository;

import com.tbs.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
}
