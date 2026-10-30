package com.doch.Payment.dto;

import com.doch.Payment.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDTO {
    private String paymentId;
    private PaymentStatus status;
    private Double amount;
    /** LINK or DIRECT. */
    private String paymentFlow;
    private String paymentMethod;
    /** Last status reported by Fapshi. */
    private String providerStatus;
    /** Fapshi transaction ID (payment reference). */
    private String providerReference;
    private String paymentUrl; // URL for the user to complete payment (LINK flow)
    private String message;
    private String redirectUrl; // The actual URL user will be redirected to after payment
    private String context;     // "MARKET" or "XENDER" for frontend routing
    private String orderId;
    private String orderReference;
    private String xenderRequestId;
    private LocalDateTime paidAt;
    private String failureReason;
}