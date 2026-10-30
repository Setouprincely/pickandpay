<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  sub?: string
}>()
</script>

<template>
  <div class="empty">
    <div class="empty__icon-wrap">
      <UIcon :name="icon || 'i-lucide-package-open'" class="empty__icon" />
    </div>
    <p class="empty__title">{{ title }}</p>
    <p v-if="sub" class="empty__sub">{{ sub }}</p>
    <div class="empty__action"><slot /></div>
  </div>
</template>

<style scoped>
.empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  text-align: center; padding: 3rem 1.5rem; gap: 0.25rem;
}
.empty__icon-wrap {
  width: 3.5rem; height: 3.5rem; border-radius: 50%;
  background: var(--color-page-bg); display: flex; align-items: center; justify-content: center;
  margin-bottom: 0.75rem;
}
.empty__icon { width: 1.75rem; height: 1.75rem; color: var(--color-text-muted); }
.empty__title { font-size: 0.95rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.empty__sub { font-size: 0.825rem; color: var(--color-text-secondary); margin: 0; max-width: 22rem; }
.empty__action { margin-top: 1rem; }
.empty__action:empty { display: none; }
</style>
