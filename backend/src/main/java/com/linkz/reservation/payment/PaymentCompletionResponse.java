package com.linkz.reservation.payment;

public record PaymentCompletionResponse(
        Long paymentId,
        String status,
        String reservationStatus
) {}

