package com.doch.Logistic.dto;

import com.doch.Logistic.model.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Price breakdown for Xender delivery before placement")
public class XenderPricePreviewResponse {

    @Schema(description = "Calculated delivery price in XAF", example = "1200.0")
    private Double price;

    @Schema(description = "Distance in kilometers", example = "8.5")
    private Double distanceKm;

    @Schema(description = "Estimated duration in minutes", example = "25")
    private Long estimatedDurationMinutes;

    @Schema(description = "Type of transport", example = "SINGLE_PARCEL")
    private TransportType transportType;

    @Schema(description = "Pickup address confirmed", example = "Carrefour Biyem-Assi, Yaoundé")
    private String pickupLocationAddress;

    @Schema(description = "Pickup coordinates (latitude)", example = "3.8480")
    private String pickupLatitude;

    @Schema(description = "Pickup coordinates (longitude)", example = "11.5021")
    private String pickupLongitude;

    @Schema(description = "Destination address confirmed", example = "Poste Centrale, Yaoundé")
    private String receiverAddress;

    @Schema(description = "Destination coordinates (latitude)", example = "3.8480")
    private String receiverLatitude;

    @Schema(description = "Destination coordinates (longitude)", example = "11.5021")
    private String receiverLongitude;

    @Schema(description = "Human-readable price breakdown", example = "Base fee: 500 XAF + Distance: 700 XAF = Total: 1200 XAF")
    private String priceBreakdown;
}