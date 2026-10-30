<script setup lang="ts">
/**
 * Self-contained map illustration (inline SVG street map) — renders offline.
 *
 * ── Switching to a real map when the backend is ready ──────────────────────
 * 1. Leaflet is already installed. Replace the `.map-illustration` <svg> block
 *    below with a `<div ref="mapEl" class="map-inner" />` wrapped in <ClientOnly>.
 * 2. In onMounted: `const L = (await import('leaflet')).default ?? ...`,
 *    `import 'leaflet/dist/leaflet.css'`, init `L.map(mapEl.value)` + a tile layer
 *    (OpenStreetMap free, or Mapbox/Google with your API key).
 * 3. Feed `markers` real `{ lat, lng }` from your delivery API instead of x/y %.
 *    Drop colored `L.divIcon` pins per `colors[type]` and a polyline for the route.
 */
type MarkerType = 'self' | 'progress' | 'delivered' | 'pending'

const markers: { type: MarkerType; label?: string; x: number; y: number }[] = [
  { type: 'self',                  x: 25, y: 62 },
  { type: 'progress',  label: '1', x: 38, y: 48 },
  { type: 'delivered', label: '1', x: 68, y: 30 },
  { type: 'pending',   label: '1', x: 58, y: 64 },
  { type: 'pending',   label: '2', x: 40, y: 82 },
]

const { $t } = useNuxtApp()

// Optional: a specific delivery to navigate to. If omitted, the GPS button
// resolves the driver's first active delivery.
const props = withDefaults(defineProps<{ deliveryId?: string }>(), { deliveryId: '' })

const { openNavigation } = useDriverNavigation()
const { get } = useDriverApi()
const gpsError = ref('')

async function onGps() {
  gpsError.value = ''
  let id = props.deliveryId
  if (!id) {
    try {
      const list = await get<any[]>('/api/drivers/me/deliveries')
      const active = (list ?? []).find((d: any) => d.status !== 'DELIVERED' && d.status !== 'CANCELLED') ?? (list ?? [])[0]
      id = active?.deliveryId ?? ''
    } catch {
      gpsError.value = $t('driver.map.routeUnavailable') ?? 'Route not available for this delivery'
      return
    }
  }
  const res = await openNavigation(id)
  if (!res.ok) {
    gpsError.value = $t('driver.map.routeUnavailable') ?? 'Route not available for this delivery'
    setTimeout(() => { gpsError.value = '' }, 3000)
  }
}

const legend = computed(() => [
  { type: 'self',      label: $t('driver.map.legendSelf') },
  { type: 'progress',  label: $t('driver.map.legendProgress') },
  { type: 'pending',   label: $t('driver.map.legendPending') },
  { type: 'delivered', label: $t('driver.map.legendDelivered') },
])
</script>

