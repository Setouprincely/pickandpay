<script setup lang="ts">
useHead({ title: 'Your Wishlist — Doch Market' })

const { items, remove, clear, count } = useWishlist()
const { add: addToCart } = useCart()
const { notify } = useNotify()
const { $t } = useNuxtApp()

function addAllToCart() {
  const available = items.value.filter(p => !p.soldOut)
  if (!available.length) {
    notify({ title: $t('market.wishlist.title'), message: $t('market.wishlist.nothingToAdd'), variant: 'info' })
    return
  }
  available.forEach(p => addToCart(p))
}
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.account.wishlist') }]" />

    <section class="wl">
      <div class="wl__inner">
        <div class="wl__head">
          <h1 class="wl__title">{{ $t('market.wishlist.title') }} <span v-if="count" class="wl__count">({{ count }})</span></h1>
          <div v-if="items.length" class="wl__actions">
            <button class="wl__btn wl__btn--primary" @click="addAllToCart">
              <UIcon name="i-heroicons-shopping-cart" /> {{ $t('market.wishlist.addAll') }}
            </button>
            <button class="wl__btn wl__btn--ghost" @click="clear">
              <UIcon name="i-heroicons-trash" /> {{ $t('market.wishlist.clear') }}
            </button>
          </div>
        </div>

        <!-- Empty -->
        <div v-if="!items.length" class="wl-empty">
          <UIcon name="i-heroicons-heart" class="wl-empty__icon" />
          <p class="wl-empty__title">{{ $t('market.wishlist.emptyTitle') }}</p>
          <p class="wl-empty__text">{{ $t('market.wishlist.emptyText') }}</p>
          <NuxtLink to="/shop" class="wl-empty__cta">{{ $t('market.wishlist.browse') }}</NuxtLink>
        </div>

        <!-- Grid -->
        <div v-else class="wl-grid">
          <div v-for="p in items" :key="p.id" class="wl-card">
            <button class="wl-card__remove" aria-label="Remove" @click="remove(p.id)">
              <UIcon name="i-heroicons-x-mark" />
            </button>
            <MarketProductCard :product="p" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wl__inner { max-width: 1280px; margin: 0 auto; padding: 1.75rem 1.5rem 3rem; }
.wl__head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
.wl__title { font-size: 1.375rem; font-weight: 800; color: #111827; margin: 0; }
.wl__count { color: #9CA3AF; font-weight: 600; }
.wl__actions { display: flex; gap: 0.625rem; flex-wrap: wrap; }
.wl__btn { display: inline-flex; align-items: center; gap: 0.4rem; border-radius: 0.375rem; padding: 0.55rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.wl__btn :deep(svg) { width: 1rem; height: 1rem; }
.wl__btn--primary { background: var(--color-primary, #E8202A); color: #fff; border: none; }
.wl__btn--primary:hover { background: var(--color-primary-hover, #C8181F); }
.wl__btn--ghost { background: #fff; color: #6B7280; border: 1px solid var(--color-border, #E5E7EB); }
.wl__btn--ghost:hover { color: var(--color-primary, #E8202A); border-color: var(--color-primary, #E8202A); }

/* Empty */
.wl-empty { text-align: center; padding: 4rem 1rem; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; }
.wl-empty__icon { width: 3rem; height: 3rem; color: #D1D5DB; }
.wl-empty__title { font-size: 1.125rem; font-weight: 700; color: #374151; margin: 1rem 0 0.25rem; }
.wl-empty__text { font-size: 0.875rem; color: #9CA3AF; margin: 0 0 1.25rem; }
.wl-empty__cta { background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.65rem 1.75rem; font-size: 0.8125rem; font-weight: 700; }

/* Grid */
.wl-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
.wl-card { position: relative; }
.wl-card__remove {
  position: absolute; top: 0.5rem; right: 0.5rem; z-index: 3;
  width: 1.75rem; height: 1.75rem; border-radius: 50%; border: none; cursor: pointer;
  background: #fff; color: #6B7280; box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center;
}
.wl-card__remove:hover { background: var(--color-primary, #E8202A); color: #fff; }
.wl-card__remove :deep(svg) { width: 1rem; height: 1rem; }

@media (max-width: 1100px) { .wl-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px) {
  .wl-grid { grid-template-columns: 1fr 1fr; gap: 0.7rem; }
  .wl__inner { padding: 1.25rem 1rem 2.5rem; }
  .wl__title { font-size: 1.1rem; }
  .wl__btn { padding: 0.45rem 0.75rem; font-size: 0.75rem; }
  .wl-card__remove { width: 1.5rem; height: 1.5rem; top: 0.35rem; right: 0.35rem; }
}
</style>
