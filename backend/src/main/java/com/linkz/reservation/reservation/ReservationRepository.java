package com.linkz.reservation.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    Optional<Reservation> findByUserIdAndSeatId(Long userId, Long seatId);

    @Query(value = """
            SELECT *
            FROM reservations r
            WHERE r.user_id = :userId
              AND r.seat_id = :seatId
              AND r.status IN ('PENDING_PAYMENT', 'CONFIRMED')
            ORDER BY r.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Reservation> findActiveReservationByUserAndSeat(Long userId, Long seatId);
    
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime dateTime);

    @Query("""
            SELECT r.id
            FROM Reservation r
            WHERE r.status = :status
              AND r.createdAt < :dateTime
            """)
    List<Long> findIdsByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime dateTime);

    @EntityGraph(attributePaths = "seat")
    @Query("SELECT r FROM Reservation r WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithSeat(Long reservationId);
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.seat.id = :seatId AND r.status = 'CONFIRMED'")
    long countConfirmedBySeatId(Long seatId);
}
