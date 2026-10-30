package com.doch.Logistic.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import com.doch.Order.model.Location;
import com.doch.User.model.User;
import com.doch.common.BaseEntity;

import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "dochXenderDeliveryRequest")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class DochXenderDeliveryRequest extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    private String deliveryNote;
    private String deliveryType;
    private String status;
    private String pickupLocationAddress;
    private String receiverAddress;

    private Double price;
    @Enumerated(EnumType.STRING)
    private TransportType transportType;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "pickup_location_id")
    private Location pickupLocation;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "receiver_location_id")
    private Location receiverLocation;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "xender_request_id")
    private List<DeliveryImage> parcelImages;

    @ManyToOne
    private User sender;
}
