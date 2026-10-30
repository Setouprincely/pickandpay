package com.doch.common.geocoding.impl;

import com.doch.common.geocoding.DistanceService;
import com.doch.common.geocoding.dto.OsrmRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OpenStreetMap-based road distance provider (OSRM API format).
 *
 * Activated with GEO_PROVIDER=osm (application.geo.provider=osm).
 *
 * Production characteristics:
 * - LRU response cache keyed on rounded coordinate pairs: repeated routes
 *   (same store to same neighbourhood) are answered without a network call.
 * - Rate limiter with configurable spacing between outbound requests.
 * - One automatic retry on transient failure, with request timeouts.
 * - Deterministic haversine fallback: if the routing endpoint is unreachable,
 *   distance is estimated as straight-line * a configurable road factor, so
 *   checkout and delivery pricing NEVER fail because a routing server is down.
 *   Fallback usage is logged for monitoring.
 * - Pluggable endpoint: public server, commercial OSRM-compatible provider
 *   (optional ?key=), or a self-hosted OSRM instance (recommended for high
 *   volume; a Cameroon OSM extract runs comfortably on a small VPS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.geo.provider", havingValue = "osm")
public class OsmDistanceServiceImpl implements DistanceService {

    private final WebClient.Builder webClientBuilder;

    /** Base URL up to and including the route API version, e.g. https://router.project-osrm.org/route/v1 */
    @Value("${application.geo.osm.routing-url:https://router.project-osrm.org/route/v1}")
    private String routingBaseUrl;

    /** Optional API key for commercial OSRM-compatible providers (sent as ?key=). */
    @Value("${application.geo.osm.api-key:}")
    private String apiKey;

    /** Minimum spacing between outbound requests; 0 for self-hosted/commercial. */
    @Value("${application.geo.osm.min-interval-ms:1000}")
    private long minIntervalMs;

    @Value("${application.geo.osm.cache-size:5000}")
    private int cacheSize;

    @Value("${application.geo.osm.timeout-seconds:10}")
    private long timeoutSeconds;

    /** Multiplier applied to straight-line distance when falling back (roads are not straight lines). */
    @Value("${application.geo.osm.fallback-road-factor:1.4}")
    private double fallbackRoadFactor;

    /** Assumed average speed (km/h) for the fallback duration estimate. */
    @Value("${application.geo.osm.fallback-avg-speed-kmh:30}")
    private double fallbackAvgSpeedKmh;

    /** Whether to estimate via haversine when the routing endpoint fails. */
    @Value("${application.geo.osm.fallback-enabled:true}")
    private boolean fallbackEnabled;

    /** LRU cache of rounded "lat1,lng1->lat2,lng2" -> result. */
    private final Map<String, DistanceResult> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, DistanceResult> eldest) {
                    return size() > Math.max(cacheSize, 100);
                }
            });

    private final Object rateLimitLock = new Object();
    private long nextAllowedRequestAt = 0L;

    @Override
    public Optional<DistanceResult> getRoadDistance(String originLat, String originLng,
                                                    String destLat, String destLng) {
        double oLat, oLng, dLat, dLng;
        try {
            oLat = Double.parseDouble(originLat.trim());
            oLng = Double.parseDouble(originLng.trim());
            dLat = Double.parseDouble(destLat.trim());
            dLng = Double.parseDouble(destLng.trim());
        } catch (Exception e) {
            log.error("OSM DISTANCE: invalid coordinates [{},{} -> {},{}]", originLat, originLng, destLat, destLng);
            return Optional.empty();
        }

        // ~4 decimal places (~11m) is precise enough to reuse a cached route.
        String key = String.format("%.4f,%.4f->%.4f,%.4f", oLat, oLng, dLat, dLng);
        DistanceResult cached = cache.get(key);
        if (cached != null) {
            log.debug("OSM DISTANCE: cache hit for {}", key);
            return Optional.of(cached);
        }

        // Routed distance with one retry on transient failure.
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                respectRateLimit();
                Optional<DistanceResult> result = fetchRoute(oLat, oLng, dLat, dLng);
                if (result.isPresent()) {
                    cache.put(key, result.get());
                    return result;
                }
            } catch (Exception e) {
                log.warn("OSM DISTANCE attempt {}/2 failed: {}", attempt, e.getMessage());
            }
        }

        // Deterministic fallback: pricing must not depend on routing uptime.
        if (fallbackEnabled) {
            DistanceResult estimate = haversineEstimate(oLat, oLng, dLat, dLng);
            log.warn("OSM DISTANCE: routing endpoint unavailable, using haversine estimate: {} km (factor {})",
                    estimate.distanceKm(), fallbackRoadFactor);
            // Deliberately NOT cached: retry real routing on the next request.
            return Optional.of(estimate);
        }

        log.error("OSM DISTANCE: routing failed and fallback disabled");
        return Optional.empty();
    }

    private Optional<DistanceResult> fetchRoute(double oLat, double oLng, double dLat, double dLng) {
        // OSRM expects lon,lat order (opposite of Google's lat,lng)
        StringBuilder uri = new StringBuilder()
                .append(routingBaseUrl.replaceAll("/$", ""))
                .append("/driving/")
                .append(oLng).append(',').append(oLat)
                .append(';')
                .append(dLng).append(',').append(dLat)
                .append("?overview=false");
        if (apiKey != null && !apiKey.isBlank()) {
            uri.append("&key=").append(apiKey);
        }

        log.info("OSM DISTANCE: requesting route ({},{}) -> ({},{})", oLat, oLng, dLat, dLng);

        // Bind to a concrete DTO (NOT JsonNode: Spring Boot 4 uses Jackson 3,
        // whose codec cannot decode into the Jackson 2 databind tree type).
        OsrmRouteResponse response = webClientBuilder.clone()
                .build()
                .get()
                .uri(uri.toString())
                .retrieve()
                .bodyToMono(OsrmRouteResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        if (response != null && "Ok".equals(response.getCode())
                && response.getRoutes() != null && !response.getRoutes().isEmpty()) {

            OsrmRouteResponse.Route route = response.getRoutes().get(0);
            double distanceKm = route.getDistance() / 1000.0;
            long durationMins = Math.round(route.getDuration() / 60.0);

            log.info("OSM DISTANCE: OK -> {} km, {} mins", distanceKm, durationMins);
            return Optional.of(new DistanceResult(distanceKm, durationMins));
        }

        log.warn("OSM DISTANCE: endpoint answered without a route (code={})",
                response != null ? response.getCode() : "null");
        return Optional.empty();
    }

    /** Great-circle distance scaled by a road factor, with a speed-based duration estimate. */
    private DistanceResult haversineEstimate(double oLat, double oLng, double dLat, double dLng) {
        final double R = 6371.0; // Earth radius, km
        double dLatRad = Math.toRadians(dLat - oLat);
        double dLngRad = Math.toRadians(dLng - oLng);
        double a = Math.sin(dLatRad / 2) * Math.sin(dLatRad / 2)
                + Math.cos(Math.toRadians(oLat)) * Math.cos(Math.toRadians(dLat))
                * Math.sin(dLngRad / 2) * Math.sin(dLngRad / 2);
        double straightKm = 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double roadKm = Math.round(straightKm * fallbackRoadFactor * 100.0) / 100.0;
        long durationMins = Math.max(1, Math.round(roadKm / Math.max(fallbackAvgSpeedKmh, 1.0) * 60.0));
        return new DistanceResult(roadKm, durationMins);
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
