package com.example.fintech_wallet_engine.service;

import com.example.fintech_wallet_engine.dto.request.CreateWalletRequest;
import com.example.fintech_wallet_engine.dto.request.WalletTransactionRequest;
import com.example.fintech_wallet_engine.dto.response.WalletResponse;
import com.example.fintech_wallet_engine.dto.response.WalletTransactionResponse;
import com.example.fintech_wallet_engine.exception.WalletEngineException;
import com.example.fintech_wallet_engine.model.*;
import com.example.fintech_wallet_engine.repository.IdempotencyKeyRepository;
import com.example.fintech_wallet_engine.repository.WalletRepository;
import com.example.fintech_wallet_engine.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet activeWallet;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        activeWallet = Wallet.builder()
                .email(TEST_EMAIL)
                .build();
        setField(activeWallet, "id", UUID.randomUUID());
        setField(activeWallet, "status", WalletEnums.WalletStatus.ACTIVE);
    }

    @Test
    @DisplayName("Create Wallet - Success")
    void createWallet_Success() {
        CreateWalletRequest request = new CreateWalletRequest(TEST_EMAIL);

        when(walletRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            setField(w, "id", UUID.randomUUID());
            return w;
        });

        WalletResponse response = walletService.createWallet(request);

        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.email());
        assertEquals(0L, response.balance()); // Should start at 0
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Create Wallet - Failure (Already Exists)")
    void createWallet_AlreadyExists() {
        CreateWalletRequest request = new CreateWalletRequest(TEST_EMAIL);
        when(walletRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThrows(WalletEngineException.class, () -> walletService.createWallet(request));
        verify(walletRepository, never()).save(any());
    }


    @Test
    @DisplayName("Credit Wallet - Success")
    void creditWallet_Success() {
        // Arrange
        String idempotencyKey = "key-123";
        Long creditAmount = 5000L;
        WalletTransactionRequest request = new WalletTransactionRequest(TEST_EMAIL, creditAmount, "Salary");

        when(idempotencyKeyRepository.existsById(idempotencyKey)).thenReturn(false);
        when(walletRepository.findByEmailForUpdateWithLock(TEST_EMAIL)).thenReturn(Optional.of(activeWallet));

        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));

        WalletTransactionResponse response = walletService.creditWallet(request, idempotencyKey);

        assertEquals(5000L, activeWallet.getBalance()); // 0 + 5000
        assertEquals(5000L, response.balanceAfter());
        assertEquals(WalletEnums.WalletTransactionType.CREDIT, response.type());

        verify(walletRepository).save(activeWallet);
        verify(idempotencyKeyRepository).save(any(IdempotencyKeyRecord.class));
    }

    @Test
    @DisplayName("Credit Wallet - Failure (Idempotency Key Used)")
    void creditWallet_DuplicateRequest() {
        String idempotencyKey = "key-duplicate";
        WalletTransactionRequest request = new WalletTransactionRequest(TEST_EMAIL, 100L, "Ref");

        when(idempotencyKeyRepository.existsById(idempotencyKey)).thenReturn(true);

        assertThrows(WalletEngineException.class, () -> walletService.creditWallet(request, idempotencyKey));
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debit Wallet - Success")
    void debitWallet_Success() {
        // Arrange: Give wallet 5000 first
        setField(activeWallet, "balance", 5000L);

        String idempotencyKey = "key-debit-1";
        Long debitAmount = 2000L;
        WalletTransactionRequest request = new WalletTransactionRequest(TEST_EMAIL, debitAmount, "Purchase");

        when(idempotencyKeyRepository.existsById(idempotencyKey)).thenReturn(false);
        when(walletRepository.findByEmailForUpdateWithLock(TEST_EMAIL)).thenReturn(Optional.of(activeWallet));
        when(transactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));

        WalletTransactionResponse response = walletService.debitWallet(request, idempotencyKey);

        assertEquals(3000L, activeWallet.getBalance()); // 5000 - 2000
        assertEquals(3000L, response.balanceAfter());
        assertEquals(WalletEnums.WalletTransactionType.DEBIT, response.type());
    }

    @Test
    @DisplayName("Debit Wallet - Insufficient Funds")
    void debitWallet_InsufficientFunds() {
        setField(activeWallet, "balance", 100L);

        WalletTransactionRequest request = new WalletTransactionRequest(TEST_EMAIL, 500L, "Purchase");

        when(walletRepository.findByEmailForUpdateWithLock(TEST_EMAIL)).thenReturn(Optional.of(activeWallet));

        assertThrows(WalletEngineException.class, () -> walletService.debitWallet(request, "key-fail"));

        assertEquals(100L, activeWallet.getBalance());
        verify(walletRepository, never()).save(activeWallet);
    }

    @Test
    @DisplayName("Debit Wallet - Wallet Inactive")
    void debitWallet_Inactive() {
        setField(activeWallet, "status", WalletEnums.WalletStatus.INACTIVE);
        setField(activeWallet, "balance", 1000L);
        WalletTransactionRequest request = new WalletTransactionRequest(TEST_EMAIL, 100L, "Purchase");

        when(walletRepository.findByEmailForUpdateWithLock(TEST_EMAIL)).thenReturn(Optional.of(activeWallet));

        assertThrows(WalletEngineException.class, () -> walletService.debitWallet(request, "key-inactive"));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}