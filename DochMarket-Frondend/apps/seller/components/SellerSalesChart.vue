<script setup lang="ts">
import { Line } from 'vue-chartjs'
import { Chart, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Filler } from 'chart.js'
import type { SeriesPoint } from '~/apps/seller/types'

Chart.register(LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Filler)

const props = withDefaults(defineProps<{
  title: string
  series: SeriesPoint[]
  color?: string
  /** Format a y value for the tooltip (e.g. money). */
  format?: (v: number) => string
}>(), {
  color: '#E8202A',
})

const data = computed(() => ({
  labels: props.series.map(p => p.label),
  datasets: [{
    data: props.series.map(p => p.value),
    borderColor: props.color,
    backgroundColor: (ctx: any) => {
      const { chart } = ctx
      const { ctx: c, chartArea } = chart
      if (!chartArea) return 'transparent'
      const g = c.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
      g.addColorStop(0, props.color + '33')
      g.addColorStop(1, props.color + '00')
      return g
    },
    fill: true,
    tension: 0.4,
    borderWidth: 2,
    pointRadius: 0,
    pointHoverRadius: 4,
    pointHoverBackgroundColor: props.color,
  }],
}))

const options = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (item: any) => (props.format ? props.format(item.parsed.y) : String(item.parsed.y)),
      },
    },
  },
  scales: {
    x: { grid: { display: false }, ticks: { color: '#9CA3AF', font: { size: 10 } } },
    y: {
      grid: { color: '#E5E7EB' },
      ticks: { color: '#9CA3AF', font: { size: 10 }, maxTicksLimit: 5 },
      beginAtZero: true,
    },
  },
}))
</script>

<template>
  <div class="chart-card">
    <div class="chart-card__header">
      <span class="chart-title">{{ title }}</span>
    </div>
    <div class="chart-wrapper">
      <Line :data="data" :options="options" />
    </div>
  </div>
</template>

<style scoped>
.chart-card         { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.chart-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.chart-title        { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }
.chart-wrapper      { height: 220px; }
@media (min-width: 768px)  { .chart-wrapper { height: 260px; } }
@media (min-width: 1024px) { .chart-wrapper { height: 280px; } .chart-title { font-size: 0.875rem; } }
</style>
