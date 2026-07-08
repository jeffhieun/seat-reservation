package com.linkz.reservation.payment;

import jakarta.validation.Valid;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        User user = findUser(authentication);

        log.debug("Initiating payment for reservation: {}", reservationId);

        PaymentInitiationResult result = paymentService.initiatePayment(reservationId, user.getId());

        if (result.created()) {
            log.info("Payment initiated: {} for reservation: {}", result.payment().getId(), reservationId);
            return ResponseEntity.status(201)
                    .body(PaymentResponse.from(result.payment()));
        }

        log.debug("Returning existing pending payment: {} for reservation: {}", result.payment().getId(), reservationId);
        return ResponseEntity.ok(PaymentResponse.from(result.payment()));
    }
    
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId, Authentication authentication) {
        User user = findUser(authentication);
        Payment payment = paymentService.getPaymentByIdForUser(paymentId, user.getId());
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentCompletionRequest request,
            Authentication authentication) {
        User user = findUser(authentication);
        PaymentResponse response = paymentCompletionService.completePayment(paymentId, user.getId(), request.result());
        return ResponseEntity.ok(response);
    }

    private User findUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
