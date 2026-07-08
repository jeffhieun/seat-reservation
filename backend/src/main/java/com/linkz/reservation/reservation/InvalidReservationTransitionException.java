package com.linkz.reservation.reservation;

public class InvalidReservationTransitionException extends RuntimeException {
    public InvalidReservationTransitionException(String message) {
        super(message);
    }
}

