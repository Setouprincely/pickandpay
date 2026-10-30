<script setup lang="ts">
const { $t } = useNuxtApp()
const { get } = useDriverApi()

// Real history is derived from the SAME source as the dashboard:
// GET /api/drivers/me/deliveries — then filtered client-side by status + period.
// No separate hardcoded arrays. "Completed" history = DELIVERED; we also surface
// CANCELLED and FAILED.
type Delivery = {
  deliveryId: string
  status: string
  createdAt: string | null
  receiverAddress: string
  price: number
}

const all = ref<Delivery[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const res = await get<any[]>('/api/drivers/me/history')
    all.value = (res ?? []).map((d: any) => ({
      deliveryId: d.deliveryId,
      status: d.status ?? 'PENDING_ASSIGNMENT',
      createdAt: d.deliveredAt ?? d.createdAt ?? null,
      receiverAddress: d.receiverAddress ?? '—',
      price: d.price ?? 0,
    }))
  } catch (e: any) {
    if (e?.status === 403 || e?.status === 500) {
      error.value = `Could not load history (${e.status}). If this is AuthorizationDeniedException with a valid driver token, it's the backend auth bug, not the frontend.`
    } else {
      error.value = `Failed to load history (${e?.status ?? 'network error'})`
    }
  } finally {
    loading.value = false
  }
})

const period = ref<'today' | 'week' | 'month'>('month')
const periods = computed(() => [
  { key: 'today', label: $t('driver.history.today') },
  { key: 'week',  label: $t('driver.history.thisWeek') },
  { key: 'month', label: $t('driver.history.thisMonth') },
] as const)

// Status filter mapped to real enum values
const statusFilter = ref<'all' | 'DELIVERED' | 'CANCELLED' | 'RETURNED'>('all')
const statusFilters = computed(() => [
  { key: 'all',       label: $t('driver.history.all') },
  { key: 'DELIVERED', label: $t('driver.history.delivered') },
  { key: 'CANCELLED', label: $t('driver.history.cancelled') },
  { key: 'RETURNED',  label: 'Returned' },
] as const)

function withinPeriod(iso: string | null): boolean {
  if (!iso) return true
  const d = new Date(iso).getTime()
  const now = Date.now()
  const day = 86_400_000
  if (period.value === 'today') return now - d < day
  if (period.value === 'week') return now - d < 7 * day
  return now - d < 31 * day
}

const filtered = computed(() =>
  all.value.filter(o => {
    if (statusFilter.value !== 'all' && o.status !== statusFilter.value) return false
    if (!withinPeriod(o.createdAt)) return false
    return true
  })
)

const statusClass = (s: string) => ({
  DELIVERED: 'status--delivered',
  CANCELLED: 'status--cancelled',
  FAILED:    'status--late',
}[s] ?? 'status--late')

const statusLabel = (s: string) =>
  s.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())

const shortId = (id: string) => id ? id.slice(0, 8).toUpperCase() : '—'
const dateLabel = (iso: string | null) =>
  iso ? new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'
const fmt = (n: number) => `${Number(n).toLocaleString('fr-CM')} XAF`

// Client-side pagination over the real filtered list
const perPage = 10
const page = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / perPage)))
const paged = computed(() => filtered.value.slice((page.value - 1) * perPage, page.value * perPage))
watch([filtered], () => { page.value = 1 })
</script>

