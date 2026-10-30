package com.doch.Logistic.model;

import com.doch.User.model.Seller;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.Order.model.Location;
import com.doch.Order.model.Order;
import com.doch.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "dochMarketDeliveryRequest")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class DochMarketDeliveryRequest extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    private String deliveryNote;

    private Double price;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(cascade = CascadeType.ALL)
    private Location pickupLocation;

    @ManyToOne(cascade = CascadeType.ALL)
    private Location receiverLocation;


    /**
     *  The photo of the sealed package
     *  This starts as NULL when the order is made.
     *  The Seller must upload this to move the status to PENDING_ASSIGNMENT.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "sealed_image_id")
    private DeliveryImage sealedProductImage;

}
