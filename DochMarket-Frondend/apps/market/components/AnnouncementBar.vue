<script setup lang="ts">
// The "Black Friday" top bar. Driven entirely by apps/market/config/announcement.ts
// via useAnnouncement(); this component never needs editing for a sale.
const { config, visible } = useAnnouncement()
</script>

<template>
  <div v-if="visible" class="ann-bar">
    <!-- Badge: red pill + rest -->
    <div class="ann-brand">
      <span v-if="config.badge" class="ann-badge">{{ config.badge }}</span>
      <span v-if="config.badgeRest" class="ann-brand__rest">{{ config.badgeRest }}</span>
    </div>

    <!-- Headline -->
    <p class="ann-message">
      <span v-if="config.lead">{{ config.lead }}</span>
      <span v-if="config.highlight" class="ann-highlight">{{ config.highlight }}</span>
      <span v-if="config.trail">{{ config.trail }}</span>
    </p>

    <!-- Action -->
    <div class="ann-actions">
      <a :href="config.cta.to" class="ann-cta">
        {{ config.cta.label }}
        <UIcon name="i-heroicons-arrow-right" class="ann-cta__icon" />
      </a>
    </div>
  </div>
</template>

<style scoped>
.ann-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: #1a1a1a;
  color: #fff;
  padding: 0.5rem 1rem;
  font-size: 0.8125rem;
  min-height: 44px;
}

/* Badge */
.ann-brand { display: flex; align-items: center; gap: 0.375rem; flex-shrink: 0; }
.ann-badge {
  background: var(--color-primary, #E8202A);
  color: #fff;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.8125rem;
  letter-spacing: 0.01em;
}
.ann-brand__rest { font-weight: 600; color: #fff; }

/* Headline (centered, takes remaining space) */
.ann-message {
  flex: 1;
  margin: 0;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  font-weight: 500;
  white-space: nowrap;
}
.ann-highlight {
  color: var(--color-primary, #E8202A);
  font-weight: 800;
  font-size: 1.0625rem;
}

/* Actions */
.ann-actions { display: flex; align-items: center; gap: 0.75rem; flex-shrink: 0; }
.ann-cta {
  display: inline-flex; align-items: center; gap: 0.375rem;
  background: var(--color-primary, #E8202A); color: #fff;
  text-decoration: none; font-weight: 700; font-size: 0.75rem;
  padding: 0.4rem 0.85rem; border-radius: 0.375rem; white-space: nowrap;
  transition: background 0.15s;
}
.ann-cta:hover { background: var(--color-primary-hover, #C8181F); }
.ann-cta__icon { width: 0.875rem; height: 0.875rem; }

/* On small screens, hide the centered message to keep badge + actions tidy */
@media (max-width: 767px) {
  .ann-message { font-size: 0.75rem; gap: 0.25rem; }
  .ann-highlight { font-size: 0.9375rem; }
  .ann-bar { gap: 0.5rem; padding: 0.5rem 0.75rem; }
}
@media (max-width: 479px) {
  .ann-message { display: none; }
}
</style>
