package com.example.fintech_wallet_engine.model;

import com.example.fintech_wallet_engine.exception.WalletEngineException;
import com.example.fintech_wallet_engine.model.WalletEnums.WalletStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets", indexes = {@Index(name = "idx_customer_email", columnList = "email")})
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    @Email(regexp = ".+@.+\\..+", message = "Please provide a valid email address")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @Column(nullable = false)
    @Setter(AccessLevel.PRIVATE)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.PUBLIC)
    private WalletStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Wallet(String email) {
        this.email = email;
        this.balance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    public void credit(Long amount) {
        if (this.status != WalletStatus.ACTIVE) {
            throw new WalletEngineException("Wallet is inactive");
        }
        if (amount <= 0) {
            throw new WalletEngineException("Credit amount must be positive");
        }
        this.balance += amount;
    }

    public void debit(Long amount) {
        if (this.status != WalletStatus.ACTIVE) {
            throw new WalletEngineException("Wallet is inactive");
        }
        if (amount <= 0) {
            throw new WalletEngineException("Debit amount must be positive");
        }
        if (this.balance < amount) {
            throw new WalletEngineException("Insufficient funds");
        }
        this.balance -= amount;
    }
}