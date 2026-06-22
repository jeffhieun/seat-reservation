package com.linkz.reservation.payment;

/**
 * Wraps the result of a payment initiation attempt.
 *
 * @param payment the payment record (newly created or pre-existing)
 * @param created {@code true} if the payment was just created, {@code false} if it already existed
 *                and was returned idempotently
 */
public record PaymentInitiationResult(Payment payment, boolean created) {
}

