<script setup lang="ts">
const features = [
  { icon: 'i-heroicons-cube', title: 'FASTED DELIVERY', sub: 'Delivery in 24/H' },
  { icon: 'i-heroicons-trophy', title: '24 HOURS RETURN', sub: '100% money-back guarantee' },
  { icon: 'i-heroicons-credit-card', title: 'SECURE PAYMENT', sub: 'Your money is safe' },
  { icon: 'i-heroicons-headphones', title: 'SUPPORT 24/7', sub: 'Live contact/message' },
]
</script>

<template>
  <section class="feature-strip">
    <div class="feature-strip__inner">
      <div v-for="(f, i) in features" :key="f.title" class="feature" :class="{ 'feature--divider': i > 0 }">
        <UIcon :name="f.icon" class="feature__icon" />
        <div class="feature__text">
          <p class="feature__title">{{ f.title }}</p>
          <p class="feature__sub">{{ f.sub }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.feature-strip { background: #fff; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.feature-strip__inner {
  max-width: 1280px; margin: 0 auto; padding: 1.25rem 1.5rem;
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem;
}
.feature { display: flex; align-items: center; gap: 0.875rem; padding-left: 1.25rem; }
.feature--divider { border-left: 1px solid var(--color-border, #E5E7EB); }
.feature__icon { width: 1.75rem; height: 1.75rem; color: var(--color-primary, #E8202A); flex-shrink: 0; }
.feature__title { font-size: 0.8125rem; font-weight: 700; color: #111827; margin: 0 0 0.15rem; }
.feature__sub { font-size: 0.75rem; color: #9CA3AF; margin: 0; }

@media (max-width: 900px) {
  .feature-strip__inner { grid-template-columns: 1fr 1fr; gap: 1.25rem; }
  .feature:nth-child(odd) { padding-left: 0; border-left: none; }
}
@media (max-width: 520px) {
  .feature-strip__inner { grid-template-columns: 1fr; }
  .feature { padding-left: 0; border-left: none; }
}
</style>
