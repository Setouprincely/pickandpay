package com.doch.Payment.dto;

import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toDto(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paymentFlow(payment.getPaymentFlow())
                .paymentMethod(payment.getPaymentMethod())
                .providerStatus(payment.getProviderStatus())
                .providerReference(payment.getGatewayTransactionId())
                .paymentUrl(payment.getStatus() == PaymentStatus.PENDING ? payment.getPaymentUrl() : null)
                .context(payment.getXenderRequest() != null ? "XENDER" : "MARKET")
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderReference(payment.getOrder() != null ? payment.getOrder().getReference() : null)
                .xenderRequestId(payment.getXenderRequest() != null ? payment.getXenderRequest().getId() : null)
                .paidAt(payment.getPaidAt())
                .failureReason(payment.getFailureReason())
                .message(messageFor(payment))
                .build();
    }

    private String messageFor(Payment payment) {
        if (payment.getStatus() == null) return null;
        return switch (payment.getStatus()) {
            case PENDING -> "DIRECT".equals(payment.getPaymentFlow())
                    ? "A payment request was sent to " + payment.getPayerPhone() + ". Approve it on your phone."
                    : "Payment initiated. Use the payment link to complete the transaction.";
            case COMPLETED -> "Payment successful.";
            case FAILED -> "Payment failed or expired. You can retry the payment.";
            case CANCELLED -> "Payment cancelled.";
            case PROCESSING -> "Payment is being processed.";
        };
    }
}