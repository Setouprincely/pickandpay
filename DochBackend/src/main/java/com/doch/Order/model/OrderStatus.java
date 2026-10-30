package com.doch.Order.model;

/**
 * Marketplace order lifecycle.
 * PENDING -> PROCESSING (paid) -> SHIPPED (first package picked up) -> DELIVERED (all packages delivered).
 * PENDING -> PAYMENT_FAILED -> (retry) PENDING / CANCELLED.
 */
public enum OrderStatus {
    /** Created, awaiting payment. Stock is reserved. */
    PENDING,
    /** The last payment attempt failed or expired; the customer may retry or cancel. */
    PAYMENT_FAILED,
    /** Paid; sellers are preparing the packages / dispatch is in progress. */
    PROCESSING,
    /** Legacy terminal state kept for historical records. New orders end in DELIVERED. */
    COMPLETED,
    CANCELLED,
    /** At least one package has been collected by a driver. */
    SHIPPED,
    IN_TRANSIT,
    /** Every package of the order has been delivered with OTP confirmation. */
    DELIVERED,
    /** Packages could not be delivered and were returned to the seller. */
    RETURNED,
    REFUNDED
}
