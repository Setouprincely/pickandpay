package com.doch.Order.service.interfaces;

import com.doch.Order.dto.response.DailyRevenueDTO;
import com.doch.Order.dto.response.DashboardStats;

import java.util.List;

public interface StatsService {

    Double getTotalRevenue();

    Double getProductRevenue();

    Double getDeliveryRevenue();

    Double getCurrentMonthRevenue();

    Long getOrderCount();

    List<DailyRevenueDTO> getDailyRevenue();

    DashboardStats getDashboardStats();
}
