<script setup lang="ts">
import type { Product } from '~/apps/market/types'

const props = withDefaults(defineProps<{
  product: Product
  /** Large card variant (rating row + Add to Cart). Defaults to product.detailed. */
  detailed?: boolean
}>(), {})

const { format } = useCurrency()
const { open } = useQuickView()
const { add, openCart } = useCart()
const { has: inWishlist, toggle: toggleWishlist } = useWishlist()
const { $t } = useNuxtApp()

const isDetailed = computed(() => props.detailed || props.product.detailed || false)
const badges = computed(() => props.product.badges ?? [])
const isWished = computed(() => inWishlist(props.product.id))

function addToCart() {
  add(props.product)
  openCart()
}
</script>

<template>
  <article class="pc" :class="{ 'pc--detailed': isDetailed }">
    <!-- Media -->
    <div class="pc__media">
      <!-- Badges -->
      <div class="pc__badges">
        <span
          v-for="(b, i) in badges" :key="i"
          class="pc__badge" :class="`pc__badge--${b.variant ?? 'discount'}`"
        >{{ b.text }}</span>
      </div>

      <!-- Image (click opens quick view); falls back to an icon when none provided -->
      <button class="pc__img" type="button" aria-label="View product details" @click="open(product)">
        <img v-if="product.image" :src="product.image" :alt="product.title" class="pc__img-photo" loading="lazy" />
        <UIcon v-else name="i-heroicons-cube" class="pc__img-icon" />
      </button>

      <!-- Hover actions -->
      <div class="pc__actions">
        <button
          class="pc__action" :class="{ 'pc__action--on': isWished }"
          :aria-label="isWished ? 'Remove from wishlist' : 'Add to wishlist'"
          :aria-pressed="isWished"
          @click="toggleWishlist(product)"
        >
          <UIcon :name="isWished ? 'i-heroicons-heart-solid' : 'i-heroicons-heart'" />
        </button>
        <button class="pc__action" aria-label="Add to cart" @click="addToCart"><UIcon name="i-heroicons-shopping-cart" /></button>
        <button class="pc__action" aria-label="Quick view" @click="open(product)"><UIcon name="i-heroicons-eye" /></button>
      </div>
    </div>

    <!-- Body -->
    <div class="pc__body">
      <!-- Rating (detailed: above title) -->
      <div v-if="isDetailed && product.rating != null" class="pc__rating">
        <span class="pc__stars">
          <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                 class="pc__star" :class="{ 'pc__star--on': n <= Math.round(product.rating) }" />
        </span>
        <span v-if="product.reviews != null" class="pc__reviews">({{ product.reviews }})</span>
      </div>

      <h3 class="pc__title" @click="open(product)">{{ product.title }}</h3>

      <p v-if="product.vendor" class="pc__vendor">{{ product.vendor }}</p>
      <p v-if="product.vendorLocation" class="pc__vendor-loc">
        {{ product.vendorLocation }} <UIcon name="i-heroicons-map-pin-solid" class="pc__pin" />
        <span v-if="product.stock" class="pc__stock">{{ product.stock }}</span>
      </p>

      <div class="pc__foot">
        <div class="pc__prices">
          <span v-if="product.oldPrice != null" class="pc__old">{{ format(product.oldPrice) }}</span>
          <span class="pc__price">{{ format(product.price) }}</span>
        </div>

        <!-- Compact rating (non-detailed cards that pass a rating) -->
        <div v-if="!isDetailed && product.rating != null" class="pc__rating pc__rating--compact">
          <span class="pc__stars">
            <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                   class="pc__star" :class="{ 'pc__star--on': n <= Math.round(product.rating) }" />
          </span>
          <span v-if="product.reviews != null" class="pc__reviews">({{ product.reviews }})</span>
        </div>
      </div>

      <!-- Detailed add-to-cart row -->
      <div v-if="isDetailed" class="pc__buy">
        <button
          class="pc__icon-btn" :class="{ 'pc__icon-btn--on': isWished }"
          :aria-label="isWished ? 'Remove from wishlist' : 'Add to wishlist'"
          :aria-pressed="isWished"
          @click="toggleWishlist(product)"
        >
          <UIcon :name="isWished ? 'i-heroicons-heart-solid' : 'i-heroicons-heart'" />
        </button>
        <button class="pc__addcart" :disabled="product.soldOut" @click="addToCart">
          <UIcon name="i-heroicons-shopping-cart" class="pc__addcart-icon" />
          {{ $t('market.card.addToCart') }}
        </button>
        <button class="pc__icon-btn" aria-label="Quick view" @click="open(product)"><UIcon name="i-heroicons-eye" /></button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.pc {
  background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem;
  overflow: hidden; display: flex; flex-direction: column; transition: box-shadow 0.15s, border-color 0.15s;
}
.pc:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: #D1D5DB; }

