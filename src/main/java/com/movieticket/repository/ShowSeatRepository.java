package com.movieticket.repository;

import com.movieticket.entity.ShowSeat;
import com.movieticket.entity.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    Optional<ShowSeat> findByShowIdAndSeatId(Long showId, Long seatId);

    List<ShowSeat> findByShowIdAndIdIn(Long showId, List<Long> ids);

    /** Find holds that have expired so scheduler can release them */
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = :status AND ss.holdExpiresAt < :now")
    List<ShowSeat> findExpiredHolds(SeatStatus status, LocalDateTime now);
}
