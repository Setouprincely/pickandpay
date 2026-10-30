<script setup lang="ts">
import KpiCard from '~/apps/driver/components/KpiCard.vue'
import AvailabilityToggle from '~/apps/driver/components/AvailabilityToggle.vue'
import DeliveryMap from '~/apps/driver/components/DeliveryMap.vue'
import DeliveriesOfDay from '~/apps/driver/components/DeliveriesOfDay.vue'
import WeeklyPerformance from '~/apps/driver/components/WeeklyPerformance.vue'
import WeeklyEarnings from '~/apps/driver/components/WeeklyEarnings.vue'

const { $t } = useNuxtApp()
const { get } = useDriverApi()

// REAL DriverPerformanceDTO (confirmed from backend source):
//   completedToday: Integer
//   completedThisWeek: Integer
//   completedThisMonth: Integer
//   recentTaskHistory: [{ deliveryId, completedAt, destination }]
// NOTE: the backend exposes NO distance, NO rating, NO earnings here — the old
// KPI cards for "distance traveled" and "average rating" were invented and have
// been removed. We show the three real counts the API actually returns.
type RecentTask = {
  deliveryId: string
  completedAt: string
  destination: string
}
type DriverPerformance = {
  completedToday: number
  completedThisWeek: number
  completedThisMonth: number
  recentTaskHistory: RecentTask[]
}

const performance = ref<DriverPerformance | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    performance.value = await get<DriverPerformance>('/api/drivers/me/performance')
  } catch (e: any) {
    error.value = `Failed to load (${e?.status ?? 'network error'})`
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <AvailabilityToggle />
    <div v-if="loading" class="dash-state">
      <span class="dash-spinner" /> Loading…
    </div>
    <div v-else-if="error" class="dash-state dash-state--error">⚠️ {{ error }}</div>

    <template v-else>
      <div class="kpi-grid">
        <KpiCard
          icon="i-heroicons-cube"
          icon-tint="blue"
          :label="$t('driver.kpi.deliveriesToday')"
          :value="String(performance?.completedToday ?? 0)"
          :info="$t('driver.kpi.deliveriesTodayInfo')"
        />
        <KpiCard
          icon="i-heroicons-calendar-days"
          icon-tint="blue"
          :label="$t('driver.kpi.completedThisWeek')"
          :value="String(performance?.completedThisWeek ?? 0)"
          :info="$t('driver.kpi.completedThisWeekInfo')"
        />
        <KpiCard
          icon="i-heroicons-chart-bar"
          icon-tint="amber"
          :label="$t('driver.kpi.completedThisMonth')"
          :value="String(performance?.completedThisMonth ?? 0)"
          :info="$t('driver.kpi.completedThisMonthInfo')"
        />
      </div>

      <DeliveryMap />

      <div class="two-col">
        <DeliveriesOfDay />
        <WeeklyPerformance :performance="performance" />
      </div>

      <WeeklyEarnings />
    </template>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex; flex-direction: column;
  gap: 1rem; padding: 1rem;
  overflow-x: hidden; min-width: 0;
}
.kpi-grid { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.two-col  { display: grid; grid-template-columns: 1fr; gap: 1rem; }
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
@media (min-width: 640px) { .kpi-grid { grid-template-columns: repeat(3, 1fr); } }
@media (min-width: 1024px) {
  .dashboard { padding: 1.5rem; gap: 1.25rem; }
  .two-col   { grid-template-columns: 1fr 1fr; }
}
</style>
