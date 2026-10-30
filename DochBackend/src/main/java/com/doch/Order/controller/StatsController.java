package com.doch.Order.controller;

import com.doch.Order.dto.response.DailyRevenueDTO;
import com.doch.Order.dto.response.DashboardStats;
import com.doch.Order.service.interfaces.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "APIs for system analytics and financial statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    // 🔥 DASHBOARD (main endpoint)
    @Operation(summary = "Get dashboard statistics", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns global stats including revenue, orders, and current month performance")
    @PreAuthorize("hasAuthority('VIEW_STATS')")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboard() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }

    // 💰 TOTAL REVENUE
    @Operation(summary = "Get total revenue", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns total revenue (products + delivery) for all completed orders")
    @PreAuthorize("hasAuthority('VIEW_FINANCIAL_STATS')")
    @GetMapping("/revenue")
    public ResponseEntity<Double> getTotalRevenue() {
        return ResponseEntity.ok(statsService.getTotalRevenue());
    }

    // 📦 PRODUCT REVENUE
    @Operation(summary = "Get product revenue", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns total revenue generated from products only")
    @PreAuthorize("hasAuthority('VIEW_FINANCIAL_STATS')")
    @GetMapping("/revenue/products")
    public ResponseEntity<Double> getProductRevenue() {
        return ResponseEntity.ok(statsService.getProductRevenue());
    }

    // 🚚 DELIVERY REVENUE
    @Operation(summary = "Get delivery revenue", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns total revenue generated from delivery fees")
    @PreAuthorize("hasAuthority('VIEW_FINANCIAL_STATS')")
    @GetMapping("/revenue/delivery")
    public ResponseEntity<Double> getDeliveryRevenue() {
        return ResponseEntity.ok(statsService.getDeliveryRevenue());
    }

    // 📅 CURRENT MONTH REVENUE
    @Operation(summary = "Get current month revenue", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns total revenue for the current month")
    @PreAuthorize("hasAuthority('VIEW_FINANCIAL_STATS')")
    @GetMapping("/revenue/current-month")
    public ResponseEntity<Double> getCurrentMonthRevenue() {
        return ResponseEntity.ok(statsService.getCurrentMonthRevenue());
    }

    // 📊 ORDER COUNT
    @Operation(summary = "Get total delivered orders", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns number of completed/delivered orders")
    @PreAuthorize("hasAuthority('VIEW_ORDER_STATS')")
    @GetMapping("/orders/count")
    public ResponseEntity<Long> getOrderCount() {
        return ResponseEntity.ok(statsService.getOrderCount());
    }

    // 📈 DAILY REVENUE
    @Operation(summary = "Get daily revenue", security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns revenue grouped by day")
    @PreAuthorize("hasAuthority('VIEW_STATS')")
    @GetMapping("/revenue/daily")
    public ResponseEntity<List<DailyRevenueDTO>> getDailyRevenue() {
        return ResponseEntity.ok(statsService.getDailyRevenue());
    }
}