package com.linkz.reservation.reservation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

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
    @JsonProperty("expires_at")
    String expiresAt,
    @JsonProperty("confirmed_at")
    String confirmedAt,
    @JsonProperty("expired_at")
    String expiredAt
) {
    public static ReservationResponse from(Reservation reservation, int expirationMinutes) {
        LocalDateTime createdAt = reservation.getCreatedAt();
        String expiresAt = createdAt != null
                ? createdAt.plusMinutes(expirationMinutes).toString()
                : null;

        return new ReservationResponse(
                reservation.getId(),
                reservation.getSeat().getId(),
                reservation.getSeat().getSeatNumber(),
                reservation.getStatus().name(),
                createdAt != null ? createdAt.toString() : null,
                expiresAt,
                reservation.getConfirmedAt() != null ? reservation.getConfirmedAt().toString() : null,
                reservation.getExpiredAt() != null ? reservation.getExpiredAt().toString() : null
        );
    }
}
