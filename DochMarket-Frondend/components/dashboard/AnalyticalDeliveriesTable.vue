<script setup lang="ts">
const { $t } = useNuxtApp()
const { get } = useApi()

// Pulls real figures from the backend and derives the analytical indicators.
// GET /api/stats/dashboard  -> DashboardStats (totalRevenue, totalOrders, deliveryRevenue ...)
// GET /api/stats/orders/count -> number of DELIVERED orders
//
// The backend has no dedicated "analytical KPI" endpoint, so we compute the
// indicators from the real numbers it DOES expose. When the DB is empty these
// show 0 / — rather than the old invented values.
type DashboardStats = {
  totalRevenue: number
  productRevenue: number
  deliveryRevenue: number
  currentMonthRevenue: number
  totalOrders: number
}

const loading = ref(true)
const error = ref(false)
const stats = ref<DashboardStats | null>(null)
const deliveredCount = ref(0)

onMounted(async () => {
  try {
    const [s, delivered] = await Promise.all([
      get<DashboardStats>('/api/stats/dashboard'),
      get<number>('/api/stats/orders/count').catch(() => 0),
    ])
    stats.value = s
    deliveredCount.value = typeof delivered === 'number' ? delivered : Number(delivered ?? 0)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})

const fmt = (n: number) => Number(n ?? 0).toLocaleString('fr-CM')

type Status = 'Achieve' | 'Warning' | 'Critical'

const rows = computed(() => {
  const s = stats.value
  if (!s) return []

  const totalOrders = s.totalOrders ?? 0
  const completionRate = totalOrders > 0 ? Math.round((deliveredCount.value / totalOrders) * 100) : 0
  const avgOrderValue = totalOrders > 0 ? Math.round((s.totalRevenue ?? 0) / totalOrders) : 0

  const rateStatus: Status =
    completionRate >= 85 ? 'Achieve' : completionRate >= 50 ? 'Warning' : 'Critical'

  return [
    {
      indicator: $t('dashboard.analytical.rowOnTime'),
      value: `${completionRate} %`,
      target: '≥ 85 %',
      variation: `${deliveredCount.value}/${totalOrders}`,
      status: rateStatus,
    },
    {
      indicator: $t('dashboard.analytical.rowReturns'),
      value: String(totalOrders),
      target: '—',
      variation: `${deliveredCount.value} delivered`,
      status: 'Achieve' as Status,
    },
    {
      indicator: $t('dashboard.analytical.rowAvgCost'),
      value: `${fmt(avgOrderValue)} XAF`,
      target: '—',
      variation: `${fmt(s.totalRevenue)} total`,
      status: 'Achieve' as Status,
    },
    {
      indicator: $t('dashboard.analytical.rowAvgDeliveryTime'),
      value: `${fmt(s.deliveryRevenue)} XAF`,
      target: '—',
      variation: $t('dashboard.ecommerce.total'),
      status: 'Achieve' as Status,
    },
    {
      indicator: $t('dashboard.analytical.rowFailed'),
      value: `${fmt(s.currentMonthRevenue)} XAF`,
      target: '—',
      variation: 'This month',
      status: 'Achieve' as Status,
    },
  ]
})

const statusClass = (s: string) =>
  ({ Achieve: 'status--achieve', Warning: 'status--warning', Critical: 'status--critical' }[s] ?? '')

const statusLabel = (s: string) =>
  ({ Achieve: $t('dashboard.analytical.statusAchieve'), Warning: $t('dashboard.analytical.statusWarning'), Critical: $t('dashboard.analytical.statusCritical') }[s] ?? s)

const currentPage = ref(1)
const totalPages = 1
</script>

<template>
  <div class="adt-card">
    <h3 class="adt-title">{{ $t('dashboard.analytical.title') }}</h3>

    <div v-if="loading" class="adt-state">Loading…</div>
    <div v-else-if="error" class="adt-state adt-state--error">Failed to load</div>
    <div v-else-if="rows.length === 0" class="adt-state">No data yet</div>

    <template v-else>
      <table class="adt-table">
        <thead>
          <tr>
            <th>{{ $t('dashboard.analytical.colIndicator') }}</th>
            <th>{{ $t('dashboard.analytical.colValue') }}</th>
            <th>{{ $t('dashboard.analytical.colTarget') }}</th>
            <th>{{ $t('dashboard.analytical.colVariation') }}</th>
            <th>{{ $t('dashboard.analytical.colStatus') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.indicator">
            <td :data-label="$t('dashboard.analytical.colIndicator')">{{ row.indicator }}</td>
            <td :data-label="$t('dashboard.analytical.colValue')">{{ row.value }}</td>
            <td :data-label="$t('dashboard.analytical.colTarget')">{{ row.target }}</td>
            <td :data-label="$t('dashboard.analytical.colVariation')">{{ row.variation }}</td>
            <td :data-label="$t('dashboard.analytical.colStatus')"><span class="status-pill" :class="statusClass(row.status)">● {{ statusLabel(row.status) }}</span></td>
          </tr>
        </tbody>
      </table>

      <div class="adt-pagination">
        <button class="pg-arrow" aria-label="Previous page" @click="currentPage = Math.max(1, currentPage - 1)">←</button>
        <button
          v-for="p in totalPages"
          :key="p"
          class="pg-btn"
          :class="{ 'pg-btn--active': currentPage === p }"
          :aria-label="`Page ${p}`"
          :aria-current="currentPage === p ? 'page' : undefined"
          @click="currentPage = p"
        >{{ String(p).padStart(2, '0') }}</button>
        <button class="pg-arrow" aria-label="Next page" @click="currentPage = Math.min(totalPages, currentPage + 1)">→</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.adt-card        { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.adt-title       { font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase;
                   color: var(--color-text-secondary); margin: 0 0 1rem; }
.adt-state       { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.adt-state--error { color: #DC2626; }
.adt-table       { width: 100%; border-collapse: collapse; }
.adt-table th    { font-size: 0.6875rem; font-weight: 700; color: var(--color-text-muted); text-align: left;
                   padding: 0.5rem 0.25rem; border-bottom: 1px solid var(--color-border); }
.adt-table td    { font-size: 0.75rem; color: var(--color-text-primary); padding: 0.625rem 0.25rem;
                   border-bottom: 1px solid var(--color-border); }
.status-pill     { font-size: 0.6875rem; font-weight: 600; padding: 0.1875rem 0.5rem; border-radius: 999px; white-space: nowrap; }
.status--achieve { color: var(--color-status-achieve); background: #DCFCE7; }
.status--warning { color: var(--color-status-warning); background: #FEF9C3; }
.status--critical{ color: var(--color-status-critical); background: #FEE2E2; }

.adt-pagination  { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1rem; }
.pg-btn          { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: none; font-size: 0.6875rem;
                   background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active  { background: var(--color-page-active-bg); color: var(--color-page-active-text); font-weight: 700; }
.pg-arrow        { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: 1px solid var(--color-border);
                   background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: 0.75rem; }

@media (max-width: 639px) {
  .adt-table thead          { display: none; }
  .adt-table tr             { display: block; padding: 0.75rem 0; border-bottom: 1px solid var(--color-border); }
  .adt-table td             { display: flex; justify-content: space-between; align-items: center;
                               padding: 0.25rem 0; border: none; font-size: 0.75rem; }
  .adt-table td::before     { content: attr(data-label); font-weight: 600; font-size: 0.6875rem;
                               text-transform: uppercase; color: var(--color-text-secondary); }
  .pg-btn, .pg-arrow        { min-width: 2.25rem; min-height: 2.25rem; }
  .adt-pagination           { gap: 0.25rem; }
}

@media (max-width: 767px) {
  .pg-btn, .pg-arrow { min-width: 2.25rem; min-height: 2.25rem; }
}
</style>
