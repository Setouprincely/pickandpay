<script setup lang="ts">
const props = defineProps<{
  label: string
  value: string
  icon: string
  trend: string
  trendType: 'positive' | 'stable' | 'neutral'
  iconColor: string
}>()
</script>

<template>
  <div class="kpi-card">
    <span class="kpi-label">{{ label }}</span>
    <UIcon :name="icon" class="kpi-icon" :style="{ color: `var(${iconColor})` }" />
    <p class="kpi-value">{{ value }}</p>
    <span class="kpi-trend" :class="`kpi-trend--${trendType}`">{{ trend }}</span>
  </div>
</template>

<style scoped>
.kpi-card  { background: var(--color-surface); border: 1px solid var(--color-border);
             border-radius: 0.75rem; padding: 1rem; display: flex; flex-direction: column;
             gap: 0.5rem; min-width: 0; }
.kpi-label { font-size: 0.6875rem; color: var(--color-text-secondary); }
.kpi-icon  { width: 1.5rem; height: 1.5rem; }
.kpi-value { font-size: 1.125rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.kpi-trend { font-size: 0.75rem; font-weight: 600; }
.kpi-trend--positive { color: var(--color-trend-positive); }
.kpi-trend--stable   { color: var(--color-trend-stable); }
.kpi-trend--neutral  { color: var(--color-text-muted); }

@media (min-width: 1024px) {
  .kpi-card  { padding: 1.25rem; }
  .kpi-label { font-size: 0.75rem; }
  .kpi-icon  { width: 2rem; height: 2rem; }
  .kpi-value { font-size: 1.375rem; }
}
</style>
