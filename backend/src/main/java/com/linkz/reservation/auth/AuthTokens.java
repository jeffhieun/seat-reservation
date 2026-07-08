package com.linkz.reservation.auth;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String email
) {}
