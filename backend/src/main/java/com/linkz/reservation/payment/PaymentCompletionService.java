package com.linkz.reservation.payment;

import com.linkz.reservation.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCompletionService {

    private final PaymentService paymentService;
    private final WebhookService webhookService;

    @Transactional
    public PaymentCompletionResponse completePayment(Long paymentId, Long userId) {
        Payment payment = paymentService.getPaymentByIdForUser(paymentId, userId);

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            return toResponse(payment);
        }

        if (PaymentStatus.FAILED.equals(payment.getStatus())) {
            throw new PaymentConflictException("Failed payment cannot be completed");
        }

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new PaymentConflictException("Payment cannot be completed from current status");
        }

        String eventId = "evt-local-complete-" + paymentId + "-" + UUID.randomUUID();
        webhookService.processPaymentSuccess(eventId, payment.getProviderReference());

        Payment updatedPayment = paymentService.getPaymentByIdForUser(paymentId, userId);
        return toResponse(updatedPayment);
    }

    private PaymentCompletionResponse toResponse(Payment payment) {
        return new PaymentCompletionResponse(
                payment.getId(),
                payment.getStatus().name(),
                payment.getReservation().getStatus().name()
        );
    }
}

