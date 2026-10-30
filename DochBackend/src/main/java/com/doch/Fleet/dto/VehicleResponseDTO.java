package com.doch.Fleet.dto;

import com.doch.Fleet.model.Vehicle;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
public class VehicleResponseDTO {

    private String id;
    private String name;
    private String vehicleType;
    private String plateNumber;
    private String category;
    private String categoryLabel;
    private Double maxLoadKg;
    private String make;
    private String model;
    private Integer year;
    private String status;

    // Assigned driver summary (null when unassigned)
    private String assignedDriverProfileId;
    private String assignedDriverName;
    private String assignedDriverPhone;

    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private String notes;
    private Date createdAt;
    private Date updatedAt;

    public static VehicleResponseDTO fromVehicle(Vehicle v) {
        VehicleResponseDTOBuilder builder = VehicleResponseDTO.builder()
                .id(v.getId())
                .name(v.getName())
                .vehicleType(v.getVehicleType())
                .plateNumber(v.getPlateNumber())
                .category(v.getCategory() != null ? v.getCategory().name() : null)
                .categoryLabel(v.getCategory() != null ? v.getCategory().getLabel() : null)
                .maxLoadKg(v.getMaxLoadKg())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .lastMaintenanceDate(v.getLastMaintenanceDate())
                .nextMaintenanceDate(v.getNextMaintenanceDate())
                .notes(v.getNotes())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt());

        if (v.getAssignedDriver() != null) {
            builder.assignedDriverProfileId(v.getAssignedDriver().getId());
            if (v.getAssignedDriver().getUser() != null) {
                builder.assignedDriverName(v.getAssignedDriver().getUser().getFirstName()
                                + " " + v.getAssignedDriver().getUser().getLastName())
                        .assignedDriverPhone(v.getAssignedDriver().getUser().getTel());
            }
        }
        return builder.build();
    }
}
