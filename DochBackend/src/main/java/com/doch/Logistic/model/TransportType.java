package com.doch.Logistic.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categories of transportation available in the system")
public enum TransportType {
    @Schema(description = "Standard single package delivery")
    SINGLE_PARCEL,

    @Schema(description = "Bulk transportation using a full vehicle/truck")
    FULL_TRUCK
}