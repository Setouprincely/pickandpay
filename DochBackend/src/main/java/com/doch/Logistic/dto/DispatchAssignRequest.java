package com.doch.Logistic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DispatchAssignRequest {
    @NotBlank(message = "driverId is required")
    private String driverId;

    @NotBlank(message = "vehicleId is required")
    private String vehicleId;

    /** Optional planned pickup/delivery time (used for rescheduled deliveries). */
    private LocalDateTime scheduledFor;

    @Size(max = 500)
    private String note;
}