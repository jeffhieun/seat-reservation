package com.linkz.reservation.reservation;

import com.linkz.reservation.audit.AuditService;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ReservationProperties reservationProperties;
    private final AuditService auditService;
    
    @Transactional
    public Reservation reserveSeat(Long userId, Long seatId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
        
        // Verify seat is available
        if (!SeatStatus.AVAILABLE.equals(seat.getStatus())) {
            throw new SeatUnavailableException("Seat is not available");
        }
        
        // Update seat status to PENDING_PAYMENT
        seat.setStatus(SeatStatus.PENDING_PAYMENT);
        seatRepository.saveAndFlush(seat);
        
        // Create reservation
        Reservation reservation = Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();
        
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        auditService.recordReservationCreated(savedReservation);
        return savedReservation;
    }
    
    @Transactional
    public void confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        
        if (!ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())) {
            throw new IllegalArgumentException("Invalid reservation status for confirmation");
        }
        
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservation.getSeat().setStatus(SeatStatus.RESERVED);
        
        reservationRepository.saveAndFlush(reservation);
        seatRepository.saveAndFlush(reservation.getSeat());
        
        log.info("Reservation {} confirmed at {}", reservationId, reservation.getConfirmedAt());
        auditService.recordReservationConfirmed(reservation);
    }
    
    @Transactional
    public void expireReservation(Reservation reservation) {
        if (ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setExpiredAt(LocalDateTime.now());
            reservation.getSeat().setStatus(SeatStatus.AVAILABLE);
            
            reservationRepository.saveAndFlush(reservation);
            seatRepository.saveAndFlush(reservation.getSeat());
            
            log.info("Reservation {} expired at {}", reservation.getId(), reservation.getExpiredAt());
            auditService.recordReservationExpired(reservation);
        }
    }
    
    public Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }
    
    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Reservation> getExpiredReservations() {
        LocalDateTime expirationThreshold = LocalDateTime.now()
                .minusMinutes(reservationProperties.getExpirationMinutes());
        
        return reservationRepository.findByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING_PAYMENT, 
                expirationThreshold
        );
    }
}

