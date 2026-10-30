package com.doch.Payment.model;

import com.doch.Logistic.model.DochXenderDeliveryRequest;
import com.doch.Order.model.Order;
import com.doch.User.model.User;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "payment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Payment extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    private String gatewayTransactionId;

    @Column(length = 500)
    private String paymentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xender_request_id", nullable = true)
    private DochXenderDeliveryRequest xenderRequest;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentMethod;

    /** LINK = Fapshi hosted checkout page, DIRECT = mobile money prompt sent to {@link #payerPhone}. */
    @Column(name = "payment_flow", length = 10)
    private String paymentFlow;

    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    /** Last status reported by the provider (CREATED, PENDING, SUCCESSFUL, FAILED, EXPIRED). */
    @Column(name = "provider_status", length = 20)
    private String providerStatus;

    @Column(name = "provider_checked_at")
    private java.time.LocalDateTime providerCheckedAt;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @jakarta.persistence.Version
    private Long version;
}