<script setup lang="ts">
// REAL DeliveryResponseDTO (confirmed from backend source):
//   deliveryId, status, price, receiverName, deliveryNote,
//   pickupLocationAddress, receiverAddress, parcelImageUrls, ...
// Status enum: PENDING_ASSIGNMENT | DRIVER_ASSIGNED | PICKED_UP | IN_TRANSIT
//              | DELIVERED | CANCELLED | FAILED
// There is NO estimatedTime and NO per-delivery distance in the API, so those
// invented fields are removed.
type Delivery = {
  deliveryId: string
  status: string
  price: number
  pickupLocationAddress: string
  receiverAddress: string
  receiverName: string
}

const { get } = useDriverApi()
const { $t } = useNuxtApp()

const deliveries = ref<Delivery[]>([])
const loading = ref(true)
const error = ref(false)
const page = ref(1)
const perPage = 3

onMounted(async () => {
  try {
    const res = await get<any[]>('/api/drivers/me/deliveries')
    deliveries.value = (res ?? []).map((d: any) => ({
      deliveryId: d.deliveryId,
      status: d.status ?? 'PENDING_ASSIGNMENT',
      price: d.price ?? 0,
      pickupLocationAddress: d.pickupLocationAddress ?? '—',
      receiverAddress: d.receiverAddress ?? '—',
      receiverName: d.receiverName ?? '',
    }))
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})

const totalPages = computed(() => Math.max(1, Math.ceil(deliveries.value.length / perPage)))
const paged = computed(() =>
  deliveries.value.slice((page.value - 1) * perPage, page.value * perPage)
)

// Map the real status enum to a badge style + label
const statusClass = (s: string) => ({
  PENDING_ASSIGNMENT: 'badge--pending',
  DRIVER_ASSIGNED:    'badge--pending',
  PICKED_UP:          'badge--progress',
  IN_TRANSIT:         'badge--progress',
  DELIVERED:          'badge--delivered',
  CANCELLED:          'badge--pending',
  FAILED:             'badge--failed',
}[s] ?? 'badge--pending')

const statusLabel = (s: string) =>
  s.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())

const fmt = (n: number) => `${Number(n).toLocaleString('fr-CM')} XAF`
</script>

<template>
  <div class="dod-card">
    <h2 class="dod-title">{{ $t('driver.dod.title') }}</h2>

    <div v-if="loading" class="dod-state">Loading…</div>
    <div v-else-if="error" class="dod-state dod-state--error">Failed to load</div>
    <div v-else-if="deliveries.length === 0" class="dod-state">No deliveries assigned</div>

    <template v-else>
      <div class="dod-list">
        <div v-for="d in paged" :key="d.deliveryId" class="dod-item">
          <div class="dod-row">
            <div class="dod-col">
              <p class="dod-label">{{ $t('driver.dod.pickup') }}</p>
              <p class="dod-value">{{ d.pickupLocationAddress }}</p>
            </div>
            <div class="dod-col">
              <p class="dod-label">{{ $t('driver.dod.dropoff') }}</p>
              <p class="dod-value">{{ d.receiverAddress }}</p>
            </div>
            <div class="dod-status-col">
              <span class="dod-badge" :class="statusClass(d.status)">
                {{ statusLabel(d.status) }}
              </span>
            </div>
          </div>
          <div class="dod-foot">
            <span class="dod-distance">
              <UIcon name="i-heroicons-user" class="dod-pin" />
              {{ d.receiverName || '—' }}
            </span>
            <span class="dod-price">{{ fmt(d.price) }}</span>
          </div>
        </div>
      </div>

      <div class="dod-pagination">
        <button class="pg-arrow" @click="page = Math.max(1, page - 1)">
          <UIcon name="i-heroicons-arrow-up" class="pg-arrow__icon" />
        </button>
        <button
          v-for="p in totalPages" :key="p"
          class="pg-btn" :class="{ 'pg-btn--active': page === p }"
          @click="page = p"
        >{{ String(p).padStart(2, '0') }}</button>
        <button class="pg-arrow" @click="page = Math.min(totalPages, page + 1)">
          <UIcon name="i-heroicons-arrow-down" class="pg-arrow__icon" />
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dod-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.dod-title { font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0 0 1rem; }
.dod-state { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.dod-state--error { color: #DC2626; }
.dod-list { display: flex; flex-direction: column; gap: 0.75rem; }
.dod-item { border: 1px solid var(--color-border); border-radius: 0.625rem; padding: 0.875rem 1rem; }
.dod-row { display: flex; align-items: flex-start; gap: 1rem; margin-bottom: 0.5rem; }
.dod-col { flex: 1; min-width: 0; }
.dod-label { font-size: 0.6875rem; color: var(--color-text-muted); margin: 0 0 0.125rem; }
.dod-value { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dod-status-col { display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; flex-shrink: 0; }
.dod-badge { font-size: 0.625rem; font-weight: 600; padding: 0.15rem 0.5rem; border-radius: 999px; white-space: nowrap; }
.badge--progress  { background: #DBEAFE; color: #1D4ED8; }
.badge--pending   { background: #F3F4F6; color: #6B7280; }
.badge--delivered { background: #DCFCE7; color: #16A34A; }
.badge--failed    { background: #FEE2E2; color: #DC2626; }
.dod-time { font-size: 0.6875rem; color: var(--color-text-muted); }
.dod-foot { display: flex; align-items: center; justify-content: space-between; }
.dod-distance { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--color-text-muted); }
.dod-pin { width: 0.875rem; height: 0.875rem; color: var(--color-text-muted); flex-shrink: 0; }
.dod-price { font-size: 0.8125rem; font-weight: 700; color: #EA580C; }
.dod-pagination { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1rem; }
.pg-btn { width: 1.75rem; height: 1.75rem; border-radius: 50%; border: none; font-size: 0.6875rem; background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active { background: var(--color-page-active-bg); color: #fff; font-weight: 700; }
.pg-arrow { width: 1.75rem; height: 1.75rem; border-radius: 50%; border: 1px solid var(--color-primary); background: transparent; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.pg-arrow__icon { width: 0.875rem; height: 0.875rem; color: var(--color-primary); }
</style>
