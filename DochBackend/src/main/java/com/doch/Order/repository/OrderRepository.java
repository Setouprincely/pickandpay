package com.doch.Order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.doch.Order.dto.response.DailyRevenueDTO;
import com.doch.Order.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.doch.Order.model.Order;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByReferenceIgnoreCase(String reference);

    boolean existsByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.createdAt < :before")
    List<Order> findByStatusInCreatedBefore(@Param("statuses") List<OrderStatus> statuses,
                                            @Param("before") java.util.Date before);

    @Query("""
    SELECT DISTINCT o
    FROM Order o
    JOIN o.items i
    JOIN i.product p
    WHERE p.seller.id = :sellerId
    """)
    List<Order> findOrdersBySeller(@Param("sellerId") String sellerId);

    List<Order> findByItemsProductId(String productId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    Double getTotalRevenue(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.productTotal) FROM Order o WHERE o.status = :status")
    Double getProductRevenue(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.deliveryFee) FROM Order o WHERE o.status = :status")
    Double getDeliveryRevenue(@Param("status") OrderStatus status);

    @Query("""
        SELECT SUM(o.totalAmount)
        FROM Order o
        WHERE o.status = :status
        AND o.createdAt >= :start
        AND o.createdAt < :end
    """)
    Double getRevenueBetweenDates(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Long countByStatus(OrderStatus status);

    @Query("""
    SELECT new com.doch.Order.dto.response.DailyRevenueDTO(
        CAST(FUNCTION('DATE', o.createdAt) AS LocalDate),
        SUM(o.totalAmount)
        )
        FROM Order o
        WHERE o.status = :status
        GROUP BY FUNCTION('DATE', o.createdAt)
        ORDER BY FUNCTION('DATE', o.createdAt)
    """)
    List<DailyRevenueDTO> getDailyRevenue(@Param("status") OrderStatus status);

}
