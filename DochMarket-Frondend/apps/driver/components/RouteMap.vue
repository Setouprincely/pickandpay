<script setup lang="ts">
/**
 * Maps & Routes view. Self-contained SVG street-map illustration (renders offline).
 *
 * phase = 'navigating' → bottom "Next Destination" card + Start Navigation
 * phase = 'arrived'    → centered "End of Route" card + Deliver Package
 *
 * Backend integration: swap the <svg class="map-illustration"> for a real Leaflet
 * map (already installed) — see the note in DeliveryMap.vue. Feed coords, ETA,
 * distance and phase from your delivery API.
 */
const props = withDefaults(defineProps<{
  phase?: 'navigating' | 'arrived'
  destination?: string
  time?: string
  eta?: string
  distance?: string
  deliveryId?: string
}>(), {
  phase: 'navigating',
  deliveryId: '',
})

defineEmits<{ navigate: []; deliver: [] }>()

const { $t } = useNuxtApp()
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
      <svg class="map-illustration" viewBox="0 0 400 280" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        <rect width="400" height="280" fill="#E9EDF0" />
        <rect x="20"  y="25"  width="70" height="50" rx="8" fill="#D6E8D2" />
        <rect x="300" y="170" width="80" height="80" rx="8" fill="#D6E8D2" />
        <path d="M -10 80 C 80 110, 140 60, 210 100 S 340 170, 420 140 L 420 185 C 340 215, 260 150, 200 165 S 70 160, -10 125 Z"
              fill="#BFE0EF" opacity="0.9" />
        <g stroke="#FFFFFF" stroke-linecap="round">
          <line x1="0" y1="55"  x2="400" y2="55"  stroke-width="8" />
          <line x1="0" y1="150" x2="400" y2="150" stroke-width="10" />
          <line x1="0" y1="220" x2="400" y2="220" stroke-width="7" />
          <line x1="90"  y1="0" x2="90"  y2="280" stroke-width="8" />
          <line x1="200" y1="0" x2="200" y2="280" stroke-width="10" />
          <line x1="310" y1="0" x2="310" y2="280" stroke-width="7" />
        </g>
        <g fill="#DEE3E7">
          <rect x="105" y="65"  width="40" height="30" rx="3" />
          <rect x="215" y="70"  width="45" height="25" rx="3" />
          <rect x="105" y="165" width="40" height="35" rx="3" />
          <rect x="215" y="170" width="50" height="30" rx="3" />
        </g>
      </svg>

      <!-- route line -->
      <svg class="map-route" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline points="22,80 22,55 55,55 55,30 78,30"
                  fill="none" stroke="#2563EB" stroke-width="0.9" />
      </svg>

      <!-- destination marker (green) -->
      <span class="dest-marker">
        <UIcon name="i-heroicons-map-pin-solid" class="dest-pin__icon dest-pin__icon--green" />
      </span>

      <!-- on 'arrived', show the driver position right next to the destination -->
      <span v-if="phase === 'arrived'" class="self-marker">
        <UIcon name="i-heroicons-map-pin-solid" class="dest-pin__icon dest-pin__icon--red" />
      </span>

      <!-- ETA hint (navigating only) -->
      <span v-if="phase === 'navigating' && eta" class="map-eta">{{ eta }}</span>

      <!-- legend -->
      <div class="map-legend">
        <div v-for="l in legend" :key="l.type" class="legend-item">
          <span class="legend-dot" :class="`legend-dot--${l.type}`" />
          {{ l.label }}
        </div>
      </div>

      <!-- Navigating: bottom Next Destination card -->
      <div v-if="phase === 'navigating'" class="next-card">
        <div class="next-row">
          <div>
            <p class="next-label">{{ $t('driver.route.nextDestination') }}</p>
            <p class="next-sub">{{ destination }}</p>
          </div>
          <span class="next-time">{{ time }}</span>
        </div>
        <button class="action-btn" @click="$emit('navigate')">
          <UIcon name="i-heroicons-paper-airplane" class="action-btn__icon" />
          {{ $t('driver.route.startNavigation') }}
        </button>
      </div>

      <!-- Arrived: centered End of Route card -->
      <div v-else class="end-card">
        <div class="next-row">
          <div>
            <p class="next-label">{{ $t('driver.route.endOfRoute') }}</p>
            <p class="next-sub">{{ distance }}</p>
          </div>
          <span class="next-time">{{ time }}</span>
        </div>
        <button class="action-btn" @click="$emit('deliver')">
          {{ $t('driver.route.deliverPackage') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-card   { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.map-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
.map-title  { font-size: 0.9375rem; font-weight: 600; color: var(--color-text-primary); margin: 0; }

.gps-btn       { display: flex; align-items: center; gap: 0.5rem; background: #2563EB; color: #fff;
                 border: none; border-radius: 0.5rem; padding: 0.5rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
.gps-btn:hover { background: #1D4ED8; }
.gps-btn__icon { width: 1rem; height: 1rem; }
.gps-error { margin: 0 0 0.75rem; font-size: 0.8125rem; color: #DC2626; }

.map-canvas { position: relative; height: 340px; border-radius: 0.5rem; overflow: hidden; }
.map-illustration { position: absolute; inset: 0; width: 100%; height: 100%; }
.map-route  { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; }

/* Markers */
.dest-marker { position: absolute; left: 78%; top: 30%; transform: translate(-50%, -100%); z-index: 2; }
.self-marker { position: absolute; left: 70%; top: 31%; transform: translate(-50%, -100%); z-index: 2; }
.dest-pin__icon { width: 2rem; height: 2rem; filter: drop-shadow(0 2px 3px rgba(0,0,0,0.3)); display: block; }
.dest-pin__icon--green { color: #16A34A; }
.dest-pin__icon--red   { color: #EF4444; }

.map-eta { position: absolute; left: 50%; top: 56%; transform: translateX(-50%); z-index: 2;
           font-size: 0.625rem; color: var(--color-text-muted); background: rgba(255,255,255,0.7);
           padding: 1px 6px; border-radius: 4px; white-space: nowrap; }

/* Legend */
.map-legend { position: absolute; bottom: 0.75rem; left: 0.75rem; z-index: 3;
              background: #fff; border-radius: 0.5rem; padding: 0.625rem 0.75rem;
              box-shadow: 0 2px 8px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 0.375rem; }
.legend-item { display: flex; align-items: center; gap: 0.5rem; font-size: 0.6875rem; color: var(--color-text-secondary); }
.legend-dot  { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-dot--self      { background: #EF4444; }
.legend-dot--progress  { background: #2563EB; }
.legend-dot--delivered { background: #16A34A; }

/* Shared card bits */
.next-row    { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 0.75rem; }
.next-label  { font-size: 0.875rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 0.125rem; }
.next-sub    { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0; }
.next-time   { font-size: 0.8125rem; color: var(--color-text-secondary); white-space: nowrap; flex-shrink: 0; }

.action-btn  { width: 100%; display: flex; align-items: center; justify-content: center; gap: 0.5rem;
               background: #2563EB; color: #fff; border: none; border-radius: 0.5rem;
               padding: 0.625rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
.action-btn:hover { background: #1D4ED8; }
.action-btn__icon { width: 1rem; height: 1rem; }

/* Navigating: anchored to bottom */
.next-card { position: absolute; left: 0.75rem; right: 0.75rem; bottom: 0.75rem; z-index: 3;
             background: #fff; border-radius: 0.625rem; padding: 0.875rem 1rem;
             box-shadow: 0 4px 16px rgba(0,0,0,0.12); }

/* Arrived: centered overlay */
.end-card  { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); z-index: 4;
             width: min(320px, calc(100% - 2rem));
             background: #fff; border-radius: 0.625rem; padding: 1rem 1.25rem;
             box-shadow: 0 10px 40px rgba(0,0,0,0.18); }
</style>
