package com.doch.Payment.model;

import com.doch.User.model.User;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Linked to the Seller's User account

    @Builder.Default
    private Double pendingBalance = 0.0;   // Money from orders currently in delivery

    @Builder.Default
    private Double availableBalance = 0.0; // Money ready to be paid out on the 1st/16th

    @Builder.Default
    private Double totalWithdrawn = 0.0;  // Total money already sent to their Momo/Bank
}