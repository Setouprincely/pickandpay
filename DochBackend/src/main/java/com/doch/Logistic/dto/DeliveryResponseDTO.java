package com.doch.Logistic.dto;

import com.doch.Logistic.model.TransportType;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryResponseDTO {
    private String deliveryId;

    @Schema(description = "Unique package tracking ID (printed on the label / QR code)", example = "DCH-4KQ8Z7MX2P")
    private String trackingNumber;

    private String status;

    @Schema(description = "MARKET (marketplace order leg) or XENDER (ad-hoc parcel)")
    private String source;

    private String orderId;
    private String orderReference;
    private String sellerName;
    private String sellerPhone;

    private String driverId;
    private String driverName;
    private String driverPhone;

    private VehicleSummary vehicle;

    @Schema(description = "Vehicle category required by the decision matrix: BICYCLE, K_TRUCK or VAN")
    private String requiredVehicleCategory;
    private Double packageWeightKg;
    private Double packageMaxDimensionCm;

    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime scheduledFor;

    private String failureReason;
    private String currentHub;
    private Integer otpFailedAttempts;
    private Boolean otpLocked;

    private Double price;
    private TransportType transportType;

    @Schema(description = "The person the driver is looking for")
    private String receiverName;
    private String receiverPhone;

    @Schema(description = "ONLY VISIBLE TO THE OWNER (buyer/sender). Share this with the driver upon arrival.")
    private String deliveryOtp;

    private String deliveryNote;
    private String pickupLocationAddress;
    private LocationDTO pickupLocationCoords;
    private String receiverAddress;
    private LocationDTO receiverLocationCoords;
    private PaymentResponseDTO paymentDetails;

    @Schema(description = "List of URLs to view the parcel images.")
    private List<String> parcelImageUrls;

    private String proofOfDeliveryUrl;

    @Schema(description = "Tracking history, oldest first")
    private List<DeliveryEventDTO> events;

    @Data
    @Builder
    public static class VehicleSummary {
        private String id;
        private String name;
        private String plateNumber;
        private String category;
        private String vehicleType;
    }
}