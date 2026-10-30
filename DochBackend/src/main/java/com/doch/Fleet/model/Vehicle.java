package com.doch.Fleet.model;

import com.doch.User.model.DriverProfile;
import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "fleet_vehicle")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Vehicle extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** Human friendly name, e.g. "Truck 01 - Hilux" */
    private String name;

    /** Free-text model/type description, e.g. "Suzuki Carry". */
    private String vehicleType;

    /** Fleet category used by the dispatch decision matrix. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VehicleCategory category;

    /** Maximum payload in kg (informational, shown to dispatchers). */
    @Column(name = "max_load_kg")
    private Double maxLoadKg;

    @Column(unique = true)
    private String plateNumber;

    private String make;
    private String model;
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;

    /** The driver currently assigned to this vehicle (nullable). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_profile_id")
    private DriverProfile assignedDriver;

    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;

    @Column(length = 1000)
    private String notes;
}
