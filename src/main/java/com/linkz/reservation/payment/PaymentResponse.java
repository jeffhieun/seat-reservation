package com.linkz.reservation.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentResponse(
    Long id,
    @JsonProperty("reservation_id")
    Long reservationId,
    String status,
    @JsonProperty("provider_reference")
    String providerReference,
    String amount,
    @JsonProperty("created_at")
    String createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getStatus().name(),
                payment.getProviderReference(),
                payment.getAmount().toPlainString(),
                payment.getCreatedAt().toString()
        );
    }
}

