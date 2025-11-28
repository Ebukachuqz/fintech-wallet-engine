package com.example.fintech_wallet_engine.dto.response;

import com.example.fintech_wallet_engine.model.WalletEnums.WalletTransactionType;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        String reference,
        WalletTransactionType type,
        Long amount,
        Long balanceAfter,
        String description,
        String status,
        LocalDateTime timestamp
) {}