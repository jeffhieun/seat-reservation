package com.linkz.reservation.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    
    private final ReservationService reservationService;
    private final UserRepository userRepository;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reserveSeat(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            log.debug("Reservation attempt for user: {} and seat: {}", user.getId(), request.seatId());
            
            Reservation reservation = reservationService.reserveSeat(user.getId(), request.seatId());
            
            log.info("Reservation created: {} for user: {}", reservation.getId(), user.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ReservationResponse.from(reservation));
        } catch (SeatUnavailableException e) {
            log.warn("Reservation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            log.warn("Reservation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Unexpected error during reservation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReservationResponse>> getUserReservations(
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<ReservationResponse> reservations = reservationService.getUserReservations(user.getId())
                .stream()
                .map(ReservationResponse::from)
                .toList();
        
        return ResponseEntity.ok(reservations);
    }
    
    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getReservationDetails(
            @PathVariable Long reservationId,
            Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Reservation reservation = reservationService.getReservationById(reservationId);
            
            // Verify ownership
            if (!reservation.getUser().getId().equals(user.getId())) {
                log.warn("Access denied: User {} trying to access reservation {}", user.getId(), reservationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(ReservationResponse.from(reservation));
        } catch (IllegalArgumentException e) {
            log.warn("Reservation not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @PostMapping("/{reservationId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirmReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            Reservation reservation = reservationService.getReservationById(reservationId);
            
            // Verify ownership
            if (!reservation.getUser().getId().equals(user.getId())) {
                log.warn("Access denied: User {} trying to confirm reservation {}", user.getId(), reservationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            log.debug("Confirming reservation {} for user {}", reservationId, user.getId());
            reservationService.confirmReservation(reservationId);
            
            Reservation confirmedReservation = reservationService.getReservationById(reservationId);
            log.info("Reservation {} confirmed by user {}", reservationId, user.getId());
            
            return ResponseEntity.ok(ReservationResponse.from(confirmedReservation));
        } catch (IllegalArgumentException e) {
            log.warn("Confirmation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PostMapping("/{reservationId}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> manuallyExpireReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {
        try {
            Reservation reservation = reservationService.getReservationById(reservationId);
            
            log.debug("Manually expiring reservation {} by admin {}", reservationId, authentication.getName());
            reservationService.expireReservation(reservation);
            
            Reservation expiredReservation = reservationService.getReservationById(reservationId);
            log.info("Reservation {} manually expired by admin {}", reservationId, authentication.getName());
            
            return ResponseEntity.ok(ReservationResponse.from(expiredReservation));
        } catch (IllegalArgumentException e) {
            log.warn("Expiration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

