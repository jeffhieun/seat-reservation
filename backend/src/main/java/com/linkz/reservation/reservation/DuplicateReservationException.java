package com.linkz.reservation.reservation;

public class DuplicateReservationException extends RuntimeException {
    public DuplicateReservationException(String message) {
        super(message);
    }
}

