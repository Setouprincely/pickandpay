package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FapshiChargeRequest {
    private Integer amount;
    private String email;

    @JsonProperty("redirectUrl")
    private String redirectUrl;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("externalId")
    private String externalId;

    private String message;
}