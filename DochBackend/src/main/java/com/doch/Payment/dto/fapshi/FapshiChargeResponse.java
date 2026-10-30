package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FapshiChargeResponse {

    private String message;

    @JsonProperty("link")
    private String link;

    @JsonProperty("transId")
    private String transactionId;

    @JsonProperty("dateInitiated")
    private String dateInitiated;
}