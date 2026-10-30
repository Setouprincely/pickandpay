package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FapshiWebhookDTO {
    private String transId;         // Fapshi transaction ID
    private String status;          // SUCCESSFUL, FAILED, EXPIRED
    private String externalId;      // This is our internal Payment UUID
    private Integer amount;         // Total amount paid
    private Integer revenue;        // Net amount after Fapshi fees
    private String financialTransId; // Operator (MTN/Orange) ID
}