<template>
  <div class="map-card">
    <div class="map-header">
      <h2 class="map-title">{{ $t('driver.map.title') }}</h2>
      <button class="gps-btn" @click="onGps">
        <UIcon name="i-heroicons-paper-airplane" class="gps-btn__icon" />
        {{ $t('driver.map.gps') }}
      </button>
    </div>

    <p v-if="gpsError" class="gps-error">{{ gpsError }}</p>

    <div class="map-canvas">

      <!-- Street-map illustration -->
      <svg class="map-illustration" viewBox="0 0 400 320" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <!-- land -->
        <rect width="400" height="320" fill="#E9EDF0" />

        <!-- parks -->
        <rect x="20"  y="30"  width="70" height="55" rx="8" fill="#D6E8D2" />
        <rect x="300" y="190" width="80" height="90" rx="8" fill="#D6E8D2" />
        <rect x="250" y="20"  width="60" height="45" rx="8" fill="#D6E8D2" />

        <!-- river -->
        <path d="M -10 90 C 80 120, 140 70, 210 110 S 340 180, 420 150 L 420 200 C 340 230, 260 160, 200 175 S 70 170, -10 140 Z"
              fill="#BFE0EF" opacity="0.9" />

        <!-- roads -->
        <g stroke="#FFFFFF" stroke-linecap="round">
          <line x1="0" y1="60"  x2="400" y2="60"  stroke-width="8" />
          <line x1="0" y1="170" x2="400" y2="170" stroke-width="10" />
          <line x1="0" y1="250" x2="400" y2="250" stroke-width="7" />
          <line x1="90"  y1="0" x2="90"  y2="320" stroke-width="8" />
          <line x1="200" y1="0" x2="200" y2="320" stroke-width="10" />
          <line x1="310" y1="0" x2="310" y2="320" stroke-width="7" />
          <line x1="0" y1="0" x2="200" y2="170" stroke-width="6" />
          <line x1="200" y1="170" x2="400" y2="80" stroke-width="6" />
        </g>

        <!-- building blocks -->
        <g fill="#DEE3E7">
          <rect x="105" y="75"  width="40" height="35" rx="3" />
          <rect x="155" y="75"  width="35" height="35" rx="3" />
          <rect x="215" y="80"  width="45" height="30" rx="3" />
          <rect x="105" y="185" width="40" height="40" rx="3" />
          <rect x="155" y="185" width="35" height="40" rx="3" />
          <rect x="215" y="190" width="50" height="35" rx="3" />
          <rect x="320" y="80"  width="45" height="35" rx="3" />
        </g>
      </svg>

      <!-- route line: self → in-progress -->
      <svg class="map-route" viewBox="0 0 100 100" preserveAspectRatio="none">
        <line x1="25" y1="62" x2="38" y2="48" stroke="#EF4444" stroke-width="0.7" stroke-dasharray="2.5" />
      </svg>

      <!-- markers -->
      <span
        v-for="(m, i) in markers" :key="i"
        class="marker" :class="`marker--${m.type}`"
        :style="{ left: m.x + '%', top: m.y + '%' }"
      >
        <svg viewBox="0 0 24 24" class="marker__icon" fill="currentColor">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" />
        </svg>
        <span v-if="m.label" class="marker__badge">{{ m.label }}</span>
      </span>

      <!-- legend -->
      <div class="map-legend">
        <div v-for="l in legend" :key="l.type" class="legend-item">
          <span class="legend-dot" :class="`legend-dot--${l.type}`" />
          {{ l.label }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-card   { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.map-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
.map-title  { font-size: 0.9375rem; font-weight: 600; color: var(--color-text-primary); margin: 0; }

.gps-btn        { display: flex; align-items: center; gap: 0.5rem; background: #2563EB; color: #fff;
                  border: none; border-radius: 0.5rem; padding: 0.5rem 1rem; font-size: 0.8125rem;
                  font-weight: 600; cursor: pointer; }
.gps-btn:hover  { background: #1D4ED8; }
.gps-btn__icon  { width: 1rem; height: 1rem; }
.gps-error { margin: 0 0 0.75rem; font-size: 0.8125rem; color: #DC2626; }

.map-canvas { position: relative; height: 320px; border-radius: 0.5rem; overflow: hidden; }
.map-illustration { position: absolute; inset: 0; width: 100%; height: 100%; }

.map-route { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; }

/* Markers */
.marker { position: absolute; transform: translate(-50%, -100%); z-index: 2; }
.marker__icon { width: 1.875rem; height: 1.875rem; filter: drop-shadow(0 2px 3px rgba(0,0,0,0.3)); display: block; }
.marker__badge {
  position: absolute; top: 3px; left: 50%; transform: translateX(-50%);
  font-size: 9px; font-weight: 700; color: #fff; line-height: 1;
}
.marker--self      { color: #EF4444; }
.marker--progress  { color: #2563EB; }
.marker--delivered { color: #16A34A; }
.marker--pending   { color: #9CA3AF; }

/* Legend */
.map-legend {
  position: absolute; bottom: 0.75rem; left: 0.75rem; z-index: 3;
  background: #fff; border-radius: 0.5rem; padding: 0.625rem 0.75rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  display: flex; flex-direction: column; gap: 0.375rem;
}
.legend-item { display: flex; align-items: center; gap: 0.5rem; font-size: 0.6875rem; color: var(--color-text-secondary); }
.legend-dot  { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-dot--self      { background: #EF4444; }
.legend-dot--progress  { background: #2563EB; }
.legend-dot--pending   { background: #9CA3AF; }
.legend-dot--delivered { background: #16A34A; }
</style>
