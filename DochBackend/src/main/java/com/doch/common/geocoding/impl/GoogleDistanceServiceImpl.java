package com.doch.common.geocoding.impl;

import com.doch.common.geocoding.DistanceService;
import com.doch.common.geocoding.dto.GoogleDistanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.geo.provider", havingValue = "google", matchIfMissing = true)
public class GoogleDistanceServiceImpl implements DistanceService {

    private final WebClient.Builder webClientBuilder;

    @Value("${google.maps.api-key}")
    private String apiKey;

    @Override
    public Optional<DistanceResult> getRoadDistance(String originLat, String originLng, String destLat, String destLng) {
        String baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/json";
        String origins = originLat + "," + originLng;
        String destinations = destLat + "," + destLng;

        try {
            String finalUri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("origins", origins)
                    .queryParam("destinations", destinations)
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            log.info("DISTANCE API: Requesting route from {} to {}", origins, destinations);

            GoogleDistanceResponse response = webClientBuilder.clone()
                    .build()
                    .get()
                    .uri(finalUri)
                    .retrieve()
                    .bodyToMono(GoogleDistanceResponse.class)
                    .block();

            if (response != null && "OK".equals(response.getStatus()) && !response.getRows().isEmpty()) {
                var element = response.getRows().get(0).getElements().get(0);

                if ("OK".equals(element.getStatus())) {
                    double distanceKm = element.getDistance().getValue() / 1000.0;
                    long durationMins = element.getDuration().getValue() / 60;

                    log.info("DISTANCE SUCCESS: {} km, {} mins", distanceKm, durationMins);
                    return Optional.of(new DistanceResult(distanceKm, durationMins));
                } else {
                    log.error("DISTANCE ELEMENT ERROR: {}", element.getStatus());
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("DISTANCE EXCEPTION: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
