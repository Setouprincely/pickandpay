<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import { Chart, BarElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
Chart.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const { $t } = useNuxtApp()
const { get } = useApi()

const loading = ref(true)
const error = ref(false)
const chartData = ref<any>({ labels: [], datasets: [] })

onMounted(async () => {
  try {
    const s = await get<any>('/api/stats/dashboard')

    chartData.value = {
      labels: [
        'Total Revenue',
        'Product Revenue',
        'Delivery Revenue',
        'Month Revenue',
        'Total Orders',
      ],
      datasets: [
        {
          label: $t('dashboard.ecommerce.total'),
          data: [
            s.totalRevenue ?? 0,
            s.productRevenue ?? 0,
            s.deliveryRevenue ?? 0,
            s.currentMonthRevenue ?? 0,
            s.totalOrders ?? 0,
          ],
          backgroundColor: ['#2563EB', '#22C55E', '#8B5CF6', '#F59E0B', '#14B8A6'],
          borderRadius: 4,
          barPercentage: 0.6,
        }
      ]
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
      callbacks: {
        label: (ctx: any) => {
          if (ctx.label === 'Total Orders') return ` ${Number(ctx.raw).toLocaleString()} orders`
          return ` ${Number(ctx.raw).toLocaleString('fr-CM')} XAF`
        }
      }
    },
  },
  scales: {
    x: { grid: { display: false }, ticks: { font: { size: 9 }, color: '#374151' } },
    y: { grid: { color: '#E5E7EB' }, ticks: { font: { size: 9 }, color: '#9CA3AF' } },
  },
}
</script>

<template>
  <div class="chart-card">
    <h3 class="chart-title">{{ $t('dashboard.ecommerce.title') }}</h3>
    <div class="chart-wrapper">
      <div v-if="loading" class="chart-state">Loading…</div>
      <div v-else-if="error" class="chart-state chart-state--error">Failed to load</div>
      <Bar v-else :data="chartData" :options="options" />
    </div>
  </div>
</template>

<style scoped>
.chart-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.chart-title { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); margin: 0 0 1rem; }
.chart-state { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 0.8rem; color: var(--color-text-secondary); }
.chart-state--error { color: #DC2626; }
.chart-wrapper { height: 200px; }
@media (min-width: 768px)  { .chart-wrapper { height: 240px; } }
@media (min-width: 1024px) { .chart-wrapper { height: 280px; } .chart-title { font-size: 0.875rem; } }
</style>