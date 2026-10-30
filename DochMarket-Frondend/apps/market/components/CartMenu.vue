<script setup lang="ts">
const { items, isOpen, remove, clear, subtotal, lineCount, totalQty, toggleCart, closeCart } = useCart()
const { format } = useCurrency()
const { $t } = useNuxtApp()

const rootEl = ref<HTMLElement | null>(null)
const pad = (n: number) => String(n).padStart(2, '0')

onMounted(() => {
  const handler = (e: PointerEvent) => {
    if (rootEl.value && !rootEl.value.contains(e.target as Node)) closeCart()
  }
  window.addEventListener('pointerdown', handler)
  onBeforeUnmount(() => window.removeEventListener('pointerdown', handler))
})
</script>

<template>
  <div ref="rootEl" class="cart-menu">
    <button class="cart-btn" aria-label="Cart" @click="toggleCart">
      <UIcon name="i-heroicons-shopping-cart" class="cart-btn__icon" />
      <span v-if="totalQty" class="cart-badge">{{ totalQty }}</span>
    </button>

    <div v-if="isOpen" class="cart-panel">
      <div v-if="items.length" class="cart-panel__head">
        <p class="cart-panel__title">{{ $t('market.cartMenu.title', { n: pad(lineCount) }) }}</p>
        <button class="cart-clear" @click="clear">
          <UIcon name="i-heroicons-trash" class="cart-clear__icon" /> {{ $t('market.cartMenu.clear') }}
        </button>
      </div>
      <p v-else class="cart-panel__title">{{ $t('market.cartMenu.title', { n: pad(lineCount) }) }}</p>

      <div v-if="items.length" class="cart-items">
        <div v-for="item in items" :key="item.product.id" class="cart-item">
          <span class="cart-item__thumb">
            <img v-if="item.product.image" :src="item.product.image" :alt="item.product.title" />
          </span>
          <div class="cart-item__info">
            <p class="cart-item__title">{{ item.product.title }}</p>
            <p class="cart-item__price">
              {{ item.qty }} x <span class="cart-item__amount">{{ format(item.product.price) }}</span>
            </p>
          </div>
          <button class="cart-item__remove" aria-label="Remove" @click="remove(item.product.id)">
            <UIcon name="i-heroicons-x-mark" />
          </button>
        </div>
      </div>

      <p v-else class="cart-empty">{{ $t('market.cartMenu.empty') }}</p>

      <template v-if="items.length">
        <div class="cart-subtotal">
          <span class="cart-subtotal__label">{{ $t('market.cartMenu.subtotal') }}</span>
          <span class="cart-subtotal__val">{{ format(subtotal) }}</span>
        </div>

        <NuxtLink to="/checkout" class="cart-checkout" @click="closeCart">
          {{ $t('market.cartMenu.checkout') }} <UIcon name="i-heroicons-arrow-right" class="cart-checkout__icon" />
        </NuxtLink>
        <NuxtLink to="/cart" class="cart-view" @click="closeCart">{{ $t('market.cartMenu.view') }}</NuxtLink>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cart-menu { position: relative; }

