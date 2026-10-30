<script setup lang="ts">
import KpiCard from '~/components/dashboard/KpiCard.vue'

const { $t } = useNuxtApp()
const { format, compact } = useMoney()
const { load } = useSellerProducts()
const {
  loadAnalytics,
  revenueSeries, totalRevenue, totalOrders, liveProducts, totalUnitsSold, wallet,
  lowStock, topProducts,
} = useSellerAnalytics()

// Load real catalogue + analytics (wallet, seller orders) on mount.
onMounted(() => {
  load()
  loadAnalytics()
})

const kpis = computed(() => [
  { id: 'rev',   label: $t('seller.dashboard.kpi.revenue'),      value: compact(totalRevenue.value),    trend: wallet.value ? `Available ${compact(wallet.value.availableBalance ?? 0)} · pending ${compact(wallet.value.pendingBalance ?? 0)}` : '', trendType: 'stable' as const, icon: 'i-heroicons-banknotes',    iconColor: '--color-kpi-revenue' },
  { id: 'ord',   label: $t('seller.dashboard.kpi.orders'),       value: String(totalOrders.value),      trend: `Paid orders`, trendType: 'stable' as const, icon: 'i-heroicons-shopping-bag', iconColor: '--color-kpi-punctuality' },
  { id: 'live',  label: $t('seller.dashboard.kpi.liveProducts'), value: String(liveProducts.value),     trend: $t('seller.dashboard.kpi.stable'), trendType: 'stable' as const, icon: 'i-heroicons-cube',  iconColor: '--color-kpi-drivers' },
  { id: 'units', label: $t('seller.dashboard.kpi.unitsSold'),    value: String(totalUnitsSold.value),   trend: `From paid orders`, trendType: 'stable' as const, icon: 'i-heroicons-chart-bar',    iconColor: '--color-kpi-delivery' },
])

function restock(id: string | number) {
  navigateTo(`/products/${id}`)
}
</script>

<template>
  <div class="dash">
    <div class="kpi-grid">
      <KpiCard v-for="k in kpis" :key="k.id" v-bind="k" />
    </div>

    <SellerSalesChart
      :title="$t('seller.dashboard.salesTrend.title')"
      :series="revenueSeries"
      :format="(v:number) => format(v)"
    />

    <div class="bottom-grid">
      <!-- Low stock -->
      <section class="panel">
        <div class="panel__head">
          <h3 class="panel__title">{{ $t('seller.dashboard.lowStock.title') }}</h3>
          <UIcon name="i-lucide-triangle-alert" class="panel__head-icon" />
        </div>
        <div v-if="lowStock.length" class="list">
          <div v-for="p in lowStock" :key="p.id" class="list-row">
            <div class="list-row__img">
              <img v-if="p.image" :src="p.image" :alt="p.title" onerror="this.style.opacity='0.15'" />
            </div>
            <div class="list-row__meta">
              <span class="list-row__title">{{ p.title }}</span>
              <span class="list-row__sub" :class="{ 'is-out': p.stockCount === 0 }">
                {{ $t('seller.dashboard.lowStock.left', { n: p.stockCount }) }}
              </span>
            </div>
            <UButton size="xs" variant="soft" color="error" @click="restock(p.id)">
              {{ $t('seller.dashboard.lowStock.restock') }}
            </UButton>
          </div>
        </div>
        <p v-else class="panel__empty">{{ $t('seller.dashboard.lowStock.empty') }}</p>
      </section>

      <!-- Top sellers -->
      <section class="panel">
        <div class="panel__head">
          <h3 class="panel__title">{{ $t('seller.dashboard.topProducts.title') }}</h3>
          <UIcon name="i-lucide-trophy" class="panel__head-icon" />
        </div>
        <div v-if="topProducts.length" class="list">
          <div v-for="(p, i) in topProducts" :key="p.id" class="list-row">
            <span class="rank">{{ i + 1 }}</span>
            <div class="list-row__img">
              <img v-if="p.image" :src="p.image" :alt="p.title" onerror="this.style.opacity='0.15'" />
            </div>
            <div class="list-row__meta">
              <span class="list-row__title">{{ p.title }}</span>
              <span class="list-row__sub">{{ $t('seller.dashboard.topProducts.sold', { n: p.sold }) }}</span>
            </div>
            <span class="list-row__amount">{{ format(p.price * p.sold) }}</span>
          </div>
        </div>
        <p v-else class="panel__empty">{{ $t('seller.dashboard.topProducts.empty') }}</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; overflow-x: hidden; min-width: 0; }
.kpi-grid    { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.bottom-grid { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }

.panel { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.panel__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.panel__title { font-size: 0.85rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.panel__head-icon { width: 1.05rem; height: 1.05rem; color: var(--color-text-muted); }
.panel__empty { font-size: 0.825rem; color: var(--color-text-secondary); margin: 0.5rem 0; }

.list { display: flex; flex-direction: column; }
.list-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.6rem 0; border-bottom: 1px solid var(--color-border); }
.list-row:last-child { border-bottom: none; }
.rank { width: 1.25rem; font-size: 0.8rem; font-weight: 700; color: var(--color-text-muted); flex-shrink: 0; text-align: center; }
.list-row__img { width: 2.25rem; height: 2.25rem; border-radius: 0.5rem; overflow: hidden; flex-shrink: 0; background: var(--color-page-bg); }
.list-row__img img { width: 100%; height: 100%; object-fit: cover; }
.list-row__meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.1rem; }
.list-row__title { font-size: 0.8rem; font-weight: 600; color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.list-row__sub { font-size: 0.72rem; color: var(--color-text-secondary); }
.list-row__sub.is-out { color: #DC2626; font-weight: 600; }
.list-row__amount { font-size: 0.8rem; font-weight: 700; color: var(--color-text-primary); flex-shrink: 0; }

@media (min-width: 1024px) {
  .dash { padding: 1.5rem; gap: 1.25rem; }
  .kpi-grid    { grid-template-columns: repeat(4, 1fr); gap: 1rem; }
  .bottom-grid { grid-template-columns: 1fr 1fr; gap: 1rem; }
}
</style>
