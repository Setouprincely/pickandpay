<script setup lang="ts">
// Receives real DriverProfileDTO[] from the reports page (no more hardcoded drivers).
type DriverDTO = {
  driverId: string
  fullName: string
  rating: number | null
  totalDeliveries: number | null
  vehicleType?: string | null
  plateNumber?: string | null
  available?: boolean
  isAvailable?: boolean
}

const props = withDefaults(defineProps<{ drivers?: DriverDTO[] }>(), {
  drivers: () => [],
})

defineEmits<{ 'row-click': [driver: DriverDTO] }>()

const { $t } = useNuxtApp()

const isOnline = (d: DriverDTO) => d.available ?? d.isAvailable ?? false
const punctuality = (d: DriverDTO) => Number(((d.rating ?? 0) * 20).toFixed(1))
const punctClass = (v: number) => (v >= 95 ? 'punct--high' : v >= 80 ? 'punct--mid' : 'punct--low')
const filledStars = (r: number) => '★'.repeat(Math.round(r)) + '☆'.repeat(5 - Math.round(r))

const perPage = 6
const page = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(props.drivers.length / perPage)))
const paged = computed(() =>
  props.drivers.slice((page.value - 1) * perPage, page.value * perPage)
)
</script>

<template>
  <div class="dr-card">
    <h2 class="dr-title">{{ $t('reports.drivers.title') }}</h2>

    <div v-if="drivers.length === 0" class="dr-state">No drivers registered yet</div>

    <template v-else>
      <table class="dr-table">
        <thead>
          <tr>
            <th>{{ $t('reports.drivers.colDriver') }}</th>
            <th>{{ $t('reports.drivers.colRating') }}</th>
            <th>{{ $t('reports.drivers.colDeliveries') }}</th>
            <th>{{ $t('reports.drivers.colPunctuality') }}</th>
            <th>{{ $t('reports.drivers.colStatus') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in paged" :key="row.driverId" class="dr-row" @click="$emit('row-click', row)">
            <td class="dr-name">{{ row.fullName }}</td>
            <td>
              <span class="dr-rating-num">{{ (row.rating ?? 0).toFixed(1) }}</span>
              <span class="dr-stars">{{ filledStars(row.rating ?? 0) }}</span>
            </td>
            <td>{{ row.totalDeliveries ?? 0 }}</td>
            <td><span class="dr-punct" :class="punctClass(punctuality(row))">{{ punctuality(row) }}%</span></td>
            <td>
              <span v-if="isOnline(row)" class="dr-status dr-status--actif">● {{ $t('driverStatus.actif') }}</span>
              <span v-else class="dr-status dr-status--suspendu">● {{ $t('driverStatus.suspendu') }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="dr-pagination">
        <button class="pg-arrow" @click="page = Math.max(1, page - 1)">←</button>
        <button v-for="p in totalPages" :key="p" class="pg-btn" :class="{ 'pg-btn--active': page === p }" @click="page = p">
          {{ String(p).padStart(2, '0') }}
        </button>
        <button class="pg-arrow" @click="page = Math.min(totalPages, page + 1)">→</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dr-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.dr-title { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0 0 1rem; }
.dr-state { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.dr-table    { width: 100%; border-collapse: collapse; }
.dr-table th { font-size: 0.625rem; font-weight: 700; color: var(--color-text-muted); text-align: left; padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--color-border); }
.dr-table td { font-size: 0.8125rem; color: var(--color-text-primary); padding: 0.875rem 0.75rem; border-bottom: 1px solid var(--color-border); }
.dr-row      { cursor: pointer; transition: background 0.15s; }
.dr-row:hover{ background: #F9FAFB; }
.dr-name     { font-weight: 500; }
.dr-rating-num { font-weight: 600; margin-right: 0.375rem; }
.dr-stars      { color: #F59E0B; font-size: 0.875rem; letter-spacing: 0.05em; }
.dr-punct      { font-size: 0.8125rem; font-weight: 600; padding: 0.2rem 0.5rem; border-radius: 4px; }
.punct--high   { background: #DCFCE7; color: #16A34A; }
.punct--mid    { background: #FEF9C3; color: #B45309; }
.punct--low    { background: #FEE2E2; color: #DC2626; }
.dr-status           { font-size: 0.8125rem; font-weight: 500; }
.dr-status--actif    { color: #16A34A; }
.dr-status--suspendu { background: #FEE2E2; color: #DC2626; padding: 0.2rem 0.6rem; border-radius: 999px; }
.dr-pagination { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1rem; }
.pg-btn        { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: none; font-size: 0.6875rem; background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active{ background: var(--color-page-active-bg); color: #fff; font-weight: 700; }
.pg-arrow      { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: 1px solid var(--color-border); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: 0.75rem; }
</style>
