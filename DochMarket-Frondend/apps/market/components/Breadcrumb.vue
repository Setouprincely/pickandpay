<script setup lang="ts">
// Reusable breadcrumb trail. Items with a `to` render as links; the last is the current page.
interface Crumb { label: string; to?: string }
defineProps<{ items: Crumb[] }>()
</script>

<template>
  <nav class="bc" aria-label="Breadcrumb">
    <div class="bc__inner">
      <NuxtLink to="/" class="bc__link">Home</NuxtLink>
      <template v-for="(c, i) in items" :key="i">
        <UIcon name="i-heroicons-chevron-right" class="bc__sep" />
        <NuxtLink v-if="c.to && i < items.length - 1" :to="c.to" class="bc__link">{{ c.label }}</NuxtLink>
        <span v-else class="bc__current">{{ c.label }}</span>
      </template>
    </div>
  </nav>
</template>

<style scoped>
.bc { background: #F9FAFB; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.bc__inner {
  max-width: 1280px; margin: 0 auto; padding: 0.875rem 1.5rem;
  display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;
  font-size: 0.8125rem;
}
.bc__link { color: #6B7280; text-decoration: none; }
.bc__link:hover { color: var(--color-primary, #E8202A); }
.bc__sep { width: 0.875rem; height: 0.875rem; color: #9CA3AF; }
.bc__current { color: #111827; font-weight: 600; }
</style>
