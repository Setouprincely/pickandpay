<script setup lang="ts">
import type { Ref } from 'vue'
import type { Locale } from '~/i18n/messages'

// Floating rectangular EN / FR switch. Provided by the i18n plugin,
// so it works in every app once <DochLanguageToggle /> is placed in a layout.
const { $locale, $setLocale, $t } = useNuxtApp()
const locale = $locale as Ref<Locale>
const set = $setLocale as (l: Locale) => void
const t = $t as (key: string, params?: Record<string, string | number>) => string
</script>

<template>
  <div class="lang-toggle" role="group" :aria-label="t('lang.aria')">
    <button
      type="button"
      class="lang-toggle__btn"
      :class="{ 'lang-toggle__btn--active': locale === 'en' }"
      :aria-pressed="locale === 'en'"
      @click="set('en')"
    >
      {{ t('lang.en') }}
    </button>
    <button
      type="button"
      class="lang-toggle__btn"
      :class="{ 'lang-toggle__btn--active': locale === 'fr' }"
      :aria-pressed="locale === 'fr'"
      @click="set('fr')"
    >
      {{ t('lang.fr') }}
    </button>
  </div>
</template>

<style scoped>
.lang-toggle {
  position: fixed;
  right: 1rem;
  bottom: 17rem;
  z-index: 200;
  display: inline-flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.55rem;
  padding: 0.2rem;
  gap: 0.2rem;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.lang-toggle__btn {
  border: none;
  background: transparent;
  color: var(--color-text-secondary, #6b7280);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  padding: 0.33rem 0.7rem;
  border-radius: 0.4rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  min-width: 2.4rem;
}
.lang-toggle__btn:hover { background: var(--color-page-bg, #f3f4f6); }
.lang-toggle__btn--active {
  background: var(--color-button-red, var(--color-primary, #dc2626));
  color: #fff;
}
.lang-toggle__btn--active:hover { background: var(--color-primary-hover, #b91c1c); }

@media (min-width: 1024px) {
  .lang-toggle { right: 1.5rem; bottom: 18.5rem; }
}

/* Smaller floating switch on phones */
@media (max-width: 640px) {
  .lang-toggle { right: 0.75rem; padding: 0.15rem; gap: 0.15rem; border-radius: 0.5rem; }
  .lang-toggle__btn { font-size: 0.7rem; padding: 0.26rem 0.55rem; min-width: 2.1rem; border-radius: 0.375rem; }
}
</style>
