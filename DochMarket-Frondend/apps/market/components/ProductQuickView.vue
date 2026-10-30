<script setup lang="ts">
// Product detail / quick-view modal. Opened from any product card via useQuickView().
const { product, close } = useQuickView()
const { format } = useCurrency()
const { add, openCart } = useCart()
const { has: inWishlist, toggle: toggleWishlist } = useWishlist()
const { $t } = useNuxtApp()

const isWished = computed(() => !!product.value && inWishlist(product.value.id))

function addToCart() {
  if (product.value) { add(product.value, qty.value); openCart() }
}
function buyNow() {
  if (product.value) { add(product.value, qty.value); close(); openCart() }
}

const qty = ref(1)
const activeImg = ref(0)
const activeColor = ref(0)

// Reset transient state whenever a different product is opened.
watch(product, () => { qty.value = 1; activeImg.value = 0; activeColor.value = 0 })

const gallery = computed(() => {
  const p = product.value
  if (!p) return []
  return p.gallery?.length ? p.gallery : (p.image ? [p.image] : [])
})

const discount = computed(() => {
  const p = product.value
  if (!p) return null
  if (p.discountPercent) return p.discountPercent
  if (p.oldPrice && p.price) return Math.round((1 - p.price / p.oldPrice) * 100)
  return null
})

const sku = computed(() => product.value?.sku ?? `A${String(product.value?.id ?? '').toUpperCase()}`)
const availability = computed(() =>
  product.value?.availability ?? (product.value?.soldOut ? $t('market.quickView.outOfStock') : $t('market.quickView.inStock')))
const category = computed(() => product.value?.category ?? 'Electronics Devices')
const feedback = computed(() => product.value?.userFeedback ?? product.value?.reviews)
const rating = computed(() => product.value?.rating ?? 5)
const colors = computed(() => product.value?.colors ?? ['#1F2937', '#9CA3AF'])

const shares = [
  { icon: 'i-simple-icons-facebook', label: 'Facebook' },
  { icon: 'i-simple-icons-x', label: 'X' },
  { icon: 'i-simple-icons-pinterest', label: 'Pinterest' },
]
</script>

