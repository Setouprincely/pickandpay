package com.doch.Logistic.dto.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update driver's online status.")
public class UpdateAvailabilityDTO {

    @NotNull(message = "The 'isAvailable' field is required.")
    @Schema(description = "Set to true to go online, false to go offline.", example = "true")
    private Boolean isAvailable;
}