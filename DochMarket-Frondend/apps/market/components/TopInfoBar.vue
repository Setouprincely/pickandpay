<script setup lang="ts">
const socials = [
  { icon: 'i-simple-icons-x', label: 'X', to: '#' },
  { icon: 'i-simple-icons-facebook', label: 'Facebook', to: '#' },
  { icon: 'i-simple-icons-youtube', label: 'YouTube', to: '#' },
  { icon: 'i-simple-icons-instagram', label: 'Instagram', to: '#' },
]

// Shared currency — changing this re-prices the whole storefront.
const { currency, currencies } = useCurrency()
const { $t } = useNuxtApp()
</script>

<template>
  <div class="info-bar">
    <p class="info-welcome">{{ $t('market.topbar.welcome') }}</p>

    <div class="info-right">
      <div class="info-social">
        <span class="info-social__label">{{ $t('market.topbar.followUs') }}</span>
        <a v-for="s in socials" :key="s.label" :href="s.to" :aria-label="s.label" class="info-social__link">
          <UIcon :name="s.icon" class="info-social__icon" />
        </a>
      </div>

      <div class="info-currency">
        <select v-model="currency" class="info-currency__select" aria-label="Currency">
          <option v-for="c in currencies" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-bar {
  background: var(--color-primary, #E8202A);
  color: #fff;
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 1rem 1.5rem 0.55rem; font-size: 0.875rem; font-weight: 600;
}
.info-welcome { margin: 0; opacity: 0.95; }

.info-right { display: flex; align-items: center; gap: 1.25rem; }

.info-social { display: flex; align-items: center; gap: 0.625rem; }
.info-social__label { opacity: 0.95; }
.info-social__link { color: #fff; display: flex; }
.info-social__icon { width: 1rem; height: 1rem; transition: opacity 0.15s; }
.info-social__link:hover .info-social__icon { opacity: 0.7; }

.info-currency__select {
  background: transparent; border: none; color: #fff; font-size: 0.875rem; font-weight: 600;
  cursor: pointer; outline: none; padding-right: 0.25rem;
}
.info-currency__select option { color: #111; }

@media (max-width: 767px) {
  /* Keep the welcome visible (left) + currency (right); drop the social row
     so there's room for them on a narrow screen. */
  .info-bar { justify-content: space-between; gap: 0.5rem; padding: 0.55rem 1rem 0.4rem; font-size: 0.75rem; }
  .info-welcome { display: block; font-size: 0.72rem; line-height: 1.2; }
  .info-social { display: none; }
}
</style>
