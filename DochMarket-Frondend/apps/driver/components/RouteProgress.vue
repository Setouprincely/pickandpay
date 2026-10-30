<script setup lang="ts">
const props = defineProps<{
  current: number
  total: number
  percent: number
}>()

const { $t } = useNuxtApp()

// SVG ring maths
const R = 30
const C = 2 * Math.PI * R
const dash = computed(() => `${(props.percent / 100) * C} ${C}`)
</script>

<template>
  <div class="rp-card">
    <div class="rp-text">
      <p class="rp-title">{{ $t('driver.rp.title') }}</p>
      <p class="rp-count"><span class="rp-count__current">{{ current }}</span> / {{ total }} {{ $t('driver.rp.deliveries') }}</p>
    </div>

    <div class="rp-ring">
      <svg viewBox="0 0 76 76" class="rp-ring__svg">
        <circle cx="38" cy="38" :r="R" fill="none" stroke="#E5E7EB" stroke-width="7" />
        <circle
          cx="38" cy="38" :r="R" fill="none" stroke="#2563EB" stroke-width="7"
          stroke-linecap="round"
          :stroke-dasharray="dash"
          transform="rotate(-90 38 38)"
        />
      </svg>
      <span class="rp-ring__label">{{ percent }}%</span>
    </div>
  </div>
</template>

<style scoped>
.rp-card  { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem;
            padding: 1.25rem 1.5rem; display: flex; align-items: center; justify-content: space-between; gap: 1rem; min-width: 0; }
.rp-title { font-size: 0.9375rem; font-weight: 600; color: var(--color-text-primary); margin: 0 0 0.375rem; }
.rp-count { font-size: 0.875rem; color: var(--color-text-secondary); margin: 0; }
.rp-count__current { font-size: 1.5rem; font-weight: 700; color: var(--color-text-primary); }

.rp-ring { position: relative; width: 76px; height: 76px; flex-shrink: 0; }
.rp-ring__svg { width: 76px; height: 76px; display: block; }
.rp-ring__label { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
                  font-size: 0.875rem; font-weight: 700; color: #2563EB; }
</style>
