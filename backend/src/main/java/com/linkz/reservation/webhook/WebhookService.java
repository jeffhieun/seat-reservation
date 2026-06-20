package com.linkz.reservation.webhook;

import com.linkz.reservation.audit.AuditService;
import com.linkz.reservation.payment.Payment;
import com.linkz.reservation.payment.PaymentRepository;
import com.linkz.reservation.payment.PaymentService;
import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookService {
    
    private final WebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final AuditService auditService;
    
    @Transactional
    public void processPaymentSuccess(String eventId, String providerReference) {
        // Check if event was already processed (idempotency)
        if (webhookEventRepository.existsByEventId(eventId)) {
            return;  // Event already processed, return early
        }
        
        // Record the webhook event for future idempotency checks
        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .build();
        webhookEventRepository.save(event);
        
        // Find payment by provider reference
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        auditService.recordWebhookPaymentSuccessReceived(eventId, payment);

        // Update payment status through the payment service so audit tracking stays consistent
        Payment updatedPayment = paymentService.markPaymentSuccess(providerReference);
        auditService.recordPaymentSuccess(updatedPayment);
        
        // Confirm the reservation (which also updates seat status)
        Reservation reservation = payment.getReservation();
        reservationService.confirmReservation(reservation.getId());
    }
    
    @Transactional
    public void processPaymentFailure(String eventId, String providerReference) {
        // Check if event was already processed
        if (webhookEventRepository.existsByEventId(eventId)) {
            return;
        }
        
        // Record the webhook event
        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .build();
        webhookEventRepository.save(event);
        
        // Find and update payment
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        auditService.recordWebhookPaymentFailureReceived(eventId, payment);

        Payment updatedPayment = paymentService.markPaymentFailed(providerReference);
        auditService.recordPaymentFailure(updatedPayment);
    }
}

