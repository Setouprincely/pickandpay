package com.doch.Order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** All figures are computed from the database at request time. Revenue is recognised on delivered orders. */
@Data
@Builder
public class DashboardStats {
    private Double totalRevenue;
    private Double productRevenue;
    private Double deliveryRevenue;
    private Double currentMonthRevenue;
    /** Delivered orders. */
    private Long totalOrders;

    /** Sum of all successful payments (orders and Xender requests). */
    private Double paymentsCollected;
    private Long paidOrders;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> packagesByStatus;
    private Long pendingProductReviews;
    private Long pendingSellerApprovals;
    private Long driversOnline;
    private Long driversTotal;
    private Double averageOrderRating;
    private Double averageDeliveryRating;
    private Long reviewCount;

    private List<MonthlyRating> monthlyRatings;

    @Data
    @Builder
    public static class MonthlyRating {
        private String month;
        private Double averageRating;
        private Double averageDeliveryRating;
        private Long reviews;
    }
}