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
}

