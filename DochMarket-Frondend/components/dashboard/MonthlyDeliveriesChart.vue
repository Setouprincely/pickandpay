<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import { Chart, BarElement, CategoryScale, LinearScale, Tooltip } from 'chart.js'
Chart.register(BarElement, CategoryScale, LinearScale, Tooltip)

const { $t } = useNuxtApp()
const { get } = useApi()

type DailyRevenueDTO = { date: string; revenue: number }

const loading = ref(true)
const error = ref(false)
const chartData = ref<any>({ labels: [], datasets: [] })

onMounted(async () => {
  try {
    const raw = await get<DailyRevenueDTO[]>('/api/stats/revenue/daily')

    // Group by month
    const monthMap: Record<string, number> = {}
    for (const entry of raw) {
      const label = new Date(entry.date).toLocaleDateString('en-GB', { month: 'short', year: '2-digit' })
      monthMap[label] = (monthMap[label] ?? 0) + entry.revenue
    }

    if (Object.keys(monthMap).length === 0) {
      // No data yet — show empty state, not hardcoded data
      chartData.value = { labels: [], datasets: [] }
    } else {
      chartData.value = {
        labels: Object.keys(monthMap),
        datasets: [
          {
            label: $t('dashboard.monthly.deliveries'),
            data: Object.values(monthMap),
            backgroundColor: '#2563EB',
            stack: 's',
            borderRadius: 3,
          }
        ]
      }
    }
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
})

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      mode: 'index' as const,
      callbacks: {
        label: (ctx: any) => ` ${Number(ctx.raw).toLocaleString('fr-CM')} XAF`
      }
    },
  },
  scales: {
    x: { stacked: true, grid: { display: false }, ticks: { color: '#9CA3AF', font: { size: 9 } } },
    y: { stacked: true, display: false },
  },
}
</script>

<template>
  <div class="chart-card">
    <div class="chart-card__header">
      <span class="chart-title">{{ $t('dashboard.monthly.title') }}</span>
      <div class="chart-legend">
        <span class="legend-dot" style="background:#2563EB" /> {{ $t('dashboard.monthly.deliveries') }}
      </div>
    </div>
    <div class="chart-wrapper">
      <div v-if="loading" class="chart-state">Loading…</div>
      <div v-else-if="error" class="chart-state chart-state--error">Failed to load</div>
      <div v-else-if="!chartData.labels.length" class="chart-state">No revenue data yet</div>
      <Bar v-else :data="chartData" :options="options" />
    </div>
  </div>
</template>

<style scoped>
.chart-card         { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.chart-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.chart-title        { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }
.chart-legend       { display: flex; align-items: center; font-size: 0.75rem; color: var(--color-text-secondary); gap: 0.375rem; }
.legend-dot         { display: inline-block; width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.chart-state        { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 0.8rem; color: var(--color-text-secondary); }
.chart-state--error { color: #DC2626; }
.chart-wrapper { height: 200px; }
@media (min-width: 768px)  { .chart-wrapper { height: 240px; } }
@media (min-width: 1024px) { .chart-wrapper { height: 280px; } .chart-title { font-size: 0.875rem; } }
</style>