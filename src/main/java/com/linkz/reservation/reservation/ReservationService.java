package com.linkz.reservation.reservation;

import com.linkz.reservation.payment.Payment;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.payment.PaymentStatus;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager entityManager;
    
    /**
     * Reserve a seat for a user with pessimistic write lock to prevent double booking.
     * This ensures that only one reservation succeeds for each seat.
     */
    @Transactional
    public Reservation reserveSeat(Long userId, Long seatId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Lock the seat row to prevent concurrent modifications
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
        
        // Apply pessimistic write lock
        seat = entityManager.find(Seat.class, seatId, LockModeType.PESSIMISTIC_WRITE);
        
        // Verify seat is available
        if (!SeatStatus.AVAILABLE.equals(seat.getStatus())) {
            throw new IllegalArgumentException("Seat is not available");
        }
        
        // Update seat status to PENDING_PAYMENT
        seat.setStatus(SeatStatus.PENDING_PAYMENT);
        seatRepository.save(seat);
        
        // Create reservation
        Reservation reservation = Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();
        
        return reservationRepository.save(reservation);
    }
    
    @Transactional
    public void confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        
        if (!ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Invalid reservation status for confirmation");
        }
        
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.getSeat().setStatus(SeatStatus.RESERVED);
        
        reservationRepository.save(reservation);
        seatRepository.save(reservation.getSeat());
    }
    
    @Transactional
    public void expireReservation(Reservation reservation) {
        if (ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.getSeat().setStatus(SeatStatus.AVAILABLE);
            
            reservationRepository.save(reservation);
            seatRepository.save(reservation.getSeat());
        }
    }
    
    public Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }
    
    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Reservation> getExpiredReservations(LocalDateTime before) {
        return reservationRepository.findByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING_PAYMENT, 
                before
        );
    }
}

