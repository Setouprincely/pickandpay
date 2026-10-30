<script setup lang="ts">
// Shown in the footer on mobile only — the top info bar hides its social row on
// small screens, so this is where "Follow us" lives on phones.
const socials = [
  { icon: 'i-simple-icons-x', label: 'X', to: '#' },
  { icon: 'i-simple-icons-facebook', label: 'Facebook', to: '#' },
  { icon: 'i-simple-icons-youtube', label: 'YouTube', to: '#' },
  { icon: 'i-simple-icons-instagram', label: 'Instagram', to: '#' },
]

const { $t } = useNuxtApp()
const columns = computed(() => [
  {
    title: $t('market.footer.shopProduct'),
    links: [
      $t('market.footer.shopLinks.computer'),
      $t('market.footer.shopLinks.smartphone'),
      $t('market.footer.shopLinks.headphone'),
      $t('market.footer.shopLinks.accessories'),
      $t('market.footer.shopLinks.camera'),
      $t('market.footer.shopLinks.tvHome'),
    ],
    browse: true,
    to: '/shop',
  },
  {
    title: $t('market.footer.sell'),
    links: [
      $t('market.footer.sellLinks.start'),
      $t('market.footer.sellLinks.guidelines'),
      $t('market.footer.sellLinks.support'),
      $t('market.footer.sellLinks.dashboard'),
      $t('market.footer.sellLinks.success'),
    ],
  },
  {
    title: $t('market.footer.support'),
    links: [
      $t('market.footer.supportLinks.help'),
      $t('market.footer.supportLinks.shipping'),
      $t('market.footer.supportLinks.returns'),
      $t('market.footer.supportLinks.contact'),
      $t('market.footer.supportLinks.track'),
    ],
  },
])
</script>

<template>
  <footer class="footer">
    <div class="footer__inner">
      <!-- Brand -->
      <div class="footer__brand">
        <NuxtLink to="/" class="footer__logo">
          <img src="/images/logos/Logo-01.png" alt="Doch" class="footer__logo-img" />
        </NuxtLink>
        <p class="footer__support-label">{{ $t('market.footer.supportLabel') }}</p>
        <p class="footer__phone">(+237) 676-026-603</p>
        <p class="footer__addr">Check Point Molyko, Buea, Cameroon</p>
        <p class="footer__email">info@kinbo.com</p>

        <!-- Follow us (mobile only — top bar carries this on desktop) -->
        <div class="footer__social">
          <span class="footer__social-label">{{ $t('market.footer.followUs') }}</span>
          <a v-for="s in socials" :key="s.label" :href="s.to" :aria-label="s.label" class="footer__social-link">
            <UIcon :name="s.icon" class="footer__social-icon" />
          </a>
        </div>
      </div>

      <!-- Link columns -->
      <div v-for="col in columns" :key="col.title" class="footer__col">
        <p class="footer__col-title">{{ col.title }}</p>
        <ul class="footer__links">
          <li v-for="link in col.links" :key="link">
            <NuxtLink v-if="col.to" :to="col.to" class="footer__link" :class="{ 'footer__link--active': link === $t('market.footer.shopLinks.accessories') }">{{ link }}</NuxtLink>
            <a v-else href="#" class="footer__link">{{ link }}</a>
          </li>
          <li v-if="col.browse">
            <NuxtLink to="/shop" class="footer__link footer__link--browse">{{ $t('market.footer.browseAll') }} <UIcon name="i-heroicons-arrow-right" class="footer__browse-icon" /></NuxtLink>
          </li>
        </ul>
      </div>
    </div>

    <div class="footer__bar">
      <p>{{ $t('market.footer.copyright') }}</p>
    </div>
  </footer>
</template>

<style scoped>
.footer { background: #1a1a1a; color: #D1D5DB; }
.footer__inner {
  max-width: 1280px; margin: 0 auto; padding: 2.5rem 1.5rem;
  display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr; gap: 2rem;
}

/* Brand */
.footer__logo { display: inline-flex; align-items: center; margin-bottom: 1.25rem; }
.footer__logo-img { height: 2.75rem; width: auto; display: block; }
.footer__support-label { font-size: 0.75rem; color: #9CA3AF; margin: 0 0 0.25rem; }
.footer__phone { font-size: 0.9375rem; font-weight: 700; color: #fff; margin: 0 0 0.875rem; }
.footer__addr { font-size: 0.8125rem; color: #9CA3AF; margin: 0 0 0.5rem; }
.footer__email { font-size: 0.8125rem; color: #9CA3AF; margin: 0; }

/* Follow us — hidden on desktop (top bar shows it), revealed on mobile */
.footer__social { display: none; align-items: center; gap: 0.625rem; margin-top: 1.25rem; }
.footer__social-label { font-size: 0.8125rem; color: #9CA3AF; }
.footer__social-link { color: #D1D5DB; display: flex; }
.footer__social-link:hover { color: #fff; }
.footer__social-icon { width: 1.05rem; height: 1.05rem; }

/* Columns */
.footer__col-title { font-size: 0.8125rem; font-weight: 700; color: #fff; margin: 0 0 1rem; letter-spacing: 0.03em; }
.footer__links { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.625rem; }
.footer__link { font-size: 0.8125rem; color: #9CA3AF; text-decoration: none; display: inline-flex; align-items: center; gap: 0.375rem; }
.footer__link:hover { color: #fff; }
.footer__link--active { color: var(--color-primary, #E8202A); }
.footer__link--browse { color: var(--color-primary, #E8202A); font-weight: 600; }
.footer__browse-icon { width: 0.875rem; height: 0.875rem; }

/* Bottom bar */
.footer__bar { border-top: 1px solid #333; padding: 1.25rem 1.5rem; text-align: center; }
.footer__bar p { font-size: 0.8125rem; color: #9CA3AF; margin: 0; }

@media (max-width: 900px) {
  .footer__inner { grid-template-columns: 1fr 1fr; gap: 1.75rem; }
}
@media (max-width: 767px) {
  .footer__social { display: flex; }
}
@media (max-width: 520px) {
  /* Keep two columns on phones (instead of stacking to one) */
  .footer__inner { grid-template-columns: 1fr 1fr; gap: 1.25rem 1rem; padding: 2rem 1.25rem; }
  .footer__col-title { font-size: 0.75rem; margin-bottom: 0.75rem; }
  .footer__link { font-size: 0.75rem; }
}
</style>
