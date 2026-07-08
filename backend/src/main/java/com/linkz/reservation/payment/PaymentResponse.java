package com.linkz.reservation.payment;

public record PaymentResponse(
    Long id,
    Long reservationId,
    String reservationStatus,
    String status,
    String providerReference,
    String amount,
    String createdAt,
    String updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getReservation().getStatus().name(),
                payment.getStatus().name(),
                payment.getProviderReference(),
                payment.getAmount().toPlainString(),
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null,
                payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : null
        );
    }
}
