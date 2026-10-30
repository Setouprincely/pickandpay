package com.doch.Order.service;

import com.doch.Order.dto.response.DailyRevenueDTO;
import com.doch.Order.dto.response.DashboardStats;
import com.doch.Order.model.OrderStatus;
import com.doch.Order.repository.OrderRepository;
import com.doch.Order.service.interfaces.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final OrderRepository orderRepository;
    private final jakarta.persistence.EntityManager em;

    private static final OrderStatus COMPLETED_STATUS = OrderStatus.DELIVERED;

    @Override
    public Double getTotalRevenue() {
        return defaultValue(orderRepository.getTotalRevenue(COMPLETED_STATUS));
    }

    @Override
    public Double getProductRevenue() {
        return defaultValue(orderRepository.getProductRevenue(COMPLETED_STATUS));
    }

    @Override
    public Double getDeliveryRevenue() {
        return defaultValue(orderRepository.getDeliveryRevenue(COMPLETED_STATUS));
    }

    @Override
    public Double getCurrentMonthRevenue() {
        LocalDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();

        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        return defaultValue(
                orderRepository.getRevenueBetweenDates(COMPLETED_STATUS, startOfMonth, endOfMonth)
        );
    }

    @Override
    public Long getOrderCount() {
        return orderRepository.countByStatus(COMPLETED_STATUS);
    }

    @Override
    public List<DailyRevenueDTO> getDailyRevenue() {

        return orderRepository.getDailyRevenue(COMPLETED_STATUS);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        java.util.Map<String, Long> ordersByStatus = new java.util.LinkedHashMap<>();
        for (Object[] r : em.createQuery("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status", Object[].class).getResultList()) {
            ordersByStatus.put(String.valueOf(r[0]), (Long) r[1]);
        }
        java.util.Map<String, Long> packagesByStatus = new java.util.LinkedHashMap<>();
        for (Object[] r : em.createQuery("SELECT d.status, COUNT(d) FROM Delivery d GROUP BY d.status", Object[].class).getResultList()) {
            packagesByStatus.put(String.valueOf(r[0]), (Long) r[1]);
        }
        Double collected = em.createQuery("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = com.doch.Payment.model.PaymentStatus.COMPLETED", Double.class).getSingleResult();
        Long paidOrders = em.createQuery("SELECT COUNT(o) FROM Order o WHERE o.paidAt IS NOT NULL", Long.class).getSingleResult();
        Long pendingProducts = em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.publicationStatus = com.doch.Catalog.model.ProductPublicationStatus.PENDING_REVIEW", Long.class).getSingleResult();
        Long pendingSellers = em.createQuery("SELECT COUNT(s) FROM Seller s WHERE s.isVerified = false AND s.verificationNote IS NULL", Long.class).getSingleResult();
        Long driversTotal = em.createQuery("SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.extraRoles x WHERE (u.userRole.name = 'ROLE_DELIVERY' OR x.name = 'ROLE_DELIVERY') AND u.enabled = true", Long.class).getSingleResult();
        Long driversOnline = em.createQuery("SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.extraRoles x WHERE (u.userRole.name = 'ROLE_DELIVERY' OR x.name = 'ROLE_DELIVERY') AND u.enabled = true AND u.isAvailable = true", Long.class).getSingleResult();
        Object[] ratings = em.createQuery("SELECT AVG(r.rating), AVG(r.deliveryRating), COUNT(r) FROM OrderReview r", Object[].class).getSingleResult();

        java.util.List<DashboardStats.MonthlyRating> monthly = new java.util.ArrayList<>();
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> rows = em.createNativeQuery("""
                SELECT to_char(created_at, 'YYYY-MM') AS m, AVG(rating), AVG(delivery_rating), COUNT(*)
                FROM order_review WHERE created_at >= now() - interval '12 months'
                GROUP BY m ORDER BY m""").getResultList();
        for (Object[] r : rows) {
            monthly.add(DashboardStats.MonthlyRating.builder()
                    .month((String) r[0])
                    .averageRating(round(r[1]))
                    .averageDeliveryRating(round(r[2]))
                    .reviews(((Number) r[3]).longValue())
                    .build());
        }

        return DashboardStats.builder()
                .totalRevenue(getTotalRevenue())
                .productRevenue(getProductRevenue())
                .deliveryRevenue(getDeliveryRevenue())
                .currentMonthRevenue(getCurrentMonthRevenue())
                .totalOrders(getOrderCount())
                .paymentsCollected(collected)
                .paidOrders(paidOrders)
                .ordersByStatus(ordersByStatus)
                .packagesByStatus(packagesByStatus)
                .pendingProductReviews(pendingProducts)
                .pendingSellerApprovals(pendingSellers)
                .driversTotal(driversTotal)
                .driversOnline(driversOnline)
                .averageOrderRating(round(ratings[0]))
                .averageDeliveryRating(round(ratings[1]))
                .reviewCount((Long) ratings[2])
                .monthlyRatings(monthly)
                .build();
    }

    private static Double round(Object value) {
        return value == null ? null : Math.round(((Number) value).doubleValue() * 10.0) / 10.0;
    }

    private Double defaultValue(Double value) {
        return value != null ? value : 0.0;
    }
}
