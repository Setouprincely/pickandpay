package com.doch.Support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportReportRequestDTO {

    /** e.g. VEHICLE_ISSUE, CUSTOMER_ISSUE, APP_BUG, PAYMENT_ISSUE, OTHER */
    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    /** Optional: the delivery this problem relates to. */
    private String deliveryId;
}
