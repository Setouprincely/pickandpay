package com.doch.Logistic.dto;

import com.doch.Logistic.model.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to preview Xender delivery price before placing")
public class XenderPricePreviewRequest {

    @NotBlank(message = "Pickup location address is required.")
    @Schema(description = "User-friendly address string for the pickup location.", example = "Carrefour Biyem-Assi, Yaoundé")
    private String pickupLocationAddress;

    @NotBlank(message = "Receiver address is required.")
    @Schema(description = "User-friendly address string for the destination.", example = "Poste Centrale, Yaoundé")
    private String receiverAddress;

    @NotNull(message = "Transport type is required")
    @Schema(description = "Type of transport needed", implementation = TransportType.class)
    private TransportType transportType = TransportType.SINGLE_PARCEL;
}