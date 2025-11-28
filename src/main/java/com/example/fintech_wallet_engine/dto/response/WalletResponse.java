package com.example.fintech_wallet_engine.dto.response;

import com.example.fintech_wallet_engine.model.WalletEnums.WalletStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String email,
        Long balance,
        WalletStatus status,
        LocalDateTime createdAt
) {}