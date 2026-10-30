package com.doch.Logistic.model;

/**
 * Lifecycle stages of a physical delivery (one package / one pickup leg).
 * Allowed transitions are enforced by {@link DeliveryStateMachine}.
 */
public enum DeliveryStatus {
    /** The delivery request has been created but is awaiting successful payment. */
    PENDING_PAYMENT,
    /** Payment is successful and the seller is packaging the items. */
    PENDING_PREPARATION,
    /** Ready (sealed photo uploaded / Xender paid) and waiting for dispatch to assign a driver and vehicle. */
    PENDING_ASSIGNMENT,
    /** A driver and vehicle have been assigned. */
    DRIVER_ASSIGNED,
    /** The driver scanned the package at the pickup location. */
    PICKED_UP,
    /** The package was scanned in at a DOCH hub (inspection / sorting / onward transfer). */
    AT_HUB,
    /** The driver is on the way to the recipient. */
    IN_TRANSIT,
    /** Delivered and confirmed with the recipient OTP and proof-of-delivery photo. */
    DELIVERED,
    /** Delivery attempt failed (recipient unavailable, damaged, mismatched details...). Recoverable. */
    FAILED,
    /** The package was returned to the seller / sender. Terminal. */
    RETURNED,
    /** Cancelled before collection. Terminal. */
    CANCELLED
}