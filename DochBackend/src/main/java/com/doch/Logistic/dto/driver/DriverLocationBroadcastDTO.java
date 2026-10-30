package com.doch.Logistic.dto.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data payload broadcasted via WebSocket for real-time tracking on maps.")
public class DriverLocationBroadcastDTO {

    @Schema(description = "The UUID of the Driver (User ID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String driverId;

    @Schema(description = "Full name of the driver for map display", example = "Jean-Paul Biya")
    private String driverName;

    @Schema(description = "Current latitude coordinate", example = "3.8480")
    private String latitude;

    @Schema(description = "Current longitude coordinate", example = "11.5021")
    private String longitude;

    @Schema(description = "ISO-8601 formatted timestamp of the update", example = "2026-01-28T15:30:00")
    private String timestamp;
}