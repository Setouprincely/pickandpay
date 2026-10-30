package com.doch.Logistic.dto;

import com.doch.Logistic.model.TransportType;
import com.doch.common.validation.ValidPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request payload for creating a new DochXender delivery.")
public class DochXenderRequestDTO {

    @NotBlank(message = "Pickup location address is required.")
    @Size(max = 500)
    @Schema(description = "User-friendly address string for the pickup location.", example = "Carrefour Biyem-Assi, Yaounde")
    private String pickupLocationAddress;

    @NotBlank(message = "Receiver address is required.")
    @Size(max = 500)
    @Schema(description = "User-friendly address string for the destination.", example = "Poste Centrale, Yaounde")
    private String receiverAddress;

    @Size(max = 500)
    @Schema(description = "Optional notes for the delivery driver.", example = "Package is fragile, handle with care.")
    private String deliveryNote;

    @Schema(description = "Ignored: the price is always calculated by the backend.")
    private Double price;

    @NotBlank(message = "Payment method is required.")
    @ValidPaymentMethod
    @Schema(description = "The payment method to be used.", example = "MTN_MOMO")
    private String paymentMethod;

    @Schema(description = "Optional mobile money number for a direct payment prompt", example = "670000000")
    @Pattern(regexp = "^$|^6[0-9]{8}$", message = "Phone must be a 9-digit Cameroon mobile number starting with 6.")
    private String paymentPhone;

    @Schema(description = "Declared package weight in kg (drives the vehicle choice)", example = "3.5")
    @Positive(message = "Package weight must be positive.")
    @Max(value = 5000, message = "Package weight cannot exceed 5000 kg.")
    private Double packageWeightKg;

    @Schema(description = "Largest package dimension in cm", example = "40")
    @Positive(message = "Package dimension must be positive.")
    private Double packageMaxDimensionCm;

    @Schema(description = "Type of transport needed", example = "SINGLE_PARCEL")
    private TransportType transportType = TransportType.SINGLE_PARCEL;

    @Schema(description = "Not used: parcel images are uploaded as multipart parts.")
    private List<String> imageIds;
}