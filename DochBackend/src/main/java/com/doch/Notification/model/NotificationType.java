package com.doch.Notification.model;

/**
 * Defines the standardized categories for notifications sent throughout the system.
 */
public enum NotificationType {

    // --- User & Authentication Related ---
    WELCOME,
    ACCOUNT_VERIFIED,
    PASSWORD_RESET,

    // --- Order Related (Marketplace) ---
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,

    // --- Logistics Related (DochXender & Driver) ---
    DELIVERY_REQUEST_RECEIVED,
    DELIVERY_DRIVER_ASSIGNED,
    DELIVERY_IN_TRANSIT,
    DELIVERY_PICKED_UP,
    DELIVERY_COMPLETED,
    DELIVERY_FAILED,

    // --- Payment Related ---
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REFUND_PROCESSED,

    // --- General & Promotional ---
    SYSTEM_ANNOUNCEMENT,
    PROMOTION
}