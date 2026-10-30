<script setup lang="ts">
const { $t } = useNuxtApp()
const links = computed(() => [
  { label: $t('market.nav.support'), icon: 'i-heroicons-headphones' },
])
const phone = '+1-202-555-0104'
</script>

<template>
  <nav class="cat-nav">
    <div class="cat-nav__inner">
      <!-- All Category -->
      <NuxtLink to="/shop" class="cat-all">
        <UIcon name="i-heroicons-squares-2x2" class="cat-all__icon" />
        {{ $t('market.nav.allCategory') }}
      </NuxtLink>

      <!-- Links -->
      <div class="cat-links">
        <a v-for="l in links" :key="l.label" href="#" class="cat-link">
          <UIcon :name="l.icon" class="cat-link__icon" />
          {{ l.label }}
        </a>
      </div>

      <!-- Phone -->
      <a :href="`tel:${phone}`" class="cat-phone">
        <UIcon name="i-heroicons-phone" class="cat-phone__icon" />
        {{ phone }}
      </a>
    </div>
  </nav>
</template>

<style scoped>
.cat-nav { background: #fff; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.cat-nav__inner {
  display: flex; align-items: center; gap: 1.5rem;
  padding: 0.625rem 1.5rem; max-width: 1280px; margin: 0 auto;
}

.cat-all {
  display: flex; align-items: center; gap: 0.5rem;
  background: #F3F4F6; border: 1px solid var(--color-border, #E5E7EB);
  border-radius: 0.375rem; padding: 0.5rem 0.875rem; text-decoration: none;
  font-size: 0.8125rem; font-weight: 600; color: #374151; cursor: pointer; white-space: nowrap;
}
.cat-all__icon { width: 1rem; height: 1rem; }
.cat-all__chevron { width: 0.875rem; height: 0.875rem; color: #9CA3AF; }

.cat-links { display: flex; align-items: center; gap: 1.5rem; flex: 1; }
.cat-link {
  display: flex; align-items: center; gap: 0.375rem;
  font-size: 0.8125rem; color: #6B7280; text-decoration: none; white-space: nowrap;
}
.cat-link:hover { color: var(--color-primary, #E8202A); }
.cat-link__icon { width: 1rem; height: 1rem; }

.cat-phone {
  display: flex; align-items: center; gap: 0.375rem;
  font-size: 0.875rem; font-weight: 600; color: #111827; text-decoration: none; white-space: nowrap;
}
.cat-phone__icon { width: 1rem; height: 1rem; color: var(--color-primary, #E8202A); }

@media (max-width: 1023px) {
  .cat-links { display: none; }
}
@media (max-width: 767px) {
  .cat-nav__inner { gap: 0.75rem; padding: 0.5rem 1rem; }
}
</style>
