<script setup lang="ts">
/*
 * DriverDetailsModal — shows the SPECIFIC driver that was clicked.
 * Receives the driver object already loaded on the reports page (from
 * GET /api/drivers). Optionally deep-fetches GET /api/drivers/{id}/profile for
 * any extra fields the list view didn't include.
 */
type DriverDTO = {
  driverId: string
  fullName: string
  vehicleType: string | null
  plateNumber: string | null
  rating: number | null
  totalDeliveries: number | null
  available?: boolean
  isAvailable?: boolean
}

const props = defineProps<{ driver: DriverDTO | null }>()
defineEmits<{ close: [] }>()

const { $t } = useNuxtApp()
const { get } = useApi()

const detail = ref<DriverDTO | null>(null)
const loading = ref(false)

onMounted(async () => {
  if (!props.driver) return
  detail.value = props.driver
  // Try to enrich with the dedicated profile endpoint; fall back to list data.
  try {
    loading.value = true
    const res = await get<DriverDTO>(`/api/drivers/${props.driver.driverId}/profile`)
    if (res) detail.value = { ...props.driver, ...res }
  } catch {
    // list data is enough
  } finally {
    loading.value = false
  }
})

const online = computed(() => detail.value?.available ?? detail.value?.isAvailable ?? false)
const ratingStars = computed(() => {
  const r = Math.round(detail.value?.rating ?? 0)
  return '★'.repeat(r) + '☆'.repeat(5 - r)
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">
        <div class="modal-header">
          <h3 class="modal-title">Driver details</h3>
          <button class="modal-close" @click="$emit('close')">×</button>
        </div>

        <div v-if="!driver" class="modal-state">No driver selected</div>

        <div v-else class="modal-body">
          <div class="dd-top">
            <span class="dd-avatar">{{ (detail?.fullName || 'D').slice(0,2).toUpperCase() }}</span>
            <div>
              <p class="dd-name">{{ detail?.fullName || '—' }}</p>
              <span class="dd-status" :class="online ? 'dd-status--on' : 'dd-status--off'">
                ● {{ online ? $t('driverStatus.actif') : $t('driverStatus.suspendu') }}
              </span>
            </div>
          </div>

          <div class="dd-grid">
            <div class="dd-cell">
              <p class="dd-label">{{ $t('reports.drivers.colRating') }}</p>
              <p class="dd-value">{{ (detail?.rating ?? 0).toFixed(1) }} <span class="dd-stars">{{ ratingStars }}</span></p>
            </div>
            <div class="dd-cell">
              <p class="dd-label">{{ $t('reports.drivers.colDeliveries') }}</p>
              <p class="dd-value">{{ detail?.totalDeliveries ?? 0 }}</p>
            </div>
            <div class="dd-cell">
              <p class="dd-label">{{ $t('reports.fleet.colType') }}</p>
              <p class="dd-value">{{ detail?.vehicleType || '—' }}</p>
            </div>
            <div class="dd-cell">
              <p class="dd-label">{{ $t('reports.fleet.colVehicle') }}</p>
              <p class="dd-value">{{ detail?.plateNumber || '—' }}</p>
            </div>
          </div>

          <p v-if="loading" class="dd-loading">Loading full profile…</p>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" @click="$emit('close')">Close</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 440px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1.1rem 1.25rem 0; }
.modal-title { font-size: 1rem; font-weight: 700; margin: 0; color: var(--color-text-primary); }
.modal-close { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); line-height: 1; }
.modal-state { padding: 2rem; text-align: center; color: var(--color-text-secondary); font-size: 0.875rem; }
.modal-body { padding: 1.25rem; }
.dd-top { display: flex; align-items: center; gap: 0.875rem; margin-bottom: 1.25rem; }
.dd-avatar { width: 2.75rem; height: 2.75rem; border-radius: 50%; background: var(--color-primary); color: #fff; font-size: 0.875rem; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.dd-name { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 0.2rem; }
.dd-status { font-size: 0.75rem; font-weight: 600; }
.dd-status--on { color: #16A34A; }
.dd-status--off { color: #DC2626; }
.dd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.dd-cell { border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.75rem; }
.dd-label { font-size: 0.7rem; color: var(--color-text-muted); margin: 0 0 0.25rem; text-transform: uppercase; letter-spacing: 0.04em; }
.dd-value { font-size: 0.95rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.dd-stars { color: #F59E0B; font-size: 0.8rem; font-weight: 400; }
.dd-loading { font-size: 0.75rem; color: var(--color-text-muted); margin: 0.875rem 0 0; }
.modal-footer { display: flex; justify-content: flex-end; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }
.btn-secondary { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.55rem 1.25rem; font-size: 0.875rem; cursor: pointer; }
</style>
