package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FapshiStatusResponse {
    @JsonProperty("transId")
    private String transactionId;
    private FapshiStatus status; // CREATED, PENDING, SUCCESSFUL, FAILED, EXPIRED
    private Integer amount;
    private Integer revenue;
    private String externalId;
    private String userId;
    private String medium;
    private String financialTransId;
}