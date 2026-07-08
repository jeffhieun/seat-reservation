package com.linkz.reservation.payment;

public class PaymentConflictException extends PaymentException {
    public PaymentConflictException(String message) {
        super(message);
    }
}
