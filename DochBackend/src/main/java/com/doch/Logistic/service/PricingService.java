package com.doch.Logistic.service;

import com.doch.Logistic.model.TransportType;

public interface PricingService {
    /**
     * Calculates the delivery price based on distance.
     * @param distanceKm The road distance in Kilometers
     * @param type The Transport Type
     * @return The calculated price in XAF, rounded to the nearest 50.
     */
    double calculateDeliveryPrice(double distanceKm, TransportType type);
}