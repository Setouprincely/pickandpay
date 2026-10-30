package com.doch.common.events;

import com.doch.Logistic.model.DeliveryStatus;

/** Published inside the transaction that changed a package status; consumed after commit. */
public record DeliveryStatusChangedEvent(String deliveryId, DeliveryStatus from, DeliveryStatus to) {
}