package com.linkz.reservation.payment;

public class PaymentAccessDeniedException extends PaymentException {
    public PaymentAccessDeniedException(String message) {
        super(message);
    }
}
