package com.linkz.reservation.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentCompletionRequest(
        @NotBlank(message = "result is required")
        String result
) {}
