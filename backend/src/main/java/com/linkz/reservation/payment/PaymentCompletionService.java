package com.linkz.reservation.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

    private final PaymentService paymentService;

    @Transactional
    public PaymentResponse completePayment(Long paymentId, Long userId, String result) {
        return PaymentResponse.from(paymentService.completePayment(paymentId, userId, result));
    }
}
