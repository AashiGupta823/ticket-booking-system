package com.tbs.repository;

import com.tbs.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndSeat_Category_Id(Long showId, Long categoryId);

    /**
     * Takes a row-level exclusive lock (SELECT ... FOR UPDATE) on the seat for
     * the duration of the enclosing transaction. This is what actually
     * prevents two concurrent hold/booking requests for the same seat from
     * both succeeding: the second transaction blocks here until the first
     * commits or rolls back, then re-reads the now-updated status.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id = :id")
    Optional<ShowSeat> findByIdForUpdate(@Param("id") Long id);

    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);
}
