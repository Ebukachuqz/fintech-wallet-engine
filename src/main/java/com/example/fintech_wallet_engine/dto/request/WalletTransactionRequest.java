package com.example.fintech_wallet_engine.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WalletTransactionRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    Long amount,

    String description
) {}