package com.example.fintech_wallet_engine.controller;

import com.example.fintech_wallet_engine.dto.request.CreateWalletRequest;
import com.example.fintech_wallet_engine.dto.request.WalletStatusRequest;
import com.example.fintech_wallet_engine.dto.request.WalletTransactionRequest;
import com.example.fintech_wallet_engine.dto.response.ApiResponse;
import com.example.fintech_wallet_engine.dto.response.WalletTransactionResponse;
import com.example.fintech_wallet_engine.dto.response.WalletResponse;
import com.example.fintech_wallet_engine.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Wallet created successfully"));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByEmail(email)));
    }

    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> creditWallet(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalletTransactionRequest request) {

        WalletTransactionResponse response = walletService.creditWallet(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response, "Wallet credited successfully"));
    }

    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> debitWallet(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalletTransactionRequest request) {

        WalletTransactionResponse response = walletService.debitWallet(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response, "Wallet debited successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WalletResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody WalletStatusRequest request) {

        WalletResponse response = walletService.updateWalletStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Wallet status updated"));
    }
}