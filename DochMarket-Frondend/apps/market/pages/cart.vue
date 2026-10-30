<script setup lang="ts">
useHead({ title: 'Your Cart — Doch Market' })

const { items, setQty, remove, clear, subtotal } = useCart()
const { format } = useCurrency()
const { $t } = useNuxtApp()
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.cart.title') }]" />

    <section class="cart">
      <div class="cart__inner">
        <h1 class="cart__title">{{ $t('market.cart.title') }}</h1>

        <!-- Empty -->
        <div v-if="!items.length" class="cart-empty">
          <UIcon name="i-heroicons-shopping-cart" class="cart-empty__icon" />
          <p class="cart-empty__title">{{ $t('market.cart.emptyTitle') }}</p>
          <p class="cart-empty__text">{{ $t('market.cart.emptyText') }}</p>
          <NuxtLink to="/shop" class="cart-empty__btn">{{ $t('market.cart.continueShopping') }}</NuxtLink>
        </div>

        <!-- Filled -->
        <div v-else class="cart-grid">
          <!-- Line items -->
          <div class="cart-lines">
            <div class="cart-lines__head">
              <span>{{ $t('market.cart.product') }}</span>
              <span class="cart-lines__h-price">{{ $t('market.cart.price') }}</span>
              <span class="cart-lines__h-qty">{{ $t('market.cart.quantity') }}</span>
              <span class="cart-lines__h-sub">{{ $t('market.cart.subtotal') }}</span>
              <span></span>
            </div>

            <div v-for="item in items" :key="item.product.id" class="cart-row">
              <div class="cart-row__product">
                <NuxtLink :to="`/product/${item.product.id}`" class="cart-row__thumb">
                  <img v-if="item.product.image" :src="item.product.image" :alt="item.product.title" />
                </NuxtLink>
                <div>
                  <NuxtLink :to="`/product/${item.product.id}`" class="cart-row__name">{{ item.product.title }}</NuxtLink>
                  <p v-if="item.product.brand" class="cart-row__brand">{{ item.product.brand }}</p>
                </div>
              </div>

              <div class="cart-row__price" :data-label="$t('market.cart.price')">{{ format(item.product.price) }}</div>

              <div class="cart-row__qty" data-label="Quantity">
                <div class="qty">
                  <button class="qty__btn" aria-label="Decrease" @click="setQty(item.product.id, item.qty - 1)"><UIcon name="i-heroicons-minus" /></button>
                  <span class="qty__val">{{ String(item.qty).padStart(2, '0') }}</span>
                  <button class="qty__btn" aria-label="Increase" @click="setQty(item.product.id, item.qty + 1)"><UIcon name="i-heroicons-plus" /></button>
                </div>
              </div>

              <div class="cart-row__sub" :data-label="$t('market.cart.subtotal')">{{ format(item.product.price * item.qty) }}</div>

              <button class="cart-row__remove" aria-label="Remove" @click="remove(item.product.id)">
                <UIcon name="i-heroicons-trash" />
              </button>
            </div>

            <div class="cart-lines__foot">
              <NuxtLink to="/shop" class="cart-lines__continue">
                <UIcon name="i-heroicons-arrow-left" /> {{ $t('market.cart.continueShopping') }}
              </NuxtLink>
              <button class="cart-lines__clear" @click="clear">
                <UIcon name="i-heroicons-trash" /> {{ $t('market.cart.clearCart') }}
              </button>
            </div>
          </div>

          <!-- Summary -->
          <aside class="summary">
            <h2 class="summary__title">{{ $t('market.cart.totals') }}</h2>

            <div class="summary__row"><span>{{ $t('market.cart.subTotal') }}</span><span>{{ format(subtotal) }}</span></div>
            <div class="summary__row">
              <span>{{ $t('market.cart.shipping') }}</span>
              <span>Calculated at checkout</span>
            </div>
            <p class="summary__hint">Delivery is priced per seller from the pickup address to your delivery address.</p>

            <div class="summary__total"><span>Products total</span><span>{{ format(subtotal) }}</span></div>

            <NuxtLink to="/checkout" class="summary__checkout">
              {{ $t('market.cart.proceed') }} <UIcon name="i-heroicons-arrow-right" class="summary__checkout-icon" />
            </NuxtLink>
          </aside>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cart__inner { max-width: 1280px; margin: 0 auto; padding: 1.75rem 1.5rem 3rem; }
