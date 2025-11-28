package com.example.fintech_wallet_engine.model;

public class WalletEnums {

    public enum WalletTransactionType {
        CREDIT,
        DEBIT
    }

    public enum WalletStatus {
        ACTIVE,
        INACTIVE,
        DEACTIVATED
    }

    public enum WalletActionType {
        CREATE_WALLET,
        CREDIT,
        DEBIT,
        ACTIVATE,
        DEACTIVATE
    }
}