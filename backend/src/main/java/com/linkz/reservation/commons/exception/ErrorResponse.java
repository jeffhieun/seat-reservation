package com.linkz.reservation.commons.exception;

public record ErrorResponse(
    String error,
    String message
) {}

