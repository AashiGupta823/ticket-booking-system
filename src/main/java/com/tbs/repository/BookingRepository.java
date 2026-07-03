package com.tbs.repository;

import com.tbs.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findByShowId(Long showId);
    Optional<Booking> findByBookingReference(String bookingReference);
}