.cart__title { font-size: 1.375rem; font-weight: 800; color: #111827; margin: 0 0 1.5rem; }

/* Empty */
.cart-empty { text-align: center; padding: 4rem 1rem; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; }
.cart-empty__icon { width: 3rem; height: 3rem; color: #D1D5DB; }
.cart-empty__title { font-size: 1.125rem; font-weight: 700; color: #374151; margin: 1rem 0 0.25rem; }
.cart-empty__text { font-size: 0.875rem; color: #9CA3AF; margin: 0 0 1.25rem; }
.cart-empty__btn { background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.65rem 1.75rem; font-size: 0.8125rem; font-weight: 700; }

/* Grid */
.cart-grid { display: grid; grid-template-columns: 1fr 340px; gap: 1.5rem; align-items: start; }

.cart-lines { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 0.5rem 1.25rem; }
.cart-lines__head {
  display: grid; grid-template-columns: 1fr 7rem 9rem 7rem 2rem; gap: 1rem; align-items: center;
  padding: 0.875rem 0; border-bottom: 1px solid var(--color-border, #E5E7EB);
  font-size: 0.75rem; font-weight: 700; color: #6B7280; text-transform: uppercase; letter-spacing: 0.03em;
}
.cart-lines__h-price, .cart-lines__h-qty, .cart-lines__h-sub { text-align: center; }

.cart-row {
  display: grid; grid-template-columns: 1fr 7rem 9rem 7rem 2rem; gap: 1rem; align-items: center;
  padding: 1rem 0; border-bottom: 1px solid var(--color-border, #E5E7EB);
}
.cart-row__product { display: flex; align-items: center; gap: 0.875rem; min-width: 0; }
.cart-row__thumb { width: 4rem; height: 4rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; flex-shrink: 0; background: #F3F4F6; }
.cart-row__thumb img { width: 100%; height: 100%; object-fit: cover; }
.cart-row__name { font-size: 0.8125rem; color: #111827; text-decoration: none; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.cart-row__name:hover { color: var(--color-primary, #E8202A); }
.cart-row__brand { font-size: 0.6875rem; color: #9CA3AF; margin: 0.2rem 0 0; }
.cart-row__price, .cart-row__sub { text-align: center; font-size: 0.875rem; color: #111827; }
.cart-row__sub { font-weight: 700; color: var(--color-primary, #E8202A); }
.cart-row__qty { display: flex; justify-content: center; }
.cart-row__remove { background: none; border: none; cursor: pointer; color: #9CA3AF; display: flex; align-items: center; justify-content: center; }
.cart-row__remove:hover { color: var(--color-primary, #E8202A); }
.cart-row__remove :deep(svg) { width: 1.1rem; height: 1.1rem; }

.qty { display: inline-flex; align-items: center; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; }
.qty__btn { width: 2rem; height: 2.1rem; border: none; background: #fff; cursor: pointer; color: #6B7280; display: flex; align-items: center; justify-content: center; }
.qty__btn:hover { background: #F3F4F6; }
.qty__btn :deep(svg) { width: 0.8125rem; height: 0.8125rem; }
.qty__val { min-width: 2.25rem; text-align: center; font-size: 0.875rem; font-weight: 600; color: #111827; }

.cart-lines__foot { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem 0; flex-wrap: wrap; }
.cart-lines__continue, .cart-lines__clear { display: inline-flex; align-items: center; gap: 0.4rem; background: none; border: none; cursor: pointer; font-size: 0.8125rem; font-weight: 600; text-decoration: none; }
.cart-lines__continue { color: #6B7280; }
.cart-lines__continue:hover { color: #111827; }
.cart-lines__clear { color: var(--color-primary, #E8202A); }
.cart-lines__continue :deep(svg), .cart-lines__clear :deep(svg) { width: 1rem; height: 1rem; }

/* Summary */
.summary { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; position: sticky; top: 1rem; }
.summary__title { font-size: 1rem; font-weight: 700; color: #111827; margin: 0 0 1rem; }
.summary__coupon { display: flex; gap: 0.5rem; margin-bottom: 0.5rem; }
.summary__coupon-input { flex: 1; min-width: 0; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.55rem 0.75rem; font-size: 0.8125rem; outline: none; }
.summary__coupon-btn { background: #111827; color: #fff; border: none; border-radius: 0.375rem; padding: 0 1rem; font-size: 0.75rem; font-weight: 700; cursor: pointer; }
.summary__coupon-msg { font-size: 0.75rem; color: var(--color-primary, #E8202A); margin: 0 0 0.5rem; }
.summary__row { display: flex; justify-content: space-between; font-size: 0.875rem; color: #6B7280; padding: 0.6rem 0; border-top: 1px solid var(--color-border, #E5E7EB); }
.summary__row:first-of-type { margin-top: 0.5rem; }
.summary__hint { font-size: 0.75rem; color: #16A34A; margin: 0.25rem 0 0; }
.summary__total { display: flex; justify-content: space-between; font-size: 1rem; font-weight: 800; color: #111827; padding: 0.875rem 0; border-top: 2px solid #111827; margin-top: 0.5rem; }
.summary__checkout { display: flex; align-items: center; justify-content: center; gap: 0.5rem; background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.75rem 1rem; font-size: 0.8125rem; font-weight: 700; }
.summary__checkout:hover { background: var(--color-primary-hover, #C8181F); }
.summary__checkout-icon { width: 0.9375rem; height: 0.9375rem; }

@media (max-width: 900px) {
  .cart-grid { grid-template-columns: 1fr; }
  .summary { position: static; }
}
@media (max-width: 680px) {
  .cart-lines__head { display: none; }
  .cart-row { grid-template-columns: 1fr auto; grid-template-areas: 'product remove' 'price price' 'qty qty' 'sub sub'; gap: 0.625rem; }
  .cart-row__product { grid-area: product; }
  .cart-row__remove { grid-area: remove; align-self: start; }
  .cart-row__price, .cart-row__sub { grid-area: price; text-align: left; display: flex; justify-content: space-between; }
  .cart-row__sub { grid-area: sub; }
  .cart-row__qty { grid-area: qty; justify-content: flex-start; }
  .cart-row__price::before, .cart-row__sub::before { content: attr(data-label); color: #9CA3AF; font-size: 0.75rem; }
}
</style>
