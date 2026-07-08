package com.linkz.reservation.reservation;

public class InvalidReservationTransitionException extends ReservationException {
    public InvalidReservationTransitionException(String message) {
        super(message);
    }
}
