<script setup lang="ts">
defineProps<{
  icon: string
  iconTint: 'blue' | 'amber' | 'green'
  label: string
  value: string
  info?: string        // top-right helper text (e.g. "+3 from yesterday")
  stars?: number       // top-right star rating (filled count out of 5)
}>()
</script>

<template>
  <div class="kpi-card">
    <!-- Top row: icon left, secondary info right -->
    <div class="kpi-top">
      <span class="kpi-icon-wrap" :class="`kpi-icon-wrap--${iconTint}`">
        <UIcon :name="icon" class="kpi-icon" />
      </span>

      <span v-if="info" class="kpi-info">{{ info }}</span>

      <span v-else-if="stars != null" class="kpi-stars">
        <UIcon
          v-for="n in 5" :key="n"
          name="i-heroicons-star-solid"
          class="kpi-star"
          :class="{ 'kpi-star--filled': n <= stars }"
        />
      </span>
    </div>

    <!-- Label + value -->
    <p class="kpi-label">{{ label }}</p>
    <p class="kpi-value">{{ value }}</p>
  </div>
</template>

<style scoped>
.kpi-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  padding: 1.25rem;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

/* Icon tile */
.kpi-icon-wrap {
  width: 2.5rem; height: 2.5rem;
  border-radius: 0.625rem;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.kpi-icon { width: 1.25rem; height: 1.25rem; }
.kpi-icon-wrap--blue  { background: #EFF6FF; }
.kpi-icon-wrap--blue  .kpi-icon { color: #2563EB; }
.kpi-icon-wrap--amber { background: #FFFBEB; }
.kpi-icon-wrap--amber .kpi-icon { color: #F59E0B; }
.kpi-icon-wrap--green { background: #ECFDF5; }
.kpi-icon-wrap--green .kpi-icon { color: #16A34A; }

/* Secondary info */
.kpi-info  { font-size: 0.75rem; font-weight: 500; color: #2563EB; text-align: right; }
.kpi-stars { display: inline-flex; gap: 1px; }
.kpi-star  { width: 0.875rem; height: 0.875rem; color: #E5E7EB; }
.kpi-star--filled { color: #F59E0B; }

/* Text */
.kpi-label { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0 0 0.375rem; }
.kpi-value { font-size: 1.5rem; font-weight: 700; color: var(--color-text-primary); margin: 0; line-height: 1.1; }
</style>
