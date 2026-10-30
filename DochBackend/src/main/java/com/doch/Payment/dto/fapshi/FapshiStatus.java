package com.doch.Payment.dto.fapshi;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FapshiStatus {

    @JsonProperty("CREATED")
    CREATED,

    @JsonProperty("PENDING")
    PENDING,

    @JsonProperty("SUCCESSFUL")
    SUCCESSFUL,

    @JsonProperty("FAILED")
    FAILED,

    @JsonProperty("EXPIRED")
    EXPIRED
}