package com.doch.Logistic.model;

import com.doch.Order.model.Location;
import com.doch.User.model.User;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*; // <-- IMPORT ALL LOMBOK ANNOTATIONS
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "delivery")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Delivery extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** Unique package tracking ID printed on the label / encoded in the QR code (e.g. DCH-4KQ8Z7MX2P). */
    @Column(name = "tracking_number", unique = true, length = 20)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private String deliveryOtp;

    @Builder.Default
    @Column(name = "otp_failed_attempts", nullable = false)
    private Integer otpFailedAttempts = 0;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime estimatedDeliveryTime;

    /** Total package weight in kg (sum of item weights for marketplace legs). */
    @Column(name = "package_weight_kg")
    private Double packageWeightKg;

    /** Largest single item dimension in cm, used by the vehicle decision matrix. */
    @Column(name = "package_max_dimension_cm")
    private Double packageMaxDimensionCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_vehicle_category", length = 20)
    private com.doch.Fleet.model.VehicleCategory requiredVehicleCategory;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private com.doch.Fleet.model.Vehicle vehicle;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "current_hub", length = 120)
    private String currentHub;

    @Version
    private Long version;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "live_location_id")
    private Location liveLocation;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "pod_image_id")
    private DeliveryImage proofOfDeliveryImage;

    @ManyToOne
    @JoinColumn(name = "xender_request_id")
    private DochXenderDeliveryRequest deliveryRequest;

    @ManyToOne
    @JoinColumn(name = "market_request_id")
    private DochMarketDeliveryRequest marketDeliveryRequest;

    /** The user who owns/receives this package (Xender sender or marketplace customer). */
    public User getOwner() {
        if (deliveryRequest != null) return deliveryRequest.getSender();
        if (marketDeliveryRequest != null && marketDeliveryRequest.getOrder() != null) {
            return marketDeliveryRequest.getOrder().getCustomer();
        }
        return null;
    }

    public String getPickupAddress() {
        if (deliveryRequest != null) return deliveryRequest.getPickupLocationAddress();
        if (marketDeliveryRequest != null && marketDeliveryRequest.getSeller() != null) {
            return marketDeliveryRequest.getSeller().getAddress();
        }
        return null;
    }

    public String getDropoffAddress() {
        if (deliveryRequest != null) return deliveryRequest.getReceiverAddress();
        if (marketDeliveryRequest != null && marketDeliveryRequest.getOrder() != null) {
            return marketDeliveryRequest.getOrder().getDeliveryAddress();
        }
        return null;
    }
}