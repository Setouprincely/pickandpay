<script setup lang="ts">
import type { Product } from '~/apps/market/types'

const { items, isOpen, count, remove, clear, toggleWishlist, closeWishlist } = useWishlist()
const { add: addToCart } = useCart()
const { format } = useCurrency()
const { $t } = useNuxtApp()

const rootEl = ref<HTMLElement | null>(null)
const pad = (n: number) => String(n).padStart(2, '0')

function moveToCart(product: Product) {
  addToCart(product)
  remove(product.id)
}

onMounted(() => {
  const handler = (e: PointerEvent) => {
    if (rootEl.value && !rootEl.value.contains(e.target as Node)) closeWishlist()
  }
  window.addEventListener('pointerdown', handler)
  onBeforeUnmount(() => window.removeEventListener('pointerdown', handler))
})
</script>

<template>
  <div ref="rootEl" class="wl-menu">
    <button class="wl-btn" aria-label="Wishlist" @click="toggleWishlist">
      <UIcon name="i-heroicons-heart" class="wl-btn__icon" />
      <span v-if="count" class="wl-badge">{{ count }}</span>
    </button>

    <div v-if="isOpen" class="wl-panel">
      <div class="wl-panel__head">
        <p class="wl-panel__title">{{ $t('market.wishlistMenu.title', { n: pad(count) }) }}</p>
        <button v-if="items.length" class="wl-clear" @click="clear">
          <UIcon name="i-heroicons-trash" class="wl-clear__icon" /> {{ $t('market.wishlistMenu.clear') }}
        </button>
      </div>

      <div v-if="items.length" class="wl-items">
        <div v-for="p in items" :key="p.id" class="wl-item">
          <span class="wl-item__thumb">
            <img v-if="p.image" :src="p.image" :alt="p.title" />
          </span>
          <div class="wl-item__info">
            <p class="wl-item__title">{{ p.title }}</p>
            <p class="wl-item__price">{{ format(p.price) }}</p>
            <button class="wl-item__move" :disabled="p.soldOut" @click="moveToCart(p)">
              <UIcon name="i-heroicons-shopping-cart" /> {{ p.soldOut ? $t('market.wishlistMenu.soldOut') : $t('market.wishlistMenu.addToCart') }}
            </button>
          </div>
          <button class="wl-item__remove" aria-label="Remove" @click="remove(p.id)">
            <UIcon name="i-heroicons-x-mark" />
          </button>
        </div>
      </div>

      <p v-else class="wl-empty">{{ $t('market.wishlistMenu.empty') }}</p>

      <NuxtLink v-if="items.length" to="/wishlist" class="wl-view" @click="closeWishlist">{{ $t('market.wishlistMenu.view') }}</NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.wl-menu { position: relative; }

.wl-btn {
  position: relative; background: transparent; border: none; cursor: pointer;
  width: 2.25rem; height: 2.25rem; border-radius: 0.5rem;
  display: flex; align-items: center; justify-content: center; transition: background 0.15s;
}
.wl-btn:hover { background: rgba(255,255,255,0.15); }
.wl-btn__icon { width: 1.25rem; height: 1.25rem; color: #fff; }
.wl-badge {
  position: absolute; top: -1px; right: -1px; min-width: 1.05rem; height: 1.05rem; padding: 0 0.25rem;
  background: #111827; color: #fff; font-size: 0.625rem; font-weight: 700; border-radius: 999px;
  display: flex; align-items: center; justify-content: center; border: 1.5px solid var(--color-primary, #E8202A);
}

/* Panel */
.wl-panel {
  position: absolute; top: calc(100% + 0.85rem); right: 0; z-index: 60;
  width: 21rem; max-width: 92vw; background: #fff; border-radius: 0.5rem;
  box-shadow: 0 16px 44px rgba(0,0,0,0.2); padding: 1rem;
}
.wl-panel__head { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.75rem; }
.wl-panel__title { font-size: 0.875rem; font-weight: 700; color: #111827; margin: 0; }
.wl-clear {
  display: inline-flex; align-items: center; gap: 0.2rem; background: none; border: none; cursor: pointer;
  color: #9CA3AF; font-size: 0.6875rem; font-weight: 600; padding: 0;
}
.wl-clear:hover { color: var(--color-primary, #E8202A); }
.wl-clear__icon { width: 0.875rem; height: 0.875rem; }

.wl-items { display: flex; flex-direction: column; max-height: 19rem; overflow-y: auto; }
.wl-item { display: flex; align-items: flex-start; gap: 0.625rem; padding: 0.625rem 0; border-top: 1px solid var(--color-border, #E5E7EB); }
.wl-item:first-child { border-top: none; }
.wl-item__thumb {
  width: 2.75rem; height: 2.75rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem;
  overflow: hidden; flex-shrink: 0; background: #F3F4F6;
}
.wl-item__thumb img { width: 100%; height: 100%; object-fit: cover; }
.wl-item__info { flex: 1; min-width: 0; }
.wl-item__title {
  font-size: 0.75rem; color: #374151; margin: 0 0 0.2rem; line-height: 1.3;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.wl-item__price { font-size: 0.8125rem; font-weight: 700; color: var(--color-primary, #E8202A); margin: 0 0 0.35rem; }
.wl-item__move {
  display: inline-flex; align-items: center; gap: 0.3rem; background: #FEF2F2; color: var(--color-primary, #E8202A);
  border: 1px solid #FECACA; border-radius: 0.3rem; padding: 0.25rem 0.5rem; font-size: 0.6875rem; font-weight: 700; cursor: pointer;
}
.wl-item__move:hover:not(:disabled) { background: var(--color-primary, #E8202A); color: #fff; }
.wl-item__move:disabled { opacity: 0.5; cursor: not-allowed; }
.wl-item__move :deep(svg) { width: 0.8125rem; height: 0.8125rem; }
.wl-item__remove {
  background: none; border: none; cursor: pointer; color: #9CA3AF; flex-shrink: 0; padding: 0.25rem;
  display: flex; align-items: center; justify-content: center;
}
.wl-item__remove:hover { color: var(--color-primary, #E8202A); }
.wl-item__remove :deep(svg) { width: 1rem; height: 1rem; }

.wl-empty { font-size: 0.8125rem; color: #6B7280; text-align: center; padding: 1.5rem 0; margin: 0; }

.wl-view {
  display: block; box-sizing: border-box; width: 100%; text-align: center; text-decoration: none;
  margin-top: 0.875rem; background: var(--color-primary, #E8202A); color: #fff;
  border-radius: 0.375rem; padding: 0.6rem 1rem; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.03em;
}
.wl-view:hover { background: var(--color-primary-hover, #C8181F); }

/* Mobile: tighter, narrower panel so it doesn't fill the screen */
@media (max-width: 640px) {
  .wl-panel { width: 16.5rem; max-width: 84vw; padding: 0.75rem; top: calc(100% + 0.6rem); }
  .wl-panel__title { font-size: 0.8rem; }
  .wl-items { max-height: 15rem; }
  .wl-item { gap: 0.5rem; padding: 0.5rem 0; }
  .wl-item__thumb { width: 2.25rem; height: 2.25rem; }
  .wl-item__title { font-size: 0.7rem; }
  .wl-item__price { font-size: 0.75rem; }
  .wl-item__move { font-size: 0.625rem; padding: 0.22rem 0.4rem; }
  .wl-view { padding: 0.5rem; font-size: 0.7rem; }
}
</style>
