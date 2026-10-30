package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FapshiDirectPayResponse {
    private String message;
    private String transId;
    private String dateInitiated;
}