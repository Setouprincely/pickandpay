package com.doch.Fleet.model;

/**
 * DOCH fleet vehicle categories, ordered from smallest to largest capacity.
 * A delivery requiring a category may be served by that category or any larger one.
 */
public enum VehicleCategory {
    BICYCLE("Bicycle", 1),
    K_TRUCK("Suzuki Carry / K-Truck", 2),
    VAN("Toyota HiAce", 3);

    private final String label;
    private final int capacityRank;

    VehicleCategory(String label, int capacityRank) {
        this.label = label;
        this.capacityRank = capacityRank;
    }

    public String getLabel() {
        return label;
    }

    public boolean canCarry(VehicleCategory required) {
        return required == null || this.capacityRank >= required.capacityRank;
    }

    public static VehicleCategory larger(VehicleCategory a, VehicleCategory b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.capacityRank >= b.capacityRank ? a : b;
    }

    /**
     * Maps a vehicle type entered for a driver (enum name or free text such as "truck", "moto", "HiAce")
     * to a fleet category. Two-wheelers count as BICYCLE (smallest loads); unknown types default to K_TRUCK.
     */
    public static VehicleCategory fromVehicleType(String type) {
        if (type == null || type.isBlank()) return K_TRUCK;
        String t = type.trim().toLowerCase();
        for (VehicleCategory c : values()) {
            if (c.name().equalsIgnoreCase(t)) return c;
        }
        if (t.matches(".*(bicycle|bike|velo|vélo|cycle|moto|scooter).*")) return BICYCLE;
        if (t.matches(".*(van|hiace|bus|lorry|heavy|full).*")) return VAN;
        return K_TRUCK;
    }
}
