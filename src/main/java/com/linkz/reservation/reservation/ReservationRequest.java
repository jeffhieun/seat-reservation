package com.linkz.reservation.reservation;

import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
    @NotNull(message = "Seat ID is required")
    Long seatId
) {}

