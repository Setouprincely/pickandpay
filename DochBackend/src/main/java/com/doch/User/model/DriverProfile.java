package com.doch.User.model;

import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfile extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String vehicleType;
    private String plateNumber;
    private String licenseNumber;

    private String lastLatitude;
    private String lastLongitude;
    private LocalDateTime lastLocationUpdate;

    /** Average customer delivery rating; null until the first review. */
    private Double rating;

    @Builder.Default
    private Integer totalDeliveries = 0;
}