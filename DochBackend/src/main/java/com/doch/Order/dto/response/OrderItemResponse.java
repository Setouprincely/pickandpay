package com.doch.Order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private String productId;
    private String productName;
    private String productImageUrl;
    private String sellerId;
    private String sellerName;
    private int quantity;
    private double unite_price;
    private double total_price;
    private Double weightKg;
}