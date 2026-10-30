<script setup lang="ts">
useHead({ title: 'Order Confirmed — Doch Market' })

const { lastOrder } = useOrder()
const { format } = useCurrency()
const { $t } = useNuxtApp()

const placedDate = computed(() =>
  lastOrder.value
    ? new Date(lastOrder.value.placedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })
    : '')

// Rough ETA: 3 days out from when the order was placed.
const eta = computed(() => {
  if (!lastOrder.value) return ''
  const d = new Date(lastOrder.value.placedAt + 3 * 86400000)
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })
})
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.order.thankYou') }]" />

    <section class="os">
      <div class="os__inner">
        <!-- No order in state (e.g. direct visit / refresh) -->
        <div v-if="!lastOrder" class="os-none">
          <UIcon name="i-heroicons-shopping-bag" class="os-none__icon" />
          <p class="os-none__title">{{ $t('market.order.noneTitle') }}</p>
          <p class="os-none__text">{{ $t('market.order.noneText') }}</p>
          <NuxtLink to="/shop" class="os-btn os-btn--primary">{{ $t('market.order.goToShop') }}</NuxtLink>
        </div>

        <template v-else>
          <div class="os-hero">
            <div class="os-check"><UIcon name="i-heroicons-check" class="os-check__icon" /></div>
            <h1 class="os-hero__title">{{ $t('market.order.thankYou') }}</h1>
            <p class="os-hero__text" v-html="$t('market.order.placedText', { ref: `<strong>${lastOrder.reference}</strong>`, email: `<strong>${lastOrder.customer.email}</strong>` })"></p>
          </div>

          <!-- Meta strip -->
          <div class="os-meta">
            <div class="os-meta__item">
              <span class="os-meta__label">{{ $t('market.order.orderNumber') }}</span>
              <span class="os-meta__val">{{ lastOrder.reference }}</span>
            </div>
            <div class="os-meta__item">
              <span class="os-meta__label">{{ $t('market.order.date') }}</span>
              <span class="os-meta__val">{{ placedDate }}</span>
            </div>
            <div class="os-meta__item">
              <span class="os-meta__label">{{ $t('market.order.payment') }}</span>
              <span class="os-meta__val">{{ lastOrder.payment }}</span>
            </div>
            <div class="os-meta__item">
              <span class="os-meta__label">{{ $t('market.order.estDelivery') }}</span>
              <span class="os-meta__val">{{ eta }}</span>
            </div>
          </div>

          <div class="os-grid">
            <!-- Items -->
            <div class="os-card">
              <h2 class="os-card__title">{{ $t('market.order.summary') }}</h2>
              <div v-for="item in lastOrder.items" :key="item.product.id" class="os-line">
                <span class="os-line__thumb">
                  <img v-if="item.product.image" :src="item.product.image" :alt="item.product.title" />
                </span>
                <div class="os-line__info">
                  <p class="os-line__name">{{ item.product.title }}</p>
                  <p class="os-line__qty">{{ $t('market.order.qty', { n: item.qty }) }}</p>
                </div>
                <span class="os-line__price">{{ format(item.product.price * item.qty) }}</span>
              </div>

              <div class="os-totals">
                <div class="os-totals__row"><span>{{ $t('market.order.subTotal') }}</span><span>{{ format(lastOrder.subtotal) }}</span></div>
                <div class="os-totals__row"><span>{{ $t('market.order.shipping') }}</span><span>{{ lastOrder.shipping === 0 ? $t('market.order.free') : format(lastOrder.shipping) }}</span></div>
                <div class="os-totals__total"><span>{{ $t('market.order.total') }}</span><span>{{ format(lastOrder.total) }}</span></div>
              </div>
            </div>

            <!-- Delivery -->
            <div class="os-card">
              <h2 class="os-card__title">{{ $t('market.order.deliveryTo') }}</h2>
              <p class="os-addr__name">{{ lastOrder.customer.name }}</p>
              <p class="os-addr__line">{{ lastOrder.customer.address }}</p>
              <p class="os-addr__line">{{ lastOrder.customer.city }}, {{ lastOrder.customer.region }}</p>
              <p class="os-addr__line">{{ lastOrder.customer.phone }}</p>
              <p class="os-addr__line">{{ lastOrder.customer.email }}</p>
            </div>
          </div>

          <div class="os-actions">
            <NuxtLink to="/shop" class="os-btn os-btn--primary">{{ $t('market.order.continueShopping') }}</NuxtLink>
            <NuxtLink to="/account" class="os-btn os-btn--ghost">{{ $t('market.order.viewOrders') }}</NuxtLink>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<style scoped>
