package com.linkz.reservation.payment;

import com.linkz.reservation.reservation.Reservation;
import com.linkz.reservation.reservation.ReservationService;
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
    
    @Transactional
    public Payment initiatePayment(Long reservationId) {
        Reservation reservation = reservationService.getReservationById(reservationId);
        
        // Check if payment already exists for this reservation
        if (paymentRepository.findByReservationId(reservationId).isPresent()) {
            throw new IllegalArgumentException("Payment already initiated for this reservation");
        }
        
        Payment payment = Payment.builder()
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("10.00"))
                .providerReference(generateProviderReference())
                .build();
        
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public void markPaymentSuccess(String providerReference) {
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }
    
    @Transactional
    public void markPaymentFailed(String providerReference) {
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }
    
    public Payment getPaymentByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }
    
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }
    
    private String generateProviderReference() {
        return "PAY_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();
    }
}

