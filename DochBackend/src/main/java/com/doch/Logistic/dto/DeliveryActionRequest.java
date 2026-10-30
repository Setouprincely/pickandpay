package com.doch.Logistic.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** Generic payload for driver / dispatcher package actions. Fields used depend on the action. */
@Data
public class DeliveryActionRequest {
    /** Tracking code scanned from the QR label or typed by the driver/hub operator. */
    @Size(max = 40)
    private String trackingCode;

    /** Reason code, e.g. RECIPIENT_UNAVAILABLE, DAMAGED, DETAILS_MISMATCH, PACKAGE_NOT_READY, DRIVER_UNAVAILABLE. */
    @Size(max = 60)
    private String reason;

    @Size(max = 500)
    private String note;

    @Size(max = 120)
    private String hubName;

    private LocalDateTime scheduledFor;

    private String latitude;
    private String longitude;
}