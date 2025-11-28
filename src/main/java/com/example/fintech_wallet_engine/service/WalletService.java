package com.example.fintech_wallet_engine.service;

import com.example.fintech_wallet_engine.dto.request.CreateWalletRequest;
import com.example.fintech_wallet_engine.dto.request.WalletStatusRequest;
import com.example.fintech_wallet_engine.dto.request.WalletTransactionRequest;
import com.example.fintech_wallet_engine.dto.response.WalletTransactionResponse;
import com.example.fintech_wallet_engine.dto.response.WalletResponse;
import com.example.fintech_wallet_engine.exception.WalletEngineException;
import com.example.fintech_wallet_engine.model.*;
import com.example.fintech_wallet_engine.repository.IdempotencyKeyRepository;
import com.example.fintech_wallet_engine.repository.WalletRepository;
import com.example.fintech_wallet_engine.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        if (walletRepository.existsByEmail(request.email())) {
            throw new WalletEngineException("Wallet already exists");
        }

        Wallet wallet = Wallet.builder()
                .email(request.email())
                .build();

        wallet = walletRepository.save(wallet);

        return mapToWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse updateWalletStatus(UUID walletId, WalletStatusRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletEngineException("Wallet not found"));

        wallet.setStatus(request.status());
        walletRepository.save(wallet);

        return mapToWalletResponse(wallet);
    }

    @Transactional
    public WalletTransactionResponse creditWallet(WalletTransactionRequest request, String idempotencyKey) {
        checkIdempotency(idempotencyKey);

        Wallet wallet = walletRepository.findByEmailForUpdateWithLock(request.email())
                .orElseThrow(() -> new WalletEngineException("Wallet not found"));

        Long balanceBefore = wallet.getBalance();

        wallet.credit(request.amount());
        walletRepository.save(wallet);

        String ref = "TRN-" + UUID.randomUUID();

        WalletTransaction txn = recordTransaction(
                wallet,
                WalletEnums.WalletTransactionType.CREDIT,
                request.amount(),
                balanceBefore,
                wallet.getBalance(),
                ref,
                request.description(),
                idempotencyKey
        );

        saveIdempotencyKey(idempotencyKey);

        return mapToTransactionResponse(txn);
    }

    @Transactional
    public WalletTransactionResponse debitWallet(WalletTransactionRequest request, String idempotencyKey) {
        checkIdempotency(idempotencyKey);

        Wallet wallet = walletRepository.findByEmailForUpdateWithLock(request.email())
                .orElseThrow(() -> new WalletEngineException("Wallet not found"));

        Long balanceBefore = wallet.getBalance();

        wallet.debit(request.amount());
        walletRepository.save(wallet);

        String ref = "TRN-" + UUID.randomUUID();

        WalletTransaction txn = recordTransaction(
                wallet,
                WalletEnums.WalletTransactionType.DEBIT,
                request.amount(),
                balanceBefore,
                wallet.getBalance(),
                ref,
                request.description(),
                idempotencyKey
        );

        saveIdempotencyKey(idempotencyKey);

        return mapToTransactionResponse(txn);
    }

    public WalletResponse getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .map(this::mapToWalletResponse)
                .orElseThrow(() -> new WalletEngineException("Wallet not found"));
    }

    public WalletResponse getWalletByEmail(String email) {
        return walletRepository.findByEmail(email)
                .map(this::mapToWalletResponse)
                .orElseThrow(() -> new WalletEngineException("Wallet not found"));
    }

    private WalletTransaction recordTransaction(Wallet wallet, WalletEnums.WalletTransactionType type, Long amount, Long balanceBefore, Long balanceAfter, String reference, String description, String idempotencyKey) {
        return transactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reference(reference)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private void checkIdempotency(String key) {
        if (key != null && idempotencyKeyRepository.existsById(key)) {
            throw new WalletEngineException("Duplicate request");
        }
    }

    private void saveIdempotencyKey(String key) {
        if (key != null) {
            idempotencyKeyRepository.save(IdempotencyKeyRecord.builder()
                    .key(key)
                    .statusCode(200)
                    .responseBody("SUCCESS")
                    .build());
        }
    }

    private WalletResponse mapToWalletResponse(Wallet w) {
        return new WalletResponse(w.getId(), w.getEmail(), w.getBalance(), w.getStatus(), w.getCreatedAt());
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction t) {
        return new WalletTransactionResponse(
                t.getReference(),
                t.getType(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getDescription(),
                "SUCCESS",
                t.getCreatedAt()
        );
    }
}