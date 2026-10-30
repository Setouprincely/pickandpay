package com.doch.Logistic.model;

import com.doch.common.exceptions.BusinessException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.doch.Logistic.model.DeliveryStatus.*;

/**
 * Single source of truth for the permitted package status sequence.
 * Every status change in the system goes through {@link #assertTransition}.
 */
public final class DeliveryStateMachine {

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = new EnumMap<>(DeliveryStatus.class);

    static {
        ALLOWED.put(PENDING_PAYMENT, EnumSet.of(PENDING_PREPARATION, PENDING_ASSIGNMENT, CANCELLED));
        ALLOWED.put(PENDING_PREPARATION, EnumSet.of(PENDING_ASSIGNMENT, CANCELLED));
        ALLOWED.put(PENDING_ASSIGNMENT, EnumSet.of(DRIVER_ASSIGNED, CANCELLED));
        // DRIVER_ASSIGNED -> DRIVER_ASSIGNED is a reassignment; -> PENDING_ASSIGNMENT is unassign / package not collected
        ALLOWED.put(DRIVER_ASSIGNED, EnumSet.of(DRIVER_ASSIGNED, PICKED_UP, PENDING_ASSIGNMENT, CANCELLED));
        ALLOWED.put(PICKED_UP, EnumSet.of(IN_TRANSIT, AT_HUB, FAILED));
        // From the hub the package is either assigned an onward/redelivery trip or returned
        ALLOWED.put(AT_HUB, EnumSet.of(DRIVER_ASSIGNED, RETURNED));
        ALLOWED.put(IN_TRANSIT, EnumSet.of(DELIVERED, FAILED, AT_HUB));
        // Failed attempt: driver retries (IN_TRANSIT), brings it to a hub, or dispatch returns it
        ALLOWED.put(FAILED, EnumSet.of(IN_TRANSIT, AT_HUB, RETURNED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(RETURNED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(DeliveryStatus.class));
    }

    private DeliveryStateMachine() {
    }

    public static boolean canTransition(DeliveryStatus from, DeliveryStatus to) {
        if (from == null || to == null) return false;
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransition(DeliveryStatus from, DeliveryStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException("Invalid status change: a package in status " + from
                    + " cannot move to " + to + ".");
        }
    }

    public static boolean isTerminal(DeliveryStatus status) {
        return status == DELIVERED || status == RETURNED || status == CANCELLED;
    }

    /** Statuses in which the package is physically with DOCH (driver or hub). */
    public static boolean isInDochCustody(DeliveryStatus status) {
        return status == PICKED_UP || status == AT_HUB || status == IN_TRANSIT || status == FAILED;
    }

    public static Set<DeliveryStatus> allowedNext(DeliveryStatus from) {
        return EnumSet.copyOf(ALLOWED.getOrDefault(from, EnumSet.noneOf(DeliveryStatus.class)));
    }
}
