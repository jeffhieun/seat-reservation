package com.linkz.reservation.reservation;

public class SeatUnavailableException extends ReservationException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
