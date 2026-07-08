package com.linkz.reservation.reservation;

import com.linkz.reservation.audit.AuditService;
import com.linkz.reservation.seat.Seat;
import com.linkz.reservation.seat.SeatRepository;
import com.linkz.reservation.seat.SeatStatus;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
        Instant startedAt = Instant.now();
        log.debug("Reservation attempt started userId={} seatId={}", userId, seatId);

        User user = findUserOrThrow(userId);
        validateDuplicateReservation(userId, seatId);

        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
        log.debug("Seat lock acquired userId={} seatId={}", userId, seatId);
        validateSeat(seat);

        seat.setStatus(SeatStatus.PENDING_PAYMENT);
        seatRepository.saveAndFlush(seat);

        Reservation reservation = Reservation.builder()
                .user(user)
                .seat(seat)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        auditService.recordReservationCreated(savedReservation);
        log.debug(
                "Reservation created reservationId={} seatId={} userId={} transactionDurationMs={}",
                savedReservation.getId(),
                seatId,
                userId,
                Duration.between(startedAt, Instant.now()).toMillis()
        );
        return savedReservation;
    }

    @Transactional
    public void confirmReservation(Long reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        validateTransition(reservation, ReservationStatus.CONFIRMED);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservation.getSeat().setStatus(SeatStatus.RESERVED);

        reservationRepository.saveAndFlush(reservation);
        seatRepository.saveAndFlush(reservation.getSeat());

        log.info("Reservation {} confirmed at {}", reservationId, reservation.getConfirmedAt());
        auditService.recordReservationConfirmed(reservation);
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId, Long userId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        validateOwnership(reservation, userId);
        validateTransition(reservation, ReservationStatus.CONFIRMED);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservation.getSeat().setStatus(SeatStatus.RESERVED);

        Reservation updatedReservation = reservationRepository.saveAndFlush(reservation);
        seatRepository.saveAndFlush(updatedReservation.getSeat());
        auditService.recordReservationConfirmed(updatedReservation);
        return updatedReservation;
    }

    @Transactional
    public Reservation expireReservation(Long reservationId) {
        Reservation reservation = findReservationWithSeatOrThrow(reservationId);
        validateTransition(reservation, ReservationStatus.EXPIRED);

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setExpiredAt(LocalDateTime.now());
        reservation.getSeat().setStatus(SeatStatus.AVAILABLE);

        Reservation updatedReservation = reservationRepository.saveAndFlush(reservation);
        seatRepository.saveAndFlush(updatedReservation.getSeat());

        log.info("Reservation {} expired at {}", updatedReservation.getId(), updatedReservation.getExpiredAt());
        auditService.recordReservationExpired(updatedReservation);
        return updatedReservation;
    }

    @Transactional
    public Reservation expireReservation(Long reservationId, Long userId) {
        Reservation reservation = findReservationWithSeatOrThrow(reservationId);
        validateOwnership(reservation, userId);
        validateTransition(reservation, ReservationStatus.EXPIRED);

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setExpiredAt(LocalDateTime.now());
        reservation.getSeat().setStatus(SeatStatus.AVAILABLE);

        Reservation updatedReservation = reservationRepository.saveAndFlush(reservation);
        seatRepository.saveAndFlush(updatedReservation.getSeat());
        auditService.recordReservationExpired(updatedReservation);
        return updatedReservation;
    }

    public Reservation getReservationById(Long reservationId) {
        return findReservationOrThrow(reservationId);
    }

    public Reservation getReservationById(Long reservationId, Long userId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        validateOwnership(reservation, userId);
        return reservation;
    }

    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Long> getExpiredReservationIds() {
        LocalDateTime expirationThreshold = LocalDateTime.now()
                .minusMinutes(reservationProperties.getExpirationMinutes());

        return reservationRepository.findIdsByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING_PAYMENT, 
                expirationThreshold
        );
    }

    public void validateDuplicateReservation(Long userId, Long seatId) {
        reservationRepository.findActiveReservationByUserAndSeat(userId, seatId)
                .ifPresent(reservation -> {
            throw new DuplicateReservationException("You already have an active reservation for this seat.");
        });
    }

    public void validateSeat(Seat seat) {
        if (!SeatStatus.AVAILABLE.equals(seat.getStatus())) {
            throw new SeatUnavailableException("Seat is not available");
        }
    }

    public void validateOwnership(Reservation reservation, Long userId) {
        if (!reservation.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You cannot access this reservation.");
        }
    }

    public void validateTransition(Reservation reservation, ReservationStatus targetStatus) {
        if (targetStatus == ReservationStatus.CONFIRMED) {
            if (!ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())
                    || !SeatStatus.PENDING_PAYMENT.equals(reservation.getSeat().getStatus())) {
                throw new InvalidReservationTransitionException("Reservation can only be confirmed from PENDING_PAYMENT.");
            }
            return;
        }

        if (targetStatus == ReservationStatus.EXPIRED) {
            if (!ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())
                    || !SeatStatus.PENDING_PAYMENT.equals(reservation.getSeat().getStatus())) {
                throw new InvalidReservationTransitionException("Reservation can only be expired from PENDING_PAYMENT.");
            }
            return;
        }

        throw new InvalidReservationTransitionException("Unsupported reservation transition.");
    }

    public Reservation findReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found."));
    }

    public Reservation findReservationWithSeatOrThrow(Long reservationId) {
        return reservationRepository.findByIdWithSeat(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found."));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
