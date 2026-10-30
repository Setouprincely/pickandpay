package com.doch.common.events;

/** Published when a marketplace order's payment is confirmed by the provider. */
public record OrderPaidEvent(String orderId, String paymentId) {
}