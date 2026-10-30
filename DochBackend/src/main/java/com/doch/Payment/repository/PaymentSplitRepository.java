package com.doch.Payment.repository;

import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentSplit;
import com.doch.Payment.model.PaymentSplit.SplitType;
import com.doch.User.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, String> {

    /**
     * Finds all splits associated with a specific payment.
     * Useful for auditing a single transaction.
     */
    List<PaymentSplit> findByPayment(Payment payment);

    /**
     * Finds all earnings for a specific user (usually a Seller).
     */
    List<PaymentSplit> findByRecipient(User recipient);

    /**
     * Finds all splits of a certain type (e.g., all PLATFORM_COMMISSION).
     */
    List<PaymentSplit> findByType(SplitType type);

    /**
     * Calculates the total balance for a specific seller.
     * This is the "Main Query" for the Seller Portal Dashboard.
     */
    @Query("SELECT SUM(ps.amount) FROM PaymentSplit ps WHERE ps.recipient.id = :userId AND ps.type = 'SELLER_EARNING'")
    Double getSumOfEarningsByUserId(@Param("userId") String userId);

    /**
     * Calculates total platform revenue (Commission + Delivery Fees).
     * This is for the Admin Dashboard.
     */
    @Query("SELECT SUM(ps.amount) FROM PaymentSplit ps WHERE ps.type IN (:types)")
    Double getTotalPlatformRevenue(@Param("types") List<SplitType> types);
}