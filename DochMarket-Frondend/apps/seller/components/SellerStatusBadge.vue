<script setup lang="ts">
import type { ProductStatus } from '~/apps/seller/types'

const props = defineProps<{ status: ProductStatus }>()
const { $t } = useNuxtApp()

const label = computed(() => $t(`seller.products.status.${props.status}`))
</script>

<template>
  <span class="status" :class="`status--${status}`">
    <span class="status__dot" />
    {{ label }}
  </span>
</template>

<style scoped>
.status {
  display: inline-flex; align-items: center; gap: 0.375rem;
  font-size: 0.6875rem; font-weight: 600; line-height: 1;
  padding: 0.3rem 0.55rem; border-radius: 999px; white-space: nowrap;
}
.status__dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

.status--active        { background: #DCFCE7; color: #15803D; }
.status--active .status__dot { background: #16A34A; }
.status--draft         { background: #F3F4F6; color: #4B5563; }
.status--draft .status__dot { background: #9CA3AF; }
.status--out_of_stock  { background: #FEE2E2; color: #B91C1C; }
.status--out_of_stock .status__dot { background: #EF4444; }
</style>
