package com.linkz.reservation.payment;

import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final PaymentCompletionService paymentCompletionService;
    private final UserRepository userRepository;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initiatePayment(@RequestParam Long reservationId, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            log.debug("Initiating payment for reservation: {}", reservationId);

            PaymentInitiationResult result = paymentService.initiatePayment(reservationId, user.getId());

            if (result.created()) {
                log.info("Payment initiated: {} for reservation: {}", result.payment().getId(), reservationId);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(PaymentResponse.from(result.payment()));
            } else {
                log.debug("Returning existing pending payment: {} for reservation: {}", result.payment().getId(), reservationId);
                return ResponseEntity.ok(PaymentResponse.from(result.payment()));
            }
        } catch (PaymentAccessDeniedException e) {
            log.warn("Payment access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (PaymentConflictException e) {
            log.warn("Payment initiation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            log.warn("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Unexpected error during payment initiation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Payment payment = paymentService.getPaymentByIdForUser(paymentId, user.getId());
            return ResponseEntity.ok(PaymentResponse.from(payment));
        } catch (PaymentAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> completePayment(@PathVariable Long paymentId, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            PaymentCompletionResponse response = paymentCompletionService.completePayment(paymentId, user.getId());
            return ResponseEntity.ok(response);
        } catch (PaymentAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (PaymentConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error during payment completion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
