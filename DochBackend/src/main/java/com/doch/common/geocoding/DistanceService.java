package com.doch.common.geocoding;

import java.util.Optional;

public interface DistanceService {
    /**
     * Returns the road distance in Kilometers and the duration in minutes.
     */
    Optional<DistanceResult> getRoadDistance(String originLat, String originLng, String destLat, String destLng);

    record DistanceResult(double distanceKm, long durationMinutes) {}
}