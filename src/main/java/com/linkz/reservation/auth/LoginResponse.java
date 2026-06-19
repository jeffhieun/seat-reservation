package com.linkz.reservation.auth;

public record LoginResponse(
    String token,
    String email
) {}

