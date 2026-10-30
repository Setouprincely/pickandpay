package com.doch.common.geocoding.impl;

import com.doch.Order.model.Location;
import com.doch.common.geocoding.GeocodingService;
import com.doch.common.geocoding.dto.GoogleGeocodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.geo.provider", havingValue = "google", matchIfMissing = true)
public class GoogleGeocodingServiceImpl implements GeocodingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${google.maps.api-key}")
    private String apiKey;

    @Value("${google.maps.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}")
    private String apiUrl;

    @Override
    public Optional<Location> geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            String finalUri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            log.info("GEOCODING: Requesting [{}]", address);

            // .clone() to isolate this request from Fapshi/Distance settings
            GoogleGeocodeResponse response = webClientBuilder.clone()
                    .build()
                    .get()
                    .uri(finalUri)
                    .retrieve()
                    .bodyToMono(GoogleGeocodeResponse.class)
                    .timeout(Duration.ofSeconds(10)) // Don't let it hang forever
                    .block();

            if (response != null && "OK".equals(response.getStatus()) && !response.getResults().isEmpty()) {
                var coords = response.getResults().get(0).getGeometry().getLocation();
                log.info("GEOCODING STATUS: OK");
                return Optional.of(Location.builder()
                        .latitude(coords.getLat())
                        .longitude(coords.getLng())
                        .build());
            } else {
                log.error("GEOCODING REJECTED: {} - {}",
                        response != null ? response.getStatus() : "NULL",
                        response != null ? response.getErrorMessage() : "No error message");
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("GEOCODING EXCEPTION for address [{}]: {}", address, e.getMessage());
            return Optional.empty();
        }
    }
}
