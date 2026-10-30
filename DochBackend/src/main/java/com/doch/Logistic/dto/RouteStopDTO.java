package com.doch.Logistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteStopDTO {
    @Schema(description = "PICKUP or DROP_OFF")
    private String stopType;
    private String deliveryId;
    private String trackingNumber;
    private String orderReference;
    private String deliveryStatus;
    private Double packageWeightKg;
    private String address;
    private String latitude;
    private String longitude;
    @Schema(description = "The name of the contact person at this stop")
    private String contactName;
    private String contactPhone;
    @Schema(description = "Order of this stop in the journey", example = "1")
    private Integer sequence;
    @Schema(description = "Straight-line distance from the previous stop in km (approximate)")
    private Double legDistanceKm;
}