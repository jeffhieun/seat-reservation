package com.linkz.reservation.payment;

import com.linkz.reservation.audit.AuditService;
import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationService;
import com.linkz.reservation.reservation.ReservationStatus;
import com.linkz.reservation.seat.SeatStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final AuditService auditService;
    
    @Transactional
    public Payment initiatePayment(Long reservationId, Long userId) {
        Reservation reservation = reservationService.getReservationById(reservationId);

        if (!reservation.getUser().getId().equals(userId)) {
            throw new PaymentAccessDeniedException("Reservation does not belong to the authenticated user");
        }

        if (!ReservationStatus.PENDING_PAYMENT.equals(reservation.getStatus())
                || !SeatStatus.PENDING_PAYMENT.equals(reservation.getSeat().getStatus())) {
            throw new PaymentConflictException("Payment can only be initiated for reservations pending payment");
        }
        
        if (paymentRepository.findByReservationId(reservationId).isPresent()) {
            throw new PaymentConflictException("Payment already initiated for this reservation");
        }
        
        Payment payment = Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .providerReference(generateProviderReference())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        auditService.recordPaymentInitiated(savedPayment);
        return savedPayment;
    }
    
    @Transactional
    public Payment markPaymentSuccess(String providerReference) {
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        payment.setStatus(PaymentStatus.SUCCESS);
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public Payment markPaymentFailed(String providerReference) {
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        payment.setStatus(PaymentStatus.FAILED);
        return paymentRepository.save(payment);
    }
    
    @Transactional(readOnly = true)
    public Payment getPaymentByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByIdForUser(Long paymentId, Long userId) {
        Payment payment = getPaymentById(paymentId);

        if (!payment.getReservation().getUser().getId().equals(userId)) {
            throw new PaymentAccessDeniedException("Payment does not belong to the authenticated user");
        }

        return payment;
    }
    
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }
    
    private String generateProviderReference() {
        return "PAY_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();
    }
}