<template>
  <div class="history-page">

    <div class="filter-row">
      <button
        v-for="p in periods" :key="p.key"
        class="pill pill--red"
        :class="{ 'pill--red-on': period === p.key }"
        @click="period = p.key"
      >{{ p.label }}</button>
    </div>

    <div class="filter-row">
      <button
        v-for="s in statusFilters" :key="s.key"
        class="pill pill--blue"
        :class="{ 'pill--blue-on': statusFilter === s.key }"
        @click="statusFilter = s.key"
      >{{ s.label }}</button>
    </div>

    <div v-if="loading" class="empty-card">Loading history…</div>
    <div v-else-if="error" class="empty-card empty-card--error">⚠️ {{ error }}</div>

    <div v-else class="table-card">
      <div class="table-scroll">
        <table class="hist-table">
          <thead>
            <tr>
              <th>{{ $t('driver.history.colOrderId') }}</th>
              <th>{{ $t('driver.history.colStatus') }}</th>
              <th>{{ $t('driver.history.colDate') }}</th>
              <th>{{ $t('driver.history.colTotal') }}</th>
              <th>{{ $t('driver.history.colAction') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in paged" :key="o.deliveryId">
              <td class="td-id">{{ shortId(o.deliveryId) }}</td>
              <td><span class="status" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
              <td class="td-muted">{{ dateLabel(o.createdAt) }}</td>
              <td class="td-muted">{{ fmt(o.price) }}</td>
              <td>
                <button class="details-link">
                  {{ $t('driver.history.viewDetails') }}
                  <UIcon name="i-heroicons-arrow-right" class="details-link__icon" />
                </button>
              </td>
            </tr>
            <tr v-if="paged.length === 0">
              <td colspan="5" class="empty">{{ $t('driver.history.empty') }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="pg-arrow" aria-label="Previous" @click="page = Math.max(1, page - 1)">
          <UIcon name="i-heroicons-arrow-left" class="pg-arrow__icon" />
        </button>
        <button
          v-for="p in totalPages" :key="p"
          class="pg-btn" :class="{ 'pg-btn--active': page === p }"
          @click="page = p"
        >{{ String(p).padStart(2, '0') }}</button>
        <button class="pg-arrow" aria-label="Next" @click="page = Math.min(totalPages, page + 1)">
          <UIcon name="i-heroicons-arrow-right" class="pg-arrow__icon" />
        </button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.history-page { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; min-width: 0; }
.filter-row { display: flex; gap: 0.75rem; flex-wrap: wrap; }
.pill {
  padding: 0.5rem 1.25rem; border-radius: 999px; font-size: 0.875rem; font-weight: 500;
  border: 1px solid var(--color-border); background: #fff; color: var(--color-text-secondary);
  cursor: pointer; transition: all 0.15s;
}
.pill:hover { border-color: #C7CDD4; }
.pill--red-on  { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.pill--blue-on { background: #2563EB; color: #fff; border-color: #2563EB; }
.empty-card { padding: 2.5rem 1rem; text-align: center; font-size: 0.875rem; color: var(--color-text-secondary);
              background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; }
.empty-card--error { border-color: #FCA5A5; background: #FFF1F2; color: #DC2626; }
.table-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 0.5rem 1.25rem 1.25rem; min-width: 0; }
.table-scroll { overflow-x: auto; }
.hist-table  { width: 100%; border-collapse: collapse; min-width: 620px; }
.hist-table th { font-size: 0.6875rem; font-weight: 700; color: var(--color-text-muted); text-align: left;
                 padding: 0.875rem 0.75rem; border-bottom: 1px solid var(--color-border);
                 background: #F9FAFB; letter-spacing: 0.03em; }
.hist-table td { font-size: 0.8125rem; padding: 0.75rem; border-bottom: 1px solid var(--color-border); color: var(--color-text-primary); }
.hist-table tr:last-child td { border-bottom: none; }
.td-id    { font-weight: 600; }
.td-muted { color: var(--color-text-secondary); }
.status            { font-weight: 600; }
.status--delivered { color: #16A34A; }
.status--cancelled { color: #DC2626; }
.status--late      { color: #EA580C; }
.details-link {
  display: inline-flex; align-items: center; gap: 0.25rem;
  background: none; border: none; cursor: pointer;
  color: #2563EB; font-size: 0.8125rem; font-weight: 500; padding: 0;
}
.details-link:hover { text-decoration: underline; }
.details-link__icon { width: 0.875rem; height: 0.875rem; }
.empty { text-align: center; color: var(--color-text-muted); padding: 2rem 0; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1.25rem; }
.pg-btn        { width: 1.75rem; height: 1.75rem; border-radius: 50%; border: none; font-size: 0.6875rem;
                 background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active{ background: var(--color-page-active-bg); color: #fff; font-weight: 700; }
.pg-arrow       { width: 1.75rem; height: 1.75rem; border-radius: 50%; border: 1px solid var(--color-primary);
                  background: transparent; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.pg-arrow__icon { width: 0.875rem; height: 0.875rem; color: var(--color-primary); }
@media (min-width: 1024px) { .history-page { padding: 1.5rem; } }
</style>
