package com.doch.Logistic.repository;

import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.model.DochXenderDeliveryRequest;
import com.doch.Order.model.Order;
import com.doch.User.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, String>, JpaSpecificationExecutor<Delivery> {

    // Find personal (Xender) deliveries
    List<Delivery> findByDeliveryRequest_Sender(User sender);

    // Find Marketplace deliveries by the customer who placed the order
    List<Delivery> findByMarketDeliveryRequest_Order_Customer(User customer);

    Optional<Delivery> findByDeliveryRequest(DochXenderDeliveryRequest xenderRequest);

    /** All packages (one per seller leg) belonging to a marketplace order. */
    List<Delivery> findByMarketDeliveryRequest_Order(Order order);

    List<Delivery> findByMarketDeliveryRequest_Order_Id(String orderId);

    // Find all deliveries for a driver that are not in a final state
    List<Delivery> findByDriverAndStatusNotIn(User driver, List<DeliveryStatus> statuses);

    List<Delivery> findByDriverAndStatus(User driver, DeliveryStatus status);

    Optional<Delivery> findByTrackingNumberIgnoreCase(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    long countByStatus(DeliveryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Delivery d WHERE d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") String id);
}