/* Media */
.pc__media { position: relative; padding: 1rem; }
.pc__badges { position: absolute; top: 0.625rem; left: 0.625rem; z-index: 2; display: flex; flex-direction: column; gap: 0.375rem; align-items: flex-start; }
.pc__badge { font-size: 0.625rem; font-weight: 700; padding: 0.15rem 0.45rem; border-radius: 0.25rem; color: #fff; letter-spacing: 0.02em; }
.pc__badge--discount { background: #FEE2E2; color: var(--color-primary, #E8202A); }
.pc__badge--hot  { background: var(--color-primary, #E8202A); }
.pc__badge--best { background: #FEE2E2; color: var(--color-primary, #E8202A); }
.pc__badge--sale { background: #16A34A; }
.pc__badge--sold { background: #7F1D1D; }

.pc__img {
  width: 100%; aspect-ratio: 1 / 1; border: none; padding: 0; cursor: pointer;
  border-radius: 0.375rem; background: #F3F4F6; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}
.pc__img-photo { width: 100%; height: 100%; object-fit: cover; display: block; }
.pc__img-icon { width: 2.5rem; height: 2.5rem; color: #CBD0D6; }

.pc__actions {
  position: absolute; top: 50%; right: 1.25rem; transform: translateY(-50%);
  display: flex; flex-direction: column; gap: 0.5rem; opacity: 0; transition: opacity 0.15s;
}
.pc:hover .pc__actions { opacity: 1; }
.pc__action {
  width: 2rem; height: 2rem; border-radius: 50%; border: none; cursor: pointer;
  background: var(--color-primary, #E8202A); color: #fff;
  display: flex; align-items: center; justify-content: center;
}
.pc__action:hover { background: var(--color-primary-hover, #C8181F); }
.pc__action--on { background: #111827; }
.pc__action--on:hover { background: #111827; }
.pc__action :deep(svg) { width: 1rem; height: 1rem; }

/* Body */
.pc__body { padding: 0 1rem 1rem; display: flex; flex-direction: column; gap: 0.375rem; }
.pc__title { font-size: 0.8125rem; font-weight: 600; color: #111827; margin: 0; line-height: 1.35; cursor: pointer;
             display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pc__title:hover { color: var(--color-primary, #E8202A); }
.pc__vendor { font-size: 0.6875rem; color: #6B7280; margin: 0; }
.pc__vendor-loc { font-size: 0.6875rem; color: #6B7280; margin: 0; display: flex; align-items: center; gap: 0.25rem; }
.pc__pin { width: 0.75rem; height: 0.75rem; color: var(--color-primary, #E8202A); }
.pc__stock { color: var(--color-primary, #E8202A); margin-left: auto; }

.pc__foot { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-top: 0.25rem; }
.pc__prices { display: flex; align-items: baseline; gap: 0.5rem; flex-wrap: wrap; }
.pc__old { font-size: 0.75rem; color: #9CA3AF; text-decoration: line-through; }
.pc__price { font-size: 0.9375rem; font-weight: 700; color: var(--color-primary, #E8202A); }

.pc__rating { display: flex; align-items: center; gap: 0.375rem; }
.pc__stars { display: inline-flex; gap: 1px; }
.pc__star { width: 0.8125rem; height: 0.8125rem; color: #E5E7EB; }
.pc__star--on { color: #F59E0B; }
.pc__reviews { font-size: 0.6875rem; color: #9CA3AF; }

/* Detailed buy row */
.pc__buy { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.5rem; }
.pc__icon-btn {
  width: 2.25rem; height: 2.25rem; border-radius: 0.375rem; flex-shrink: 0;
  border: 1px solid #FECACA; background: #FEF2F2; color: var(--color-primary, #E8202A);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.pc__icon-btn--on { background: var(--color-primary, #E8202A); color: #fff; border-color: var(--color-primary, #E8202A); }
.pc__icon-btn :deep(svg) { width: 1rem; height: 1rem; }
.pc__addcart {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.375rem;
  background: #FEF2F2; color: var(--color-primary, #E8202A); border: 1px solid #FECACA;
  border-radius: 0.375rem; padding: 0.5rem; font-size: 0.75rem; font-weight: 700; cursor: pointer;
}
.pc__addcart:hover:not(:disabled) { background: var(--color-primary, #E8202A); color: #fff; }
.pc__addcart:disabled { opacity: 0.5; cursor: not-allowed; }
.pc__addcart-icon { width: 1rem; height: 1rem; }

/* Mobile: tighter padding + smaller text so two cards fit a row and stay readable */
@media (max-width: 640px) {
  .pc__media { padding: 0.55rem; }
  .pc__body { padding: 0 0.6rem 0.7rem; gap: 0.3rem; }
  .pc__title { font-size: 0.72rem; line-height: 1.3; }
  .pc__vendor, .pc__vendor-loc { font-size: 0.625rem; }
  .pc__price { font-size: 0.82rem; }
  .pc__old { font-size: 0.68rem; }
  .pc__badge { font-size: 0.56rem; padding: 0.12rem 0.35rem; }
  .pc__star { width: 0.72rem; height: 0.72rem; }
  .pc__reviews { font-size: 0.625rem; }
  .pc__buy { gap: 0.35rem; margin-top: 0.4rem; }
  .pc__addcart { font-size: 0.625rem; padding: 0.45rem 0.3rem; gap: 0.25rem; }
  .pc__addcart-icon { width: 0.85rem; height: 0.85rem; }
  .pc__icon-btn { width: 2rem; height: 2rem; }
  .pc__icon-btn :deep(svg) { width: 0.9rem; height: 0.9rem; }
}
</style>
