package com.doch.Payment.model;

import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private String id;

    @ManyToOne
    private Wallet wallet;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // CREDIT_PENDING, RELEASE_TO_AVAILABLE, PAYOUT_DEBIT

    private String description; // e.g., "Earnings for Order #123"

    public enum TransactionType {
        CREDIT_PENDING,       // Buyer paid, money is "locked"
        RELEASE_TO_AVAILABLE, // Delivery confirmed, money moved to available
        PAYOUT_DEBIT,        // Admin manually sent the real money
        REVERSE_PENDING      // Order cancelled/returned before delivery: pending earnings withdrawn
    }
}