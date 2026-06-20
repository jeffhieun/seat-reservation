package com.linkz.reservation.audit;

import com.linkz.reservation.payment.Payment;
import com.linkz.reservation.payment.PaymentStatus;
import com.linkz.reservation.reservation.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void recordReservationCreated(Reservation reservation) {
        record(AuditEventType.RESERVATION_CREATED, reservation, null, null, "Reservation created");
    }

    @Transactional
    public void recordReservationConfirmed(Reservation reservation) {
        record(AuditEventType.RESERVATION_CONFIRMED, reservation, null, null, "Reservation confirmed");
    }

    @Transactional
    public void recordReservationExpired(Reservation reservation) {
        record(AuditEventType.RESERVATION_EXPIRED, reservation, null, null, "Reservation expired");
    }

    @Transactional
    public void recordPaymentInitiated(Payment payment) {
        record(AuditEventType.PAYMENT_INITIATED, payment.getReservation(), payment, payment.getProviderReference(), "Payment initiated");
    }

    @Transactional
    public void recordPaymentSuccess(Payment payment) {
        record(AuditEventType.PAYMENT_SUCCESS, payment.getReservation(), payment, payment.getProviderReference(), "Payment status updated to " + PaymentStatus.SUCCESS);
    }

    @Transactional
    public void recordPaymentFailure(Payment payment) {
        record(AuditEventType.PAYMENT_FAILED, payment.getReservation(), payment, payment.getProviderReference(), "Payment status updated to " + PaymentStatus.FAILED);
    }

    @Transactional
    public void recordWebhookPaymentSuccessReceived(String eventId, Payment payment) {
        record(AuditEventType.WEBHOOK_PAYMENT_SUCCESS_RECEIVED, payment.getReservation(), payment, eventId, "Webhook received for payment success");
    }

    @Transactional
    public void recordWebhookPaymentFailureReceived(String eventId, Payment payment) {
        record(AuditEventType.WEBHOOK_PAYMENT_FAILURE_RECEIVED, payment.getReservation(), payment, eventId, "Webhook received for payment failure");
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditEvent> getRecentEvents() {
        return auditEventRepository.findByOrderByCreatedAtDesc();
    }


    private void record(AuditEventType eventType, Reservation reservation, Payment payment, String referenceId, String details) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .reservationId(reservation != null ? reservation.getId() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .userId(reservation != null && reservation.getUser() != null ? reservation.getUser().getId() : null)
                .seatId(reservation != null && reservation.getSeat() != null ? reservation.getSeat().getId() : null)
                .referenceId(referenceId)
                .details(details)
                .build();
        auditEventRepository.save(event);
    }
}

