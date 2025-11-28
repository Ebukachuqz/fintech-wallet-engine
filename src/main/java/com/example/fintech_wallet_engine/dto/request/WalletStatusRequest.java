package com.example.fintech_wallet_engine.dto.request;
import com.example.fintech_wallet_engine.model.WalletEnums.WalletStatus;

import jakarta.validation.constraints.NotNull;

public record WalletStatusRequest(
        @NotNull(message = "Status is required")
        WalletStatus status
) {}