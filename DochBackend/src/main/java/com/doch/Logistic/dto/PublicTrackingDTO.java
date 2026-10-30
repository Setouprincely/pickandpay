package com.doch.Logistic.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Customer-safe tracking view: no names, phone numbers, addresses or OTPs. */
@Data
@Builder
public class PublicTrackingDTO {
    private String trackingNumber;
    private String status;
    private String vehicleCategory;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime lastUpdated;
    private List<DeliveryEventDTO> events;
}