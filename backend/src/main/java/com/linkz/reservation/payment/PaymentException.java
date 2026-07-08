package com.linkz.reservation.payment;

public abstract class PaymentException extends RuntimeException {
    protected PaymentException(String message) {
        super(message);
    }
}
