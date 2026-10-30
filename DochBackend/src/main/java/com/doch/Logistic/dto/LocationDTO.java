package com.doch.Logistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for geographic coordinates.")
public class LocationDTO {

    @NotBlank(message = "Latitude is required for the location.")
    @Schema(description = "The latitude coordinate", example = "4.0511")
    private String latitude;

    @NotBlank(message = "Longitude is required for the location.")
    @Schema(description = "The longitude coordinate", example = "9.7679")
    private String longitude;
}