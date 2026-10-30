package com.doch.Payment.repository;

import com.doch.Logistic.model.DochXenderDeliveryRequest;
import com.doch.Order.model.Order;
import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findAllByOrderOrderByCreatedAtDesc(Order order);

    List<Payment> findAllByXenderRequestOrderByCreatedAtDesc(DochXenderDeliveryRequest xenderRequest);

    /**
     * An order may have several payment attempts (failed, then retried).
     * Returns the successful one when it exists, otherwise the most recent attempt.
     */
    default Optional<Payment> findByOrder(Order order) {
        return pickRelevant(findAllByOrderOrderByCreatedAtDesc(order));
    }

    default Optional<Payment> findByXenderRequest(DochXenderDeliveryRequest xenderRequest) {
        return pickRelevant(findAllByXenderRequestOrderByCreatedAtDesc(xenderRequest));
    }

    private static Optional<Payment> pickRelevant(List<Payment> attempts) {
        return attempts.stream().filter(p -> p.getStatus() == PaymentStatus.COMPLETED).findFirst()
                .or(() -> attempts.stream().findFirst());
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") String id);

    Optional<Payment> findFirstByGatewayTransactionId(String gatewayTransactionId);

    @Query("SELECT p FROM Payment p WHERE p.status = com.doch.Payment.model.PaymentStatus.PENDING "
            + "AND p.gatewayTransactionId IS NOT NULL AND p.createdAt < :before")
    List<Payment> findPendingCreatedBefore(@Param("before") java.util.Date before);
}