package com.linkz.reservation.auth;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String email,
        String fullName,
        List<String> roles
) {}

