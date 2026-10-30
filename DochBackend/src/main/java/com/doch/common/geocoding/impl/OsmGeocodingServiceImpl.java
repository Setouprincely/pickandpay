package com.doch.common.geocoding.impl;

import com.doch.Order.model.Location;
import com.doch.common.geocoding.GeocodingService;
import com.doch.common.geocoding.dto.NominatimResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * OpenStreetMap-based geocoding provider (Nominatim API format).
 *
 * Activated with GEO_PROVIDER=osm (application.geo.provider=osm).
 *
 * Production characteristics:
 * - LRU response cache: repeated addresses (store locations, common landmarks)
 *   are resolved without any network call.
 * - Rate limiter: outbound requests are spaced by a configurable minimum
 *   interval, keeping usage within the endpoint's service terms.
 * - One automatic retry on transient failure, with request timeouts.
 * - Pluggable endpoint: works unchanged against the public server, a
 *   commercial Nominatim-compatible provider (e.g. LocationIQ via
 *   application.geo.osm.api-key), or a self-hosted Nominatim instance.
 *
 * Endpoint sizing guidance: the public endpoint's terms cap throughput at
 * 1 req/s, which the default min-interval honours; for higher sustained
 * volume, point geocoding-url at a self-hosted or commercial instance and set
 * min-interval-ms to 0.
 */
@Slf4j
@Service
@Primary // takes precedence over the named mockGeocodingService bean when active
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.geo.provider", havingValue = "osm")
public class OsmGeocodingServiceImpl implements GeocodingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${application.geo.osm.geocoding-url:https://nominatim.openstreetmap.org/search}")
    private String apiUrl;

    /** Optional API key for commercial Nominatim-compatible providers (sent as ?key=). */
    @Value("${application.geo.osm.api-key:}")
    private String apiKey;

    /** Bias results to a country (ISO code). Default: Cameroon. Empty = worldwide. */
    @Value("${application.geo.osm.country-codes:cm}")
    private String countryCodes;

    /** Identifying User-Agent, required by the public endpoint's terms. */
    @Value("${application.geo.osm.user-agent:DOCH-Backend/1.0 (noreply@contact.dochlogistics.com)}")
    private String userAgent;

    /** Minimum spacing between outbound requests. 1000ms complies with the public endpoint; 0 for self-hosted/commercial. */
    @Value("${application.geo.osm.min-interval-ms:1000}")
    private long minIntervalMs;

    @Value("${application.geo.osm.cache-size:5000}")
    private int cacheSize;

    @Value("${application.geo.osm.timeout-seconds:10}")
    private long timeoutSeconds;

    /** LRU cache of normalized address -> [lat, lon]. Coordinates are immutable facts, so no TTL is needed. */
    private final Map<String, String[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > Math.max(cacheSize, 100);
                }
            });

    private final Object rateLimitLock = new Object();
    private long nextAllowedRequestAt = 0L;

    @Override
    public Optional<Location> geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        // Small landmarks are often unmapped in OSM, so broaden "spot, area, city, region" one part at a time.
        String candidate = address.trim();
        while (true) {
            Optional<Location> hit = geocodeExact(candidate);
            int comma = candidate.indexOf(',');
            if (hit.isPresent() || comma < 0 || candidate.indexOf(',', comma + 1) < 0) {
                return hit;
            }
            candidate = candidate.substring(comma + 1).trim();
        }
    }

    private Optional<Location> geocodeExact(String address) {
        String key = address.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        // 1. Cache hit: no network call at all.
        String[] cached = cache.get(key);
        if (cached != null) {
            log.debug("OSM GEOCODING: cache hit for [{}]", address);
            return Optional.of(toLocation(cached[0], cached[1]));
        }

        // 2. Miss: fetch with one retry on transient failure.
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                respectRateLimit();
                Optional<String[]> coords = fetch(address);
                if (coords.isPresent()) {
                    cache.put(key, coords.get());
                    return Optional.of(toLocation(coords.get()[0], coords.get()[1]));
                }
                // A well-formed "no results" answer is definitive; do not retry.
                log.warn("OSM GEOCODING: no results for [{}]", address);
                return Optional.empty();
            } catch (Exception e) {
                log.warn("OSM GEOCODING attempt {}/2 failed for [{}]: {}", attempt, address, e.getMessage());
            }
        }

        log.error("OSM GEOCODING: giving up on [{}] after retries", address);
        return Optional.empty();
    }

    private Optional<String[]> fetch(String address) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 1);
        if (countryCodes != null && !countryCodes.isBlank()) {
            builder.queryParam("countrycodes", countryCodes);
        }
        if (apiKey != null && !apiKey.isBlank()) {
            builder.queryParam("key", apiKey);
        }
        String finalUri = builder.build().toUriString();

        log.info("OSM GEOCODING: requesting [{}]", address);

        // Nominatim returns a JSON array of results; bind to a concrete DTO
        // (NOT JsonNode: Spring Boot 4 uses Jackson 3, whose codec cannot decode
        // into the Jackson 2 databind tree type).
        NominatimResult[] response = webClientBuilder.clone()
                .build()
                .get()
                .uri(finalUri)
                .header("User-Agent", userAgent)
                .retrieve()
                .bodyToMono(NominatimResult[].class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response != null && response.length > 0) {
            NominatimResult hit = response[0];
            if (hit.getLat() != null && hit.getLon() != null) {
                log.info("OSM GEOCODING: OK -> {}, {} [{}]", hit.getLat(), hit.getLon(), hit.getDisplayName());
                return Optional.of(new String[]{hit.getLat(), hit.getLon()});
            }
        }
        return Optional.empty();
    }

    /** Always return a fresh entity instance; cached coordinates are shared, entities are not. */
    private Location toLocation(String lat, String lon) {
        return Location.builder().latitude(lat).longitude(lon).build();
    }

    /** Spaces outbound requests by minIntervalMs across all threads. */
    private void respectRateLimit() throws InterruptedException {
        if (minIntervalMs <= 0) return;
        long waitMs;
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            waitMs = Math.max(0, nextAllowedRequestAt - now);
            nextAllowedRequestAt = Math.max(now, nextAllowedRequestAt) + minIntervalMs;
        }
        if (waitMs > 0) {
            Thread.sleep(waitMs);
        }
    }
}
