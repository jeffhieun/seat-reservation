package com.linkz.reservation.auth;

import jakarta.validation.constraints.*;
public record LoginRequest(
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    String password
) {}
 
