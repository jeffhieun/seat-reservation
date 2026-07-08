package com.linkz.reservation.auth;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
