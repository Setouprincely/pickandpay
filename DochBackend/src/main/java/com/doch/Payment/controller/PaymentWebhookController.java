package com.doch.Payment.controller;

import com.doch.Payment.dto.fapshi.FapshiWebhookDTO;
import com.doch.Payment.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook Endpoint")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * Shared secret configured on the Fapshi dashboard, sent in the x-wh-secret header.
     * When configured, calls without the matching secret are rejected.
     */
    @Value("${fapshi.webhook-secret:}")
    private String webhookSecret;

    /**
     * Fapshi webhook. The payload status is NOT trusted: the payment is re-queried from the
     * Fapshi API and only the provider's answer is applied (see PaymentService#handleProviderCallback).
     * Duplicate deliveries are harmless because processing is idempotent and row-locked.
     */
    @PostMapping("/fapshi")
    public ResponseEntity<Map<String, String>> handleFapshiWebhook(
            @RequestBody FapshiWebhookDTO payload,
            @RequestHeader(value = "x-wh-secret", required = false) String whSecret) {

        if (StringUtils.hasText(webhookSecret) && !constantTimeEquals(webhookSecret, whSecret)) {
            log.warn("FAPSHI WEBHOOK REJECTED: missing or invalid x-wh-secret header.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid webhook secret"));
        }

        log.info("FAPSHI WEBHOOK: transId={}, reportedStatus={}, externalId={}",
                payload.getTransId(), payload.getStatus(), payload.getExternalId());
        try {
            paymentService.handleProviderCallback(payload.getExternalId(), payload.getTransId());
        } catch (Exception e) {
            // Always acknowledge: the reconciliation job will retry from the provider status.
            log.error("FAPSHI WEBHOOK: processing failed for transId {}: {}", payload.getTransId(), e.getMessage());
        }
        return ResponseEntity.ok(Map.of("message", "Acknowledgement of webhook receipt"));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}