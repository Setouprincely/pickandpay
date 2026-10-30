package com.doch.common.geocoding;

import com.doch.Order.model.Location;

import java.util.Optional;

public interface GeocodingService {

    /**
     * Converts a human-readable address string into geographic coordinates.
     *
     * @param address The address string (e.g., "Carrefour Nlongkak, Yaounde").
     * @return An Optional containing the Location object if successful, otherwise an empty Optional.
     */
    Optional<Location> geocodeAddress(String address);

}