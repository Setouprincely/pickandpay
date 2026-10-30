<script setup lang="ts">
// Receives a real OrderResponse from the reports page (no more hardcoded order).
type OrderItem = {
  productName?: string
  quantity?: number
  unite_price?: number
  total_price?: number
}
type OrderDTO = {
  orderId: string
  status: string
  customerName: string | null
  productTotal: number
  deliveryFee: number | null
  totalAmount: number
  deliveryAddress: string | null
  createdAt: string | null
  items?: OrderItem[]
  deliveryLegs?: any[]
}

const props = defineProps<{ order: OrderDTO | null }>()
defineEmits<{ close: [] }>()

const { $t } = useNuxtApp()

const fmt = (n: number | null | undefined) =>
  `${Number(n ?? 0).toLocaleString('fr-CM')} XAF`

const dateLabel = computed(() => {
  if (!props.order?.createdAt) return '—'
  return new Date(props.order.createdAt).toLocaleDateString('en-GB', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
})

const shortId = computed(() =>
  props.order?.orderId ? props.order.orderId.slice(0, 8).toUpperCase() : '—'
)

const statusClass = (s?: string) => {
  const v = (s ?? '').toUpperCase()
  if (['DELIVERED', 'COMPLETED'].includes(v)) return 'perf-badge--green'
  if (['CANCELLED', 'FAILED'].includes(v)) return 'perf-badge--red'
  return 'perf-badge--blue'
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">

        <!-- Header -->
        <div class="modal-header">
          <div>
            <h2 class="modal-title">{{ $t('reports.order.title') }}</h2>
            <p class="modal-sub">CMD-{{ shortId }} · {{ dateLabel }}</p>
          </div>
          <button class="modal-close" aria-label="Close" @click="$emit('close')">×</button>
        </div>

        <div v-if="!order" class="modal-empty">No order selected</div>

        <template v-else>
          <!-- Itinerary + Customer -->
          <div class="modal-grid">
            <div class="modal-section">
              <h3 class="modal-section-title">{{ $t('reports.order.itinerary') }}</h3>
              <div class="route-point">
                <span class="route-dot route-dot--green" />
                <div>
                  <p class="route-label">{{ $t('reports.order.arrival') }}</p>
                  <p class="route-addr">{{ order.deliveryAddress || '—' }}</p>
                </div>
              </div>
            </div>

            <div class="modal-section">
              <h3 class="modal-section-title">{{ $t('reports.order.driverInfo') }}</h3>
              <div class="driver-row">
                <span class="driver-name">{{ order.customerName || '—' }}</span>
              </div>
              <span class="perf-badge" :class="statusClass(order.status)">{{ order.status }}</span>
            </div>
          </div>

          <!-- Items -->
          <div class="modal-section modal-section--full">
            <h3 class="modal-section-title">Items</h3>
            <table class="fin-table">
              <tr v-for="(it, i) in (order.items || [])" :key="i">
                <td>{{ it.productName || 'Item' }} <span class="fin-qty">×{{ it.quantity ?? 1 }}</span></td>
                <td class="fin-val">{{ fmt(it.total_price ?? it.unite_price) }}</td>
              </tr>
              <tr v-if="!order.items || order.items.length === 0">
                <td colspan="2" class="fin-empty">No items</td>
              </tr>
            </table>
          </div>

          <!-- Financials -->
          <div class="modal-section modal-section--full">
            <h3 class="modal-section-title">{{ $t('reports.order.financialDetails') }}</h3>
            <table class="fin-table">
              <tr>
                <td>{{ $t('reports.order.baseRate') }}</td>
                <td class="fin-val">{{ fmt(order.productTotal) }}</td>
              </tr>
              <tr>
                <td>{{ $t('reports.order.deliveryMethod') }}</td>
                <td class="fin-val">{{ fmt(order.deliveryFee) }}</td>
              </tr>
              <tr class="fin-total">
                <td><strong>{{ $t('reports.order.total') }}</strong></td>
                <td class="fin-val"><strong>{{ fmt(order.totalAmount) }}</strong></td>
              </tr>
            </table>
          </div>
        </template>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 520px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }

.modal-header  { display: flex; justify-content: space-between; align-items: flex-start; padding: 1.25rem 1.25rem 0; }
.modal-title   { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.modal-sub     { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0.25rem 0 0; }
.modal-close   { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); line-height: 1; padding: 0.25rem; border-radius: 4px; }
.modal-close:hover { background: var(--color-page-bg); }
.modal-empty   { padding: 2.5rem 1.25rem; text-align: center; color: var(--color-text-secondary); font-size: 0.875rem; }

.modal-grid    { display: grid; grid-template-columns: 1fr 1fr; gap: 0; border-top: 1px solid var(--color-border); margin-top: 1rem; }
.modal-section { padding: 1rem 1.25rem; }
.modal-section--full { border-top: 1px solid var(--color-border); }
.modal-section + .modal-section { border-left: 1px solid var(--color-border); }
.modal-section-title { font-size: 0.8125rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 0.75rem; }

.route-point    { display: flex; align-items: flex-start; gap: 0.625rem; margin-bottom: 0.375rem; }
.route-dot      { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 3px; }
.route-dot--green { background: #22C55E; }
.route-label    { font-size: 0.75rem; font-weight: 600; color: var(--color-text-primary); margin: 0; }
.route-addr     { font-size: 0.75rem; color: var(--color-text-secondary); margin: 0.125rem 0 0; }

.driver-row     { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.driver-name    { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }

.fin-table  { width: 100%; border-collapse: collapse; }
.fin-table td { font-size: 0.8125rem; color: var(--color-text-primary); padding: 0.25rem 0; }
.fin-val    { text-align: right; font-weight: 500; }
.fin-qty    { color: var(--color-text-muted); font-size: 0.75rem; }
.fin-empty  { text-align: center; color: var(--color-text-muted); padding: 0.75rem 0; }
.fin-total  td { padding-top: 0.5rem; border-top: 1px solid var(--color-border); }

.perf-badge        { display: inline-block; font-size: 0.75rem; font-weight: 600; padding: 0.375rem 0.75rem; border-radius: 0.375rem; line-height: 1.4; }
.perf-badge--green { background: #DCFCE7; color: #16A34A; }
.perf-badge--blue  { background: #DBEAFE; color: #1D4ED8; }
.perf-badge--red   { background: #FEE2E2; color: #DC2626; }

@media (max-width: 540px) {
  .modal-grid { grid-template-columns: 1fr; }
  .modal-section + .modal-section { border-left: none; border-top: 1px solid var(--color-border); }
}
</style>
