package com.linkz.reservation.payment;

import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class DevPaymentController {

    private final PaymentCompletionService paymentCompletionService;
    private final UserRepository userRepository;

    @PostMapping("/{paymentId}/success")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> completePaymentSuccess(
            @PathVariable Long paymentId,
            Authentication authentication) {
        User user = findUser(authentication);
        PaymentResponse response = paymentCompletionService.completePayment(paymentId, user.getId(), "SUCCESS");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/failure")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> completePaymentFailure(
            @PathVariable Long paymentId,
            Authentication authentication) {
        User user = findUser(authentication);
        PaymentResponse response = paymentCompletionService.completePayment(paymentId, user.getId(), "FAILED");
        return ResponseEntity.ok(response);
    }

    private User findUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
