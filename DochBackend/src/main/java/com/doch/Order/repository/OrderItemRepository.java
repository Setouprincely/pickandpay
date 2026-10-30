package com.doch.Order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.doch.Order.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, String>{
    List<OrderItem> findByOrderId(String orderId);
}
