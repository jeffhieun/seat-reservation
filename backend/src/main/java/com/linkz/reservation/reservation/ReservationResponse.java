package com.linkz.reservation.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
    Long id,
    @JsonProperty("seat_id")
    Long seatId,
    @JsonProperty("seat_number")
    String seatNumber,
    String status,
    @JsonProperty("created_at")
    String createdAt,
    @JsonProperty("confirmed_at")
    String confirmedAt,
    @JsonProperty("expired_at")
    String expiredAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeat().getId(),
                reservation.getSeat().getSeatNumber(),
                reservation.getStatus().name(),
                reservation.getCreatedAt().toString(),
                reservation.getConfirmedAt() != null ? reservation.getConfirmedAt().toString() : null,
                reservation.getExpiredAt() != null ? reservation.getExpiredAt().toString() : null
        );
    }
}

