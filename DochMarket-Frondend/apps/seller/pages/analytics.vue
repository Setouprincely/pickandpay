<script setup lang="ts">
const { $t } = useNuxtApp()
const { format, compact } = useMoney()
const {
  revenueSeries, salesByCategory, topProducts,
  liveProducts, avgOrderValue, totalUnitsSold,
} = useSellerAnalytics()

const stats = computed(() => [
  { id: 'live',  label: $t('seller.analytics.activeProducts'), value: String(liveProducts.value),    icon: 'i-lucide-cube' },
  { id: 'aov',   label: $t('seller.analytics.avgOrder'),       value: compact(avgOrderValue.value),  icon: 'i-lucide-receipt' },
  { id: 'units', label: $t('seller.analytics.totalUnits'),     value: String(totalUnitsSold.value),  icon: 'i-lucide-package-check' },
])

const maxCatRevenue = computed(() =>
  Math.max(1, ...salesByCategory.value.map(c => c.revenue))
)

const CAT_COLORS = ['#E8202A', '#2563EB', '#16A34A', '#F59E0B', '#8B5CF6', '#14B8A6', '#EC4899']
</script>

<template>
  <div class="analytics">
    <div>
      <h1 class="analytics__title">{{ $t('seller.analytics.title') }}</h1>
      <p class="analytics__sub">{{ $t('seller.analytics.subtitle') }}</p>
    </div>

    <!-- quick stats -->
    <div class="stat-grid">
      <div v-for="s in stats" :key="s.id" class="stat">
        <UIcon :name="s.icon" class="stat__icon" />
        <div>
          <span class="stat__label">{{ s.label }}</span>
          <p class="stat__value">{{ s.value }}</p>
        </div>
      </div>
    </div>

    <SellerSalesChart
      :title="$t('seller.analytics.revenueTrend')"
      :series="revenueSeries"
      color="#2563EB"
      :format="(v:number) => format(v)"
    />

    <div class="two-col">
      <!-- Sales by category -->
      <section class="panel">
        <h3 class="panel__title">{{ $t('seller.analytics.salesByCategory') }}</h3>
        <div class="bars">
          <div v-for="(c, i) in salesByCategory" :key="c.category" class="bar-row">
            <div class="bar-row__head">
              <span class="bar-row__cat">{{ c.category }}</span>
              <span class="bar-row__val">{{ format(c.revenue) }}</span>
            </div>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ width: `${(c.revenue / maxCatRevenue) * 100}%`, background: CAT_COLORS[i % CAT_COLORS.length] }"
              />
            </div>
            <span class="bar-row__units">{{ c.units }} {{ $t('seller.analytics.units').toLowerCase() }}</span>
          </div>
        </div>
      </section>

      <!-- Top products -->
      <section class="panel">
        <h3 class="panel__title">{{ $t('seller.analytics.topProducts') }}</h3>
        <div class="list">
          <div v-for="(p, i) in topProducts" :key="p.id" class="list-row">
            <span class="rank">{{ i + 1 }}</span>
            <div class="list-row__img">
              <img v-if="p.image" :src="p.image" :alt="p.title" onerror="this.style.opacity='0.15'" />
            </div>
            <div class="list-row__meta">
              <span class="list-row__title">{{ p.title }}</span>
              <span class="list-row__sub">{{ p.sold }} {{ $t('seller.analytics.units').toLowerCase() }}</span>
            </div>
            <span class="list-row__amount">{{ format(p.price * p.sold) }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.analytics { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; min-width: 0; }
.analytics__title { font-size: 1.25rem; font-weight: 800; color: var(--color-text-primary); margin: 0; }
.analytics__sub { font-size: 0.825rem; color: var(--color-text-secondary); margin: 0.2rem 0 0; }

.stat-grid { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.stat {
  background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem;
  padding: 1rem 1.25rem; display: flex; align-items: center; gap: 0.9rem;
}
.stat__icon { width: 1.6rem; height: 1.6rem; color: var(--color-primary); flex-shrink: 0; }
.stat__label { font-size: 0.72rem; color: var(--color-text-secondary); }
.stat__value { font-size: 1.25rem; font-weight: 800; color: var(--color-text-primary); margin: 0.1rem 0 0; }

.two-col { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.panel { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.panel__title { font-size: 0.85rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 1.1rem; }

.bars { display: flex; flex-direction: column; gap: 0.9rem; }
.bar-row__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.3rem; }
.bar-row__cat { font-size: 0.8rem; font-weight: 600; color: var(--color-text-primary); }
.bar-row__val { font-size: 0.78rem; font-weight: 700; color: var(--color-text-primary); }
.bar-track { height: 8px; border-radius: 999px; background: var(--color-page-bg); overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.4s ease; }
.bar-row__units { font-size: 0.7rem; color: var(--color-text-muted); }

.list { display: flex; flex-direction: column; }
.list-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.6rem 0; border-bottom: 1px solid var(--color-border); }
.list-row:last-child { border-bottom: none; }
.rank { width: 1.25rem; font-size: 0.8rem; font-weight: 700; color: var(--color-text-muted); flex-shrink: 0; text-align: center; }
.list-row__img { width: 2.25rem; height: 2.25rem; border-radius: 0.5rem; overflow: hidden; flex-shrink: 0; background: var(--color-page-bg); }
.list-row__img img { width: 100%; height: 100%; object-fit: cover; }
.list-row__meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.1rem; }
.list-row__title { font-size: 0.8rem; font-weight: 600; color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.list-row__sub { font-size: 0.72rem; color: var(--color-text-secondary); }
.list-row__amount { font-size: 0.8rem; font-weight: 700; color: var(--color-text-primary); flex-shrink: 0; }

@media (min-width: 768px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (min-width: 1024px) {
  .analytics { padding: 1.5rem; gap: 1.25rem; }
  .two-col { grid-template-columns: 1fr 1fr; gap: 1rem; }
}
</style>
