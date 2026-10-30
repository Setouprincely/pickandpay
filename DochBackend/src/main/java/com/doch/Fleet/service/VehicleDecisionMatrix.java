package com.doch.Fleet.service;

import com.doch.Fleet.model.VehicleCategory;
import com.doch.Logistic.model.TransportType;

/**
 * DOCH fleet decision matrix:
 * <ul>
 *   <li>Bicycle: total weight up to {@code bicycleMaxWeightKg} (default 5 kg) and no item larger than
 *       {@code bicycleMaxDimensionCm} (default 60 cm).</li>
 *   <li>Suzuki Carry / K-Truck: medium loads exceeding bicycle suitability, up to {@code kTruckMaxWeightKg}.</li>
 *   <li>Toyota HiAce: anything heavier.</li>
 * </ul>
 * A FULL_TRUCK booking always requires at least a K-Truck. Unknown weights default to a K-Truck so
 * that a package is never dispatched on a bicycle without verified weight.
 */
public final class VehicleDecisionMatrix {

    private VehicleDecisionMatrix() {
    }

    public static VehicleCategory requiredCategory(Double weightKg,
                                                   Double maxDimensionCm,
                                                   TransportType transportType,
                                                   double bicycleMaxWeightKg,
                                                   double bicycleMaxDimensionCm,
                                                   double kTruckMaxWeightKg) {
        VehicleCategory byWeight;
        if (weightKg == null || weightKg <= 0) {
            byWeight = VehicleCategory.K_TRUCK;
        } else if (weightKg <= bicycleMaxWeightKg) {
            byWeight = VehicleCategory.BICYCLE;
        } else if (weightKg <= kTruckMaxWeightKg) {
            byWeight = VehicleCategory.K_TRUCK;
        } else {
            byWeight = VehicleCategory.VAN;
        }

        VehicleCategory required = byWeight;
        if (required == VehicleCategory.BICYCLE && maxDimensionCm != null && maxDimensionCm > bicycleMaxDimensionCm) {
            required = VehicleCategory.K_TRUCK;
        }
        if (transportType == TransportType.FULL_TRUCK) {
            required = VehicleCategory.larger(required, VehicleCategory.K_TRUCK);
        }
        return required;
    }
}