package com.linkz.reservation.reservation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservationResponse(
    Long id,
    @JsonProperty("seat_id")
    Long seatId,
    @JsonProperty("seat_number")
    String seatNumber,
    String status,
    @JsonProperty("created_at")
    String createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeat().getId(),
                reservation.getSeat().getSeatNumber(),
                reservation.getStatus().name(),
                reservation.getCreatedAt().toString()
        );
    }
}

