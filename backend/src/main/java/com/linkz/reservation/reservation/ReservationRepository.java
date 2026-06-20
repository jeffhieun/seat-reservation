package com.linkz.reservation.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    Optional<Reservation> findByUserIdAndSeatId(Long userId, Long seatId);
    
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime dateTime);
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.seat.id = :seatId AND r.status = 'CONFIRMED'")
    long countConfirmedBySeatId(Long seatId);
}

