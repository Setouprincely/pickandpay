package com.doch.Logistic.dto.driver;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Complete profile of a delivery driver including vehicle and performance stats.")
public class DriverProfileDTO {

    @Schema(description = "The unique UUID of the User", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("driverId") // Forces this name in the JSON response
    private String driverId;

    @Schema(description = "Internal Driver Profile ID", example = "dp_789")
    private String profileId;

    @Schema(description = "Full name of the driver", example = "Jean-Paul Biya")
    private String fullName;

    @Schema(description = "Type of vehicle being used", example = "MOTORBIKE")
    private String vehicleType;

    @Schema(description = "Vehicle plate number for identification", example = "LT-123-XY")
    private String plateNumber;

    @Schema(description = "Average customer rating (out of 5.0)", example = "4.8")
    private Double rating;

    @Schema(description = "Total number of lifetime deliveries completed", example = "154")
    private Integer totalDeliveries;

    private String email;
    private String phone;
    private String licenseNumber;

    @Schema(description = "Fleet vehicle currently assigned to the driver (if any)")
    private String vehicleId;
    private String vehicleName;
    private String vehicleCategory;

    @Schema(description = "Number of active (not finished) packages assigned to the driver")
    private Integer activeDeliveries;

    @Schema(description = "Whether the driver is currently online and accepting tasks")
    private boolean isAvailable;
}