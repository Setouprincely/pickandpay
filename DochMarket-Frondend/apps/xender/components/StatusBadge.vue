<script setup lang="ts">
import { STATUS_META, type ShipmentStatus } from '../stores/shipments'

const props = defineProps<{ status: ShipmentStatus; size?: 'sm' | 'md' }>()
const meta = computed(() => STATUS_META[props.status])
</script>

<template>
  <span class="xn-badge" :class="`xn-badge--${size || 'md'}`" :style="{ color: meta.color, background: meta.color + '1A' }">
    <UIcon :name="meta.icon" class="xn-badge__icon" />
    {{ meta.label }}
  </span>
</template>

<style scoped>
.xn-badge { display: inline-flex; align-items: center; gap: 0.35rem; font-weight: 700; border-radius: 999px; white-space: nowrap; }
.xn-badge--md { font-size: 0.72rem; padding: 0.3rem 0.65rem; }
.xn-badge--sm { font-size: 0.66rem; padding: 0.2rem 0.5rem; }
.xn-badge__icon { width: 0.9rem; height: 0.9rem; }
</style>