<template>
  <Teleport to="body">
    <Transition name="qv-fade">
      <div v-if="product" class="qv-overlay" @click.self="close">
        <button class="qv-close" aria-label="Close" @click="close">
          <UIcon name="i-heroicons-x-mark" class="qv-close__icon" />
        </button>

        <div class="qv-card">
          <!-- Gallery -->
          <div class="qv-gallery">
            <div class="qv-main">
              <img v-if="gallery[activeImg]" :src="gallery[activeImg]" :alt="product.title" class="qv-main__img" />
              <UIcon v-else name="i-heroicons-cube" class="qv-main__icon" />
            </div>
            <div v-if="gallery.length" class="qv-thumbs">
              <button
                v-for="(g, i) in (gallery.length > 1 ? gallery : [gallery[0], gallery[0], gallery[0], gallery[0], gallery[0]])"
                :key="i"
                class="qv-thumb" :class="{ 'qv-thumb--active': i === activeImg }"
                @click="activeImg = Math.min(i, gallery.length - 1)"
              >
                <img :src="g" :alt="`${product.title} ${i + 1}`" class="qv-thumb__img" />
              </button>
            </div>
          </div>

          <!-- Info -->
          <div class="qv-info">
            <div class="qv-rating">
              <span class="qv-stars">
                <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                       class="qv-star" :class="{ 'qv-star--on': n <= Math.round(rating) }" />
              </span>
              <span class="qv-rating__num">{{ $t('market.quickView.starRating', { n: rating }) }}</span>
              <span v-if="feedback != null" class="qv-rating__count">{{ $t('market.quickView.userFeedback', { n: feedback }) }}</span>
            </div>

            <h2 class="qv-title">{{ product.title }}</h2>

            <div class="qv-meta">
              <p><span class="qv-meta__label">{{ $t('market.quickView.sku') }}</span> <span class="qv-meta__val">{{ sku }}</span></p>
              <p><span class="qv-meta__label">{{ $t('market.quickView.availability') }}</span>
                <span class="qv-meta__val" :class="product.soldOut ? 'qv-out' : 'qv-in'">{{ availability }}</span>
              </p>
              <p v-if="product.brand"><span class="qv-meta__label">{{ $t('market.quickView.brand') }}</span> <span class="qv-meta__val">{{ product.brand }}</span></p>
              <p><span class="qv-meta__label">{{ $t('market.quickView.category') }}</span> <span class="qv-meta__val">{{ category }}</span></p>
            </div>

            <div class="qv-price">
              <span class="qv-price__now">{{ format(product.price) }}</span>
              <span v-if="product.oldPrice != null" class="qv-price__old">{{ format(product.oldPrice) }}</span>
              <span v-if="discount" class="qv-price__badge">{{ $t('market.quickView.off', { n: discount }) }}</span>
            </div>

            <hr class="qv-divider" />

            <!-- Options -->
            <div class="qv-options">
              <div class="qv-opt">
                <p class="qv-opt__label">{{ $t('market.quickView.color') }}</p>
                <div class="qv-swatches">
                  <button
                    v-for="(c, i) in colors" :key="i"
                    class="qv-swatch" :class="{ 'qv-swatch--active': i === activeColor }"
                    :style="{ background: c }" :aria-label="`Color ${i + 1}`"
                    @click="activeColor = i"
                  />
                </div>
              </div>

              <div v-if="product.sizes?.length" class="qv-opt">
                <p class="qv-opt__label">{{ $t('market.quickView.size') }}</p>
                <select class="qv-select"><option v-for="s in product.sizes" :key="s">{{ s }}</option></select>
              </div>

              <div v-if="product.memoryOptions?.length" class="qv-opt">
                <p class="qv-opt__label">{{ $t('market.quickView.memory') }}</p>
                <select class="qv-select"><option v-for="m in product.memoryOptions" :key="m">{{ m }}</option></select>
              </div>

              <div v-if="product.storageOptions?.length" class="qv-opt">
                <p class="qv-opt__label">{{ $t('market.quickView.storage') }}</p>
                <select class="qv-select"><option v-for="s in product.storageOptions" :key="s">{{ s }}</option></select>
              </div>
            </div>

            <NuxtLink :to="`/product/${product.id}`" class="qv-details" @click="close">
              {{ $t('market.quickView.viewDetails') }} <UIcon name="i-heroicons-arrow-right" class="qv-details__icon" />
            </NuxtLink>

            <!-- Buy row -->
            <div class="qv-buy">
              <div class="qv-qty">
                <button class="qv-qty__btn" aria-label="Decrease" @click="qty = Math.max(1, qty - 1)">
                  <UIcon name="i-heroicons-minus" />
                </button>
                <span class="qv-qty__val">{{ String(qty).padStart(2, '0') }}</span>
                <button class="qv-qty__btn" aria-label="Increase" @click="qty++">
                  <UIcon name="i-heroicons-plus" />
                </button>
              </div>
              <button class="qv-addcart" :disabled="product.soldOut" @click="addToCart">
                <UIcon name="i-heroicons-shopping-cart" class="qv-addcart__icon" />
                {{ $t('market.quickView.addToCart') }}
              </button>
              <button class="qv-buynow" :disabled="product.soldOut" @click="buyNow">{{ $t('market.quickView.buyNow') }}</button>
            </div>

            <!-- Footer -->
            <div class="qv-foot">
              <button class="qv-link" :class="{ 'qv-link--on': isWished }" @click="toggleWishlist(product)">
                <UIcon :name="isWished ? 'i-heroicons-heart-solid' : 'i-heroicons-heart'" class="qv-link__icon" />
                {{ isWished ? $t('market.quickView.inWishlist') : $t('market.quickView.addToWishlist') }}
              </button>
              <button class="qv-link"><UIcon name="i-heroicons-arrows-right-left" class="qv-link__icon" /> {{ $t('market.quickView.addToCompare') }}</button>
              <div class="qv-share">
                <span class="qv-share__label">{{ $t('market.quickView.shareProduct') }}</span>
                <button v-for="s in shares" :key="s.label" class="qv-share__btn" :aria-label="s.label">
                  <UIcon :name="s.icon" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.qv-overlay {
  position: fixed; inset: 0; z-index: 300; background: rgba(0,0,0,0.55);
  display: flex; align-items: center; justify-content: center; padding: 1.5rem;
}
.qv-close {
  position: absolute; top: 1.25rem; right: 1.5rem; z-index: 2;
  width: 2rem; height: 2rem; border-radius: 50%; border: none; cursor: pointer;
  background: #fff; color: var(--color-primary, #E8202A);
  display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.qv-close__icon { width: 1.125rem; height: 1.125rem; }

.qv-card {
  background: #fff; border-radius: 0.5rem; width: 100%; max-width: 880px;
  max-height: 88vh; overflow-y: auto; box-shadow: 0 24px 70px rgba(0,0,0,0.3);
  display: grid; grid-template-columns: 1fr 1.1fr; gap: 1.5rem; padding: 1.5rem;
}

/* Gallery */
.qv-main {
  border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; overflow: hidden;
  aspect-ratio: 1 / 1; display: flex; align-items: center; justify-content: center; background: #F9FAFB;
}
.qv-main__img { width: 100%; height: 100%; object-fit: contain; }
.qv-main__icon { width: 4rem; height: 4rem; color: #CBD0D6; }
.qv-thumbs { display: flex; gap: 0.5rem; margin-top: 0.75rem; }
.qv-thumb {
  width: 3.25rem; height: 3.25rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem;
  overflow: hidden; cursor: pointer; padding: 0; background: #F9FAFB; flex-shrink: 0;
}
.qv-thumb--active { border-color: var(--color-primary, #E8202A); }
.qv-thumb__img { width: 100%; height: 100%; object-fit: cover; }

/* Info */
.qv-rating { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.qv-stars { display: inline-flex; gap: 1px; }
.qv-star { width: 0.875rem; height: 0.875rem; color: #E5E7EB; }
.qv-star--on { color: #F59E0B; }
.qv-rating__num { font-size: 0.8125rem; font-weight: 600; color: #111827; }
.qv-rating__count { font-size: 0.75rem; color: #9CA3AF; }

.qv-title { font-size: 1.0625rem; font-weight: 700; color: #111827; margin: 0.625rem 0 0.875rem; line-height: 1.4; }

.qv-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 1rem; margin-bottom: 1rem; }
.qv-meta p { font-size: 0.8125rem; color: #6B7280; margin: 0; }
.qv-meta__label { color: #9CA3AF; }
.qv-meta__val { color: #111827; font-weight: 600; }
.qv-in { color: #16A34A; }
.qv-out { color: var(--color-primary, #E8202A); }

.qv-price { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 1rem; }
.qv-price__now { font-size: 1.375rem; font-weight: 800; color: var(--color-primary, #E8202A); }
.qv-price__old { font-size: 0.9375rem; color: #9CA3AF; text-decoration: line-through; }
.qv-price__badge { background: var(--color-primary, #E8202A); color: #fff; font-size: 0.6875rem; font-weight: 700; padding: 0.2rem 0.5rem; border-radius: 0.25rem; }

.qv-divider { border: none; border-top: 1px solid var(--color-border, #E5E7EB); margin: 0 0 1rem; }

/* Options */
.qv-options { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.25rem; }
.qv-opt__label { font-size: 0.75rem; color: #6B7280; margin: 0 0 0.5rem; }
.qv-swatches { display: flex; gap: 0.5rem; }
.qv-swatch { width: 1.5rem; height: 1.5rem; border-radius: 50%; cursor: pointer; border: 2px solid transparent; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.1); }
.qv-swatch--active { border-color: var(--color-primary, #E8202A); }
.qv-select {
  width: 100%; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem;
  padding: 0.5rem 0.625rem; font-size: 0.8125rem; color: #111827; background: #fff; outline: none; cursor: pointer;
}

.qv-details { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.8125rem; font-weight: 600; color: var(--color-primary, #E8202A); text-decoration: none; margin-bottom: 1rem; }
.qv-details:hover { text-decoration: underline; }
.qv-details__icon { width: 0.875rem; height: 0.875rem; }

/* Buy row */
.qv-buy { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 1rem; flex-wrap: wrap; }
.qv-qty { display: flex; align-items: center; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; }
.qv-qty__btn { width: 2rem; height: 2.25rem; border: none; background: #fff; cursor: pointer; color: #6B7280; display: flex; align-items: center; justify-content: center; }
.qv-qty__btn:hover { background: #F3F4F6; }
.qv-qty__btn :deep(svg) { width: 0.875rem; height: 0.875rem; }
.qv-qty__val { min-width: 2.25rem; text-align: center; font-size: 0.875rem; font-weight: 600; color: #111827; }

.qv-addcart {
  flex: 1; min-width: 150px; display: inline-flex; align-items: center; justify-content: center; gap: 0.5rem;
  background: #fff; color: var(--color-primary, #E8202A); border: 1px solid var(--color-primary, #E8202A);
  border-radius: 0.375rem; padding: 0.625rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer;
}
.qv-addcart:hover:not(:disabled) { background: #FEF2F2; }
.qv-addcart__icon { width: 1rem; height: 1rem; }
.qv-buynow {
  background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem;
  padding: 0.625rem 1.5rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer;
}
.qv-buynow:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.qv-addcart:disabled, .qv-buynow:disabled { opacity: 0.5; cursor: not-allowed; }

/* Footer */
.qv-foot { display: flex; align-items: center; gap: 1.25rem; flex-wrap: wrap; border-top: 1px solid var(--color-border, #E5E7EB); padding-top: 1rem; }
.qv-link { display: inline-flex; align-items: center; gap: 0.375rem; background: none; border: none; cursor: pointer; font-size: 0.8125rem; color: #6B7280; }
.qv-link:hover { color: var(--color-primary, #E8202A); }
.qv-link--on { color: var(--color-primary, #E8202A); font-weight: 600; }
.qv-link__icon { width: 1rem; height: 1rem; }
.qv-share { display: inline-flex; align-items: center; gap: 0.5rem; margin-left: auto; }
.qv-share__label { font-size: 0.8125rem; color: #6B7280; }
.qv-share__btn { background: none; border: none; cursor: pointer; color: #6B7280; display: flex; }
.qv-share__btn:hover { color: var(--color-primary, #E8202A); }
.qv-share__btn :deep(svg) { width: 1rem; height: 1rem; }

/* Transition */
.qv-fade-enter-active, .qv-fade-leave-active { transition: opacity 0.2s ease; }
.qv-fade-enter-from, .qv-fade-leave-to { opacity: 0; }

/* Responsive */
@media (max-width: 768px) {
  .qv-card { grid-template-columns: 1fr; max-height: 92vh; gap: 1rem; padding: 1.1rem; }
  .qv-close { top: 0.75rem; right: 0.75rem; }
  /* Don't let the photo dominate the modal on phones: shrink it and drop the
     thumbnail strip entirely. */
  .qv-main { aspect-ratio: auto; height: 10.5rem; }
  .qv-thumbs { display: none; }
  .qv-title { font-size: 0.95rem; margin: 0.4rem 0 0.6rem; }
  .qv-meta { margin-bottom: 0.75rem; }
  .qv-price { margin-bottom: 0.75rem; }
  .qv-price__now { font-size: 1.2rem; }
}
@media (max-width: 480px) {
  .qv-meta, .qv-options { grid-template-columns: 1fr; }
  .qv-overlay { padding: 0.6rem; }
  .qv-main { height: 8.5rem; }
  .qv-options { gap: 0.75rem; margin-bottom: 1rem; }
  .qv-foot { gap: 0.85rem; }
  .qv-share { margin-left: 0; }
}
</style>
