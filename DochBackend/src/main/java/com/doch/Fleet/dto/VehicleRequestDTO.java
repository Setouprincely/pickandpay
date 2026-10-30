package com.doch.Fleet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VehicleRequestDTO {

    @NotBlank(message = "Vehicle name is required")
    private String name;

    @NotBlank(message = "Vehicle model/type is required (e.g. Suzuki Carry)")
    private String vehicleType;

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    /** Fleet category: BICYCLE, K_TRUCK (Suzuki Carry) or VAN (Toyota HiAce). */
    private String category;

    @jakarta.validation.constraints.Positive(message = "Max load must be positive")
    private Double maxLoadKg;

    private String make;
    private String model;
    private Integer year;

    /** Optional: ACTIVE, IN_MAINTENANCE, INACTIVE, RETIRED. Defaults to ACTIVE. */
    private String status;

    /** Optional: DriverProfile id to assign this vehicle to on creation. */
    private String assignedDriverProfileId;

    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;

    private String notes;
}
