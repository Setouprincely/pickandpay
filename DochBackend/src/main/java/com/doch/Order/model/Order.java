package com.doch.Order.model;

import java.util.List;

import com.doch.Logistic.model.TransportType;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.User.model.User;
import com.doch.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "t_orders")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Order extends BaseEntity{

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** Short, human-friendly unique order reference shown to customers and staff (e.g. DO-7K3M9Q2X). */
    @Column(unique = true, length = 20)
    private String reference;

    private double productTotal;   // Sum of all items
    private double deliveryFee;    // Amount calculated via PricingService
    private double totalAmount;    // productTotal + deliveryFee

    @Column(length = 500)
    private String deliveryAddress;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    private TransportType transportType; // Added to store the choice

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private double taxAmount;
    private String discountCode;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @jakarta.persistence.Version
    private Long version;

    @ManyToOne
    private User customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

}
