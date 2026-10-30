package com.doch.Order.repository;

import com.doch.Order.model.Order;
import com.doch.Order.model.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderReviewRepository extends JpaRepository<OrderReview, String> {

    Optional<OrderReview> findByOrder(Order order);

    boolean existsByOrder(Order order);

    /** Average delivery rating received by a driver across all reviewed orders they delivered. */
    @Query("""
        SELECT AVG(r.deliveryRating) FROM OrderReview r
        WHERE r.deliveryRating IS NOT NULL
          AND EXISTS (SELECT 1 FROM Delivery d
                      WHERE d.marketDeliveryRequest.order = r.order AND d.driver.id = :driverId)
        """)
    Double averageDeliveryRatingForDriver(@Param("driverId") String driverId);
}