package com.doch.Payment.service;

import com.doch.Order.model.Order;
import com.doch.Payment.dto.PaymentRequestDTO;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.doch.User.model.User;

import java.util.List;

public interface PaymentService {

    /** Starts (or resumes) a payment for an order / Xender request owned by {@code currentUser}. */
    PaymentResponseDTO initiatePayment(PaymentRequestDTO request, User currentUser);

    /** Returns the payment status, refreshing it from the provider when it is still pending. */
    PaymentResponseDTO checkPaymentStatus(String paymentId, User currentUser);

    /** Re-reads the provider status of a payment and applies it (idempotent, row-locked). */
    PaymentResponseDTO reconcile(String paymentId);

    /** Webhook entry point. The payload status is never trusted: the provider is always re-queried. */
    void handleProviderCallback(String externalId, String transactionId);

    /** Reconciles every pending attempt of an order (used before cancelling it). */
    void reconcileOrderPayments(Order order);

    /** Invalidates pending payment links of an order that is being cancelled. */
    void cancelPendingPayments(Order order, String reason);

    /** Pending payments that should be reconciled by the background job. */
    List<String> findPaymentsToReconcile();
}
