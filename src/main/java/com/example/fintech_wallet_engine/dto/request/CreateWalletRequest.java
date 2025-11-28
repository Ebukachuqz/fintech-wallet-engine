package com.example.fintech_wallet_engine.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
        @NotBlank(message = "Email is required")
        @Email(regexp = ".+@.+\\..+", message = "Please provide a valid email address")
        String email
) {}