.cart-btn {
  position: relative; background: transparent; border: none; cursor: pointer;
  width: 2.25rem; height: 2.25rem; border-radius: 0.5rem;
  display: flex; align-items: center; justify-content: center; transition: background 0.15s;
}
.cart-btn:hover { background: rgba(255,255,255,0.15); }
.cart-btn__icon { width: 1.25rem; height: 1.25rem; color: #fff; }
.cart-badge {
  position: absolute; top: -1px; right: -1px; min-width: 1.05rem; height: 1.05rem; padding: 0 0.25rem;
  background: #111827; color: #fff; font-size: 0.625rem; font-weight: 700; border-radius: 999px;
  display: flex; align-items: center; justify-content: center; border: 1.5px solid var(--color-primary, #E8202A);
}

/* Panel */
.cart-panel {
  position: absolute; top: calc(100% + 0.85rem); right: 0; z-index: 60;
  width: 21rem; max-width: 92vw; background: #fff; border-radius: 0.5rem;
  box-shadow: 0 16px 44px rgba(0,0,0,0.2); padding: 1rem;
}
.cart-panel__title { font-size: 0.875rem; font-weight: 700; color: #111827; margin: 0 0 0.75rem; }
.cart-panel__head { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
.cart-panel__head .cart-panel__title { margin-bottom: 0.75rem; }
.cart-clear {
  display: inline-flex; align-items: center; gap: 0.2rem; background: none; border: none; cursor: pointer;
  color: #9CA3AF; font-size: 0.6875rem; font-weight: 600; padding: 0; margin-bottom: 0.75rem;
}
.cart-clear:hover { color: var(--color-primary, #E8202A); }
.cart-clear__icon { width: 0.875rem; height: 0.875rem; }

.cart-items { display: flex; flex-direction: column; max-height: 17rem; overflow-y: auto; }
.cart-item { display: flex; align-items: center; gap: 0.625rem; padding: 0.625rem 0; border-top: 1px solid var(--color-border, #E5E7EB); }
.cart-item:first-child { border-top: none; }
.cart-item__thumb {
  width: 2.75rem; height: 2.75rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem;
  overflow: hidden; flex-shrink: 0; background: #F3F4F6;
}
.cart-item__thumb img { width: 100%; height: 100%; object-fit: cover; }
.cart-item__info { flex: 1; min-width: 0; }
.cart-item__title {
  font-size: 0.75rem; color: #374151; margin: 0 0 0.2rem; line-height: 1.3;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.cart-item__price { font-size: 0.75rem; color: #6B7280; margin: 0; }
.cart-item__amount { color: var(--color-primary, #E8202A); font-weight: 700; }
.cart-item__remove {
  background: none; border: none; cursor: pointer; color: #9CA3AF; flex-shrink: 0; padding: 0.25rem;
  display: flex; align-items: center; justify-content: center;
}
.cart-item__remove:hover { color: var(--color-primary, #E8202A); }
.cart-item__remove :deep(svg) { width: 1rem; height: 1rem; }

.cart-empty { font-size: 0.8125rem; color: #6B7280; text-align: center; padding: 1.5rem 0; margin: 0; }

.cart-subtotal {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.75rem 0; margin-top: 0.25rem; border-top: 1px solid var(--color-border, #E5E7EB);
  font-size: 0.8125rem; color: #6B7280;
}
.cart-subtotal__val { font-weight: 700; color: var(--color-primary, #E8202A); }

.cart-checkout {
  width: 100%; box-sizing: border-box; display: flex; align-items: center; justify-content: center; gap: 0.4rem;
  background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem; text-decoration: none;
  padding: 0.6rem 1rem; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.03em; cursor: pointer; margin-bottom: 0.5rem;
}
.cart-checkout:hover { background: var(--color-primary-hover, #C8181F); }
.cart-checkout__icon { width: 0.875rem; height: 0.875rem; }
.cart-view {
  width: 100%; box-sizing: border-box; display: block; text-align: center; background: #fff; color: var(--color-primary, #E8202A);
  border: 1px solid var(--color-primary, #E8202A); border-radius: 0.375rem; text-decoration: none;
  padding: 0.6rem 1rem; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.03em; cursor: pointer;
}
.cart-view:hover { background: #FEF2F2; }

/* Mobile: tighter, narrower panel so it doesn't fill the screen */
@media (max-width: 640px) {
  .cart-panel { width: 16.5rem; max-width: 84vw; padding: 0.75rem; top: calc(100% + 0.6rem); }
  .cart-panel__title { font-size: 0.8rem; margin-bottom: 0.6rem; }
  .cart-items { max-height: 14rem; }
  .cart-item { gap: 0.5rem; padding: 0.5rem 0; }
  .cart-item__thumb { width: 2.25rem; height: 2.25rem; }
  .cart-item__title { font-size: 0.7rem; }
  .cart-item__price { font-size: 0.7rem; }
  .cart-subtotal { font-size: 0.75rem; padding: 0.6rem 0; }
  .cart-checkout, .cart-view { padding: 0.5rem; font-size: 0.7rem; }
}
</style>
