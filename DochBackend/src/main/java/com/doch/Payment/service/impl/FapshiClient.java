package com.doch.Payment.service.impl;

import com.doch.Payment.dto.fapshi.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/** Thin HTTP client for the Fapshi payment API (https://docs.fapshi.com). */
@Slf4j
@Service
public class FapshiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final WebClient webClient;

    public FapshiClient(
            WebClient.Builder webClientBuilder,
            @Value("${fapshi.base-url}") String fapshiBaseUrl,
            @Value("${fapshi.api-key}") String fapshiApiKey,
            @Value("${fapshi.api-user}") String fapshiApiUser
    ) {
        // clone() so the global builder's base URL is not changed
        this.webClient = webClientBuilder.clone()
                .baseUrl(fapshiBaseUrl)
                .defaultHeader("apiuser", fapshiApiUser)
                .defaultHeader("apikey", fapshiApiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Hosted checkout: returns a payment link the customer opens. */
    public Mono<FapshiChargeResponse> initiatePaymentLink(FapshiChargeRequest chargeRequest) {
        return this.webClient.post()
                .uri("/initiate-pay")
                .bodyValue(chargeRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body ->
                                Mono.error(new FapshiException("initiate-pay", response.statusCode().value(), body))))
                .bodyToMono(FapshiChargeResponse.class)
                .timeout(TIMEOUT);
    }

    /** Direct mobile money prompt sent to the payer's phone. */
    public Mono<FapshiDirectPayResponse> directPay(FapshiDirectPayRequest request) {
        return this.webClient.post()
                .uri("/direct-pay")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body ->
                                Mono.error(new FapshiException("direct-pay", response.statusCode().value(), body))))
                .bodyToMono(FapshiDirectPayResponse.class)
                .timeout(TIMEOUT);
    }

    public Mono<FapshiStatusResponse> getPaymentStatus(String transactionId) {
        return this.webClient.get()
                .uri("/payment-status/{transId}", transactionId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body ->
                                Mono.error(new FapshiException("payment-status", response.statusCode().value(), body))))
                .bodyToMono(FapshiStatusResponse.class)
                .timeout(TIMEOUT);
    }

    /** Expires an unpaid payment link so it can no longer be paid (used when an order is cancelled or superseded). */
    public Mono<FapshiStatusResponse> expirePaymentLink(String transactionId) {
        return this.webClient.post()
                .uri("/expire-pay")
                .bodyValue(Map.of("transId", transactionId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).defaultIfEmpty("").flatMap(body ->
                                Mono.error(new FapshiException("expire-pay", response.statusCode().value(), body))))
                .bodyToMono(FapshiStatusResponse.class)
                .timeout(TIMEOUT);
    }

    public static class FapshiException extends RuntimeException {
        private final int statusCode;

        public FapshiException(String operation, int statusCode, String body) {
            super("Fapshi " + operation + " failed with HTTP " + statusCode + ": " + body);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}