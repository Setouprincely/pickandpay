package com.doch.workflow;

import com.doch.Fleet.model.VehicleCategory;
import com.doch.Fleet.service.VehicleDecisionMatrix;
import com.doch.Logistic.model.TransportType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleDecisionMatrixTest {

    private static VehicleCategory decide(Double kg, Double maxDimCm, TransportType type) {
        return VehicleDecisionMatrix.requiredCategory(kg, maxDimCm, type, 5.0, 60.0, 350.0);
    }

    @Test
    void bicycleForLightSmallParcels() {
        assertEquals(VehicleCategory.BICYCLE, decide(0.5, 30.0, TransportType.SINGLE_PARCEL));
        assertEquals(VehicleCategory.BICYCLE, decide(5.0, null, TransportType.SINGLE_PARCEL));
    }

    @Test
    void kTruckWhenTooHeavyOrTooLargeForBicycle() {
        assertEquals(VehicleCategory.K_TRUCK, decide(5.01, 20.0, TransportType.SINGLE_PARCEL));
        assertEquals(VehicleCategory.K_TRUCK, decide(2.0, 80.0, TransportType.SINGLE_PARCEL));
        assertEquals(VehicleCategory.K_TRUCK, decide(350.0, null, TransportType.SINGLE_PARCEL));
    }

    @Test
    void vanForHeavyLoads() {
        assertEquals(VehicleCategory.VAN, decide(350.5, null, TransportType.SINGLE_PARCEL));
        assertEquals(VehicleCategory.VAN, decide(900.0, 200.0, TransportType.FULL_TRUCK));
    }

    @Test
    void unknownWeightNeverGoesOnABicycle() {
        assertEquals(VehicleCategory.K_TRUCK, decide(null, null, TransportType.SINGLE_PARCEL));
    }

    @Test
    void fullTruckBookingNeedsAtLeastAKTruck() {
        assertEquals(VehicleCategory.K_TRUCK, decide(1.0, 10.0, TransportType.FULL_TRUCK));
    }

    @Test
    void largerVehiclesCanCarrySmallerCategories() {
        assertTrue(VehicleCategory.VAN.canCarry(VehicleCategory.BICYCLE));
        assertTrue(VehicleCategory.K_TRUCK.canCarry(VehicleCategory.K_TRUCK));
        assertFalse(VehicleCategory.BICYCLE.canCarry(VehicleCategory.K_TRUCK));
        assertFalse(VehicleCategory.K_TRUCK.canCarry(VehicleCategory.VAN));
    }
}