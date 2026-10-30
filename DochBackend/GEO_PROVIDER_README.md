# Geo Provider Switch — Google or OpenStreetMap

Two interchangeable, production-grade provider stacks behind the existing
GeocodingService / DistanceService interfaces, selected at boot:

    GEO_PROVIDER=google   (default) Google Geocoding + Distance Matrix.
                          Requires a billing-enabled GOOGLE_MAPS_API_KEY.
    GEO_PROVIDER=osm      OpenStreetMap stack: Nominatim-format geocoding +
                          OSRM-format routing. No Google billing required.

## Files (under src/main/java/com/doch/common/geocoding/)
- NEW  impl/OsmGeocodingServiceImpl.java
- NEW  impl/OsmDistanceServiceImpl.java
- NEW  dto/NominatimResult.java         (concrete DTO; Boot 4 / Jackson 3 compatible)
- NEW  dto/OsrmRouteResponse.java       (concrete DTO; Boot 4 / Jackson 3 compatible)
- MOD  impl/GoogleGeocodingServiceImpl.java  (conditional on provider=google, the default)
- MOD  impl/GoogleDistanceServiceImpl.java   (conditional on provider=google, the default)

## What makes the OSM stack production-grade
- **Response caching (LRU):** repeated addresses and routes never trigger a
  network call. In a delivery platform, origins (stores) and popular
  destinations repeat constantly, so the effective hit rate is high.
- **Rate limiting:** outbound requests are spaced by `min-interval-ms`
  (default 1000ms), keeping usage within the public endpoints' service terms.
- **Retry + timeouts:** one automatic retry on transient failures; 10s timeout.
- **Haversine fallback (distance):** if the routing endpoint is unreachable,
  distance = straight-line x road factor (default 1.4) with a speed-based
  duration estimate. Checkout and delivery pricing never fail because a
  routing server is down. Fallback results are not cached, so real routing is
  retried on the next request, and every fallback is logged for monitoring.
- **Pluggable endpoints:** the same code serves three deployment tiers -
  change config, not code.

## Deployment tiers for GEO_PROVIDER=osm
1. **Public endpoints** (default config): fine at modest volume; the built-in
   rate limiter and cache keep you within their terms.
2. **Commercial Nominatim/OSRM-compatible provider** (e.g. LocationIQ):
   set the URLs + `api-key`; raise throughput by setting `min-interval-ms: 0`.
3. **Self-hosted** (recommended at scale): run Nominatim + OSRM with the
   Cameroon OSM extract (small; fits a modest VPS via Docker), point the URLs
   at them, set `min-interval-ms: 0`. Full control, zero per-request cost.

## Config — application.yml (under the existing `application:` block)
```yaml
application:
  geo:
    provider: ${GEO_PROVIDER:google}
    osm:
      geocoding-url: ${OSM_GEOCODING_URL:https://nominatim.openstreetmap.org/search}
      routing-url:   ${OSM_ROUTING_URL:https://router.project-osrm.org/route/v1}
      api-key:       ${OSM_API_KEY:}          # for commercial providers
      country-codes: cm                        # bias results to Cameroon
      user-agent:    "DOCH-Backend/1.0 (noreply@contact.dochlogistics.com)"
      min-interval-ms: 1000                    # 0 for self-hosted/commercial
      cache-size: 5000
      timeout-seconds: 10
      fallback-enabled: true
      fallback-road-factor: 1.4
      fallback-avg-speed-kmh: 30
```
All `osm.*` keys are optional - the defaults above are built into the code.
The minimal change is just `provider: ${GEO_PROVIDER:google}`.

## Config — .env.example
```
# Geo provider: google (needs billing-enabled key) | osm (OpenStreetMap stack)
GEO_PROVIDER=osm
# Optional overrides for the OSM stack (self-hosted or commercial endpoints)
# OSM_GEOCODING_URL=
# OSM_ROUTING_URL=
# OSM_API_KEY=
```

## Notes
- NavigationService (driver route optimization) is pure in-process logic and
  needs no provider.
- With provider=google (or unset), behaviour is byte-for-byte identical to
  before this patch.
- If you later enable Google billing, both stacks coexist in the codebase;
  switching is a restart with a different env var.
