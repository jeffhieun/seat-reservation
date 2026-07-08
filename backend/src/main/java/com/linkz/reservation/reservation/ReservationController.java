package com.linkz.reservation.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ReservationProperties reservationProperties;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> reserveSeat(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {
        User user = findUser(authentication);
        log.debug("Reservation attempt for user: {} and seat: {}", user.getId(), request.seatId());

        Reservation reservation = reservationService.reserveSeat(user.getId(), request.seatId());
        log.info("Reservation created: {} for user: {}", reservation.getId(), user.getId());

        return ResponseEntity.status(201)
                .body(ReservationResponse.from(reservation, reservationProperties.getExpirationMinutes()));
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReservationResponse>> getUserReservations(
            Authentication authentication) {
        User user = findUser(authentication);

        List<ReservationResponse> reservations = reservationService.getUserReservations(user.getId())
                .stream()
                .map(reservation -> ReservationResponse.from(reservation, reservationProperties.getExpirationMinutes()))
                .toList();

        return ResponseEntity.ok(reservations);
    }
    
    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<ReservationResponse> getReservationDetails(
            @PathVariable Long reservationId,
            Authentication authentication) {
        User user = findUser(authentication);
        Reservation reservation = reservationService.getReservationById(reservationId, user.getId());
        return ResponseEntity.ok(ReservationResponse.from(reservation, reservationProperties.getExpirationMinutes()));
    }
    
    @PostMapping("/{reservationId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {
        User user = findUser(authentication);
        log.debug("Confirming reservation {} for user {}", reservationId, user.getId());

        Reservation confirmedReservation = reservationService.confirmReservation(reservationId, user.getId());
        log.info("Reservation {} confirmed by user {}", reservationId, user.getId());

        return ResponseEntity.ok(ReservationResponse.from(confirmedReservation, reservationProperties.getExpirationMinutes()));
    }

    @PostMapping("/{reservationId}/expire")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> manuallyExpireReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {
        User user = findUser(authentication);
        log.debug("Manually expiring reservation {} by user {}", reservationId, user.getId());

        Reservation expiredReservation = reservationService.expireReservation(reservationId, user.getId());
        log.info("Reservation {} manually expired by user {}", reservationId, user.getId());

        return ResponseEntity.ok(ReservationResponse.from(expiredReservation, reservationProperties.getExpirationMinutes()));
    }

    private User findUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
