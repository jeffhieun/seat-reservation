package com.linkz.reservation.reservation;

public abstract class ReservationException extends RuntimeException {
    protected ReservationException(String message) {
        super(message);
    }
}
