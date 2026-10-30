package com.doch.common.geocoding.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OSRM /route/v1 response. distance is in meters, duration in seconds.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRouteResponse {

    private String code;
    private List<Route> routes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private double distance;
        private double duration;
    }
}
