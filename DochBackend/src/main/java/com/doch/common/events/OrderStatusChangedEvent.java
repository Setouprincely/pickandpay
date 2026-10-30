package com.doch.common.events;

import com.doch.Order.model.OrderStatus;

public record OrderStatusChangedEvent(String orderId, OrderStatus from, OrderStatus to) {
}