package com.doch.Payment.model;

import com.doch.common.BaseEntity;
import com.doch.User.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_splits")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentSplit extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Who gets this money (Seller or Platform Admin)
    private User recipient;

    private Double amount;

    private LocalDateTime eligiblePayoutDate; // The 1st or 16th this belongs to

    @Enumerated(EnumType.STRING)
    private SplitType type;

    @Enumerated(EnumType.STRING)
    private SplitStatus status; // PENDING, AVAILABLE, DISBURSED

    public enum SplitType {
        SELLER_EARNING,      // Money from the product sale
        DELIVERY_FEE,        // Money for the logistics department
        PLATFORM_COMMISSION  // The % fee the platform charged the seller
    }

    public enum SplitStatus {
        PENDING,    // Order not yet delivered
        AVAILABLE,  // Delivered, waiting for payout date
        DISBURSED,  // Money sent to seller
        REVERSED    // Order cancelled/returned: earning withdrawn
    }
}