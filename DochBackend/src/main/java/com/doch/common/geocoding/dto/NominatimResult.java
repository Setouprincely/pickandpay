package com.doch.common.geocoding.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * One entry of a Nominatim /search response (the response body is a JSON array).
 * Only the fields we need are mapped; the rest are ignored.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimResult {

    private String lat;
    private String lon;

    @JsonProperty("display_name")
    private String displayName;
}
