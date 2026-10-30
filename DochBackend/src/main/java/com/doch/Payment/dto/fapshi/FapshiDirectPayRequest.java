package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FapshiDirectPayRequest {
    private Integer amount;
    private String phone;
    /** "mobile money" or "orange money"; omitted to let Fapshi auto-detect. */
    private String medium;
    private String name;
    private String email;
    private String userId;
    private String externalId;
    private String message;
}