<script setup lang="ts">
// Now driven by the REAL DriverPerformanceDTO passed down from index.vue.
// The backend exposes ONLY completion counts (today/week/month) + recent tasks.
// There is NO on-time rate, NO customer-satisfaction score, NO success ratio in
// the API — those invented metrics are removed. We show the three real counts
// plus the recent task history the endpoint actually returns.
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

const props = defineProps<{ performance: DriverPerformance | null }>()
const { $t } = useNuxtApp()

// Bars are the real counts. We scale each against the month total so the
// today/week/month relationship is visible without inventing a denominator.
const metrics = computed(() => {
  const p = props.performance
  const today = p?.completedToday ?? 0
  const week = p?.completedThisWeek ?? 0
  const month = p?.completedThisMonth ?? 0
  const denom = Math.max(month, 1)
  return [
    { label: $t('driver.wp.completedToday'), value: String(today), pct: Math.round((today / denom) * 100), color: 'green' },
    { label: $t('driver.wp.completedThisWeek'), value: String(week), pct: Math.round((week / denom) * 100), color: 'blue' },
    { label: $t('driver.wp.completedThisMonth'), value: String(month), pct: 100, color: 'amber' },
  ]
})

const recent = computed(() => props.performance?.recentTaskHistory ?? [])
const fmtDate = (s: string) => {
  if (!s) return '—'
  try {
    return new Date(s).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
  } catch { return s }
}
</script>

<template>
  <div class="wp-card">
    <h2 class="wp-title">{{ $t('driver.wp.title') }}</h2>

    <div class="wp-list">
      <div v-for="m in metrics" :key="m.label" class="wp-row">
        <div class="wp-row-head">
          <span class="wp-label">{{ m.label }}</span>
          <span class="wp-value">{{ m.value }}</span>
        </div>
        <div class="wp-track">
          <div class="wp-fill" :class="`wp-fill--${m.color}`" :style="{ width: m.pct + '%' }" />
        </div>
      </div>
    </div>

    <!-- Recent task history — real data from recentTaskHistory[] -->
    <div v-if="recent.length" class="wp-recent">
      <p class="wp-recent__title">{{ $t('driver.wp.recentTitle') }}</p>
      <div v-for="t in recent" :key="t.deliveryId" class="wp-recent__row">
        <span class="wp-recent__dest">{{ t.destination || '—' }}</span>
        <span class="wp-recent__date">{{ fmtDate(t.completedAt) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wp-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.wp-title { font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0 0 1.25rem; }
.wp-list { display: flex; flex-direction: column; gap: 1rem; }
.wp-row-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem; }
.wp-label { font-size: 0.8125rem; color: var(--color-text-primary); }
.wp-value { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-secondary); }
.wp-track { height: 8px; border-radius: 999px; background: #F3F4F6; overflow: hidden; }
.wp-fill  { height: 100%; border-radius: 999px; }
.wp-fill--green { background: #22C55E; }
.wp-fill--blue  { background: #2563EB; }
.wp-fill--amber { background: #F59E0B; }
.wp-recent { margin-top: 1.25rem; background: #F9FAFB; border-radius: 0.5rem; padding: 0.875rem 1rem; }
.wp-recent__title { font-size: 0.75rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 0.625rem; text-transform: uppercase; letter-spacing: 0.04em; }
.wp-recent__row { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.375rem 0; border-top: 1px solid var(--color-border); }
.wp-recent__row:first-of-type { border-top: none; }
.wp-recent__dest { font-size: 0.8125rem; color: var(--color-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wp-recent__date { font-size: 0.75rem; color: var(--color-text-muted); flex-shrink: 0; }
</style>
