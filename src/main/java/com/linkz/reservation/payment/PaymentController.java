package com.linkz.reservation.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initiatePayment(@RequestParam Long reservationId) {
        try {
            log.debug("Initiating payment for reservation: {}", reservationId);
            
            Payment payment = paymentService.initiatePayment(reservationId);
            
            log.info("Payment initiated: {} for reservation: {}", payment.getId(), reservationId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(PaymentResponse.from(payment));
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
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(PaymentResponse.from(payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