.os__inner { max-width: 880px; margin: 0 auto; padding: 2.5rem 1.5rem 3.5rem; }

/* None */
.os-none { text-align: center; padding: 3rem 1rem; }
.os-none__icon { width: 3rem; height: 3rem; color: #D1D5DB; }
.os-none__title { font-size: 1.25rem; font-weight: 700; color: #374151; margin: 1rem 0 0.5rem; }
.os-none__text { font-size: 0.875rem; color: #9CA3AF; margin: 0 0 1.5rem; }

/* Hero */
.os-hero { text-align: center; margin-bottom: 2rem; }
.os-check { width: 4rem; height: 4rem; border-radius: 50%; background: #DCFCE7; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem; }
.os-check__icon { width: 2rem; height: 2rem; color: #16A34A; }
.os-hero__title { font-size: 1.625rem; font-weight: 800; color: #111827; margin: 0 0 0.625rem; }
.os-hero__text { font-size: 0.9375rem; color: #6B7280; margin: 0 auto; max-width: 34rem; line-height: 1.6; }

/* Meta */
.os-meta { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; margin-bottom: 1.5rem; }
.os-meta__item { display: flex; flex-direction: column; gap: 0.25rem; }
.os-meta__label { font-size: 0.6875rem; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.03em; }
.os-meta__val { font-size: 0.875rem; font-weight: 700; color: #111827; }

/* Cards */
.os-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 1.5rem; align-items: start; margin-bottom: 1.75rem; }
.os-card { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem 1.5rem; }
.os-card__title { font-size: 1rem; font-weight: 700; color: #111827; margin: 0 0 1rem; }

.os-line { display: flex; align-items: center; gap: 0.875rem; padding: 0.75rem 0; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.os-line:first-of-type { padding-top: 0; }
.os-line__thumb { width: 3rem; height: 3rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; flex-shrink: 0; background: #F3F4F6; }
.os-line__thumb img { width: 100%; height: 100%; object-fit: cover; }
.os-line__info { flex: 1; min-width: 0; }
.os-line__name { font-size: 0.8125rem; color: #111827; margin: 0; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.os-line__qty { font-size: 0.75rem; color: #9CA3AF; margin: 0.2rem 0 0; }
.os-line__price { font-size: 0.875rem; font-weight: 700; color: #111827; white-space: nowrap; }

.os-totals { margin-top: 1rem; }
.os-totals__row { display: flex; justify-content: space-between; font-size: 0.875rem; color: #6B7280; padding: 0.45rem 0; }
.os-totals__total { display: flex; justify-content: space-between; font-size: 1rem; font-weight: 800; color: #111827; padding: 0.75rem 0 0; border-top: 2px solid #111827; margin-top: 0.4rem; }

.os-addr__name { font-size: 0.875rem; font-weight: 700; color: #111827; margin: 0 0 0.5rem; }
.os-addr__line { font-size: 0.8125rem; color: #6B7280; margin: 0 0 0.35rem; }

/* Actions */
.os-actions { display: flex; gap: 0.75rem; justify-content: center; flex-wrap: wrap; }
.os-btn { text-decoration: none; border-radius: 0.375rem; padding: 0.7rem 1.75rem; font-size: 0.8125rem; font-weight: 700; }
.os-btn--primary { background: var(--color-primary, #E8202A); color: #fff; }
.os-btn--primary:hover { background: var(--color-primary-hover, #C8181F); }
.os-btn--ghost { background: #fff; color: #111827; border: 1px solid var(--color-border, #E5E7EB); }
.os-btn--ghost:hover { border-color: #111827; }

@media (max-width: 720px) {
  .os-meta { grid-template-columns: 1fr 1fr; }
  .os-grid { grid-template-columns: 1fr; }
}
</style>
