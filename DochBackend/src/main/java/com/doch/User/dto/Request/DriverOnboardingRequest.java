package com.doch.User.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Specialized request for onboarding a Driver with vehicle details")
public class DriverOnboardingRequest extends StaffOnboardingRequest {

    @NotBlank(message = "Vehicle type is required for drivers")
    private String vehicleType;

    @NotBlank(message = "Plate number is required for drivers")
    private String plateNumber;
}