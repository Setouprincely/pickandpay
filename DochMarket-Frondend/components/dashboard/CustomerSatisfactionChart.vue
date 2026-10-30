<script setup lang="ts">
import { Line } from 'vue-chartjs'
import { Chart, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
Chart.register(LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend)

const { $t } = useNuxtApp()
const { get } = useApi()

const loading = ref(true)
const error = ref(false)

// Monthly averages of customer reviews (order rating and delivery rating), from /api/stats/dashboard.
const chartData = ref<any>({ labels: [], datasets: [] })
const empty = ref(false)

onMounted(async () => {
  try {
    const stats = await get<any>('/api/stats/dashboard')
    const rows: any[] = stats?.monthlyRatings ?? []
    empty.value = rows.length === 0
    const months = rows.map(r => new Date(`${r.month}-01T00:00:00`).toLocaleDateString('en-GB', { month: 'short', year: '2-digit' }))

    chartData.value = {
      labels: months,
      datasets: [
        {
          label: $t('dashboard.satisfaction.buyers'),
          data: rows.map(r => r.averageRating),
          borderColor: '#22C55E',
          backgroundColor: '#22C55E',
          tension: 0.4, pointRadius: 3, fill: false
        },
        {
          label: $t('dashboard.satisfaction.drivers'),
          data: rows.map(r => r.averageDeliveryRating),
          borderColor: '#F59E0B',
          backgroundColor: '#F59E0B',
          tension: 0.4, pointRadius: 3, fill: false
        },
      ],
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
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { display: false }, ticks: { font: { size: 10 }, color: '#9CA3AF' } },
    y: {
      min: 0, max: 5,
      ticks: {
        stepSize: 1, font: { size: 10 }, color: '#9CA3AF',
        callback: (v: any) => v > 0 ? '★'.repeat(Number(v)) : '0',
      },
    },
  },
}
</script>

<template>
  <div class="chart-card">
    <div class="chart-card__header">
      <span class="chart-title">{{ $t('dashboard.satisfaction.title') }}</span>
      <div class="sat-legend">
        <span class="legend-dot" style="background:#22C55E" />Order rating
        <span class="legend-dot" style="background:#F59E0B" />Delivery rating
      </div>
    </div>
    <div class="chart-wrapper">
      <div v-if="loading" class="chart-state">Loading…</div>
      <div v-else-if="error" class="chart-state chart-state--error">Failed to load</div>
      <div v-else-if="empty" class="chart-state">No customer reviews yet</div>
      <Line v-else :data="chartData" :options="options" />
    </div>
  </div>
</template>

<style scoped>
.chart-card         { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.chart-card__header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 0.75rem; }
.chart-title        { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); max-width: 55%; }
.sat-legend         { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--color-text-secondary); flex-wrap: wrap; }
.legend-dot         { display: inline-block; width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.chart-state        { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 0.8rem; color: var(--color-text-secondary); }
.chart-state--error { color: #DC2626; }
.chart-wrapper { height: 200px; }
@media (min-width: 768px)  { .chart-wrapper { height: 240px; } }
@media (min-width: 1024px) { .chart-wrapper { height: 280px; } .chart-title { font-size: 0.875rem; } }
</style>