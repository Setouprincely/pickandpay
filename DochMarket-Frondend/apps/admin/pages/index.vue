<script setup lang="ts">
import KpiCard from '~/components/dashboard/KpiCard.vue'
import MonthlyDeliveriesChart from '~/components/dashboard/MonthlyDeliveriesChart.vue'
import EcommercePerformanceChart from '~/components/dashboard/EcommercePerformanceChart.vue'
import CustomerSatisfactionChart from '~/components/dashboard/CustomerSatisfactionChart.vue'
import AnalyticalDeliveriesTable from '~/components/dashboard/AnalyticalDeliveriesTable.vue'
import RealTimeAlerts from '~/components/dashboard/RealTimeAlerts.vue'
import { useApi } from '../composables/useApi'

const { $t } = useNuxtApp()
const { get } = useApi()

const loading = ref(true)
const error = ref('')
const stats = ref<any>(null)

onMounted(async () => {
  try {
    stats.value = await get('/api/stats/dashboard')
  } catch (e: any) {
    error.value = `Failed to load (${e?.status ?? 'network error'})`
  } finally {
    loading.value = false
  }
})

const fmt = (n: number) => Number(n ?? 0).toLocaleString('fr-CM')
const count = (map: Record<string, number> | undefined, ...keys: string[]) =>
  keys.reduce((sum, k) => sum + (map?.[k] ?? 0), 0)

// Every figure below is computed by the backend from the shared database (no trend estimates).
const kpis = computed(() => {
  const s = stats.value
  if (!s) return []
  const pk = s.packagesByStatus ?? {}
  return [
    { id: 1, label: 'Paid orders', value: String(s.paidOrders ?? 0),
      trend: `${s.totalOrders ?? 0} delivered`, trendType: 'stable' as const, icon: 'i-heroicons-cube', iconColor: '--color-kpi-delivery' },
    { id: 2, label: 'Payments collected', value: `${fmt(s.paymentsCollected)} XAF`,
      trend: `${fmt(s.currentMonthRevenue)} XAF delivered this month`, trendType: 'stable' as const, icon: 'i-heroicons-banknotes', iconColor: '--color-kpi-revenue' },
    { id: 3, label: 'Packages awaiting dispatch', value: String(count(pk, 'PENDING_ASSIGNMENT', 'AT_HUB')),
      trend: `${count(pk, 'PICKED_UP', 'IN_TRANSIT')} on the road · ${count(pk, 'FAILED')} exceptions`, trendType: 'stable' as const, icon: 'i-heroicons-truck', iconColor: '--color-kpi-punctuality' },
    { id: 4, label: $t('dashboard.kpi.activeDrivers'), value: `${s.driversOnline ?? 0} / ${s.driversTotal ?? 0}`,
      trend: 'online / total', trendType: 'stable' as const, icon: 'i-heroicons-user-group', iconColor: '--color-kpi-drivers' },
  ]
})

const todo = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: 'Products awaiting review', value: s.pendingProductReviews ?? 0, to: '/products' },
    { label: 'Sellers awaiting approval', value: s.pendingSellerApprovals ?? 0, to: '/vendor-agreement' },
    { label: 'Packages ready to assign', value: count(s.packagesByStatus, 'PENDING_ASSIGNMENT', 'AT_HUB'), to: '/dispatch' },
    { label: 'Delivery exceptions', value: count(s.packagesByStatus, 'FAILED'), to: '/dispatch' },
    { label: 'Orders with failed payment', value: count(s.ordersByStatus, 'PAYMENT_FAILED'), to: '/orders' },
  ]
})
</script>

<template>
  <div class="dashboard-content">
    <div v-if="loading" class="dash-state">
      <span class="dash-spinner" />
      Loading…
    </div>
    <div v-else-if="error" class="dash-state dash-state--error">{{ error }}</div>

    <template v-else>
      <div class="kpi-grid">
        <KpiCard v-for="kpi in kpis" :key="kpi.id" v-bind="kpi" />
      </div>
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-3">
        <NuxtLink v-for="t in todo" :key="t.label" :to="t.to" class="bg-white border rounded-xl p-3 hover:border-red-400">
          <div class="text-2xl font-bold" :class="t.value ? 'text-red-600' : 'text-gray-400'">{{ t.value }}</div>
          <div class="text-xs text-gray-600">{{ t.label }}</div>
        </NuxtLink>
      </div>
      <MonthlyDeliveriesChart />
      <EcommercePerformanceChart />
      <div class="bottom-grid">
        <CustomerSatisfactionChart />
        <AnalyticalDeliveriesTable />
      </div>
      <RealTimeAlerts />
    </template>
  </div>
</template>

<style scoped>
.dashboard-content {
  display: flex; flex-direction: column;
  gap: 1rem; padding: 1rem;
  overflow-x: hidden; min-width: 0;
}
.kpi-grid    { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.bottom-grid { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.dash-state {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 0.75rem; padding: 3rem 1rem;
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: 0.75rem; color: var(--color-text-secondary); font-size: 0.875rem;
}
.dash-state--error { border-color: #FCA5A5; background: #FFF1F2; color: #DC2626; }
.dash-spinner {
  width: 1.5rem; height: 1.5rem;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
@media (min-width: 1024px) {
  .dashboard-content { padding: 1.5rem; gap: 1.25rem; }
  .kpi-grid { grid-template-columns: repeat(4, 1fr); gap: 1rem; }
  .bottom-grid { grid-template-columns: 1fr 1fr; gap: 1rem; }
}
</style>
