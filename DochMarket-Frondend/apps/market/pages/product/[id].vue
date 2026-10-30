<script setup lang="ts">
const route = useRoute()
const id = computed(() => route.params.id as string)

const product = useProduct(id)
const { products, load } = useCatalog()
onMounted(load)
const { format } = useCurrency()
const { add, openCart } = useCart()
const { has: inWishlist, toggle: toggleWishlist } = useWishlist()

useHead(() => ({ title: product.value ? `${product.value.title} — Doch Market` : 'Product — Doch Market' }))

const qty = ref(1)
const activeImg = ref(0)
const activeColor = ref(0)
const activeTab = ref<'description' | 'specs' | 'reviews'>('description')

// Reset transient state when navigating between products.
watch(product, () => { qty.value = 1; activeImg.value = 0; activeColor.value = 0; activeTab.value = 'description' })

const gallery = computed(() => {
  const p = product.value
  if (!p) return []
  return p.gallery?.length ? p.gallery : (p.image ? [p.image] : [])
})
const thumbs = computed(() => gallery.value.length > 1 ? gallery.value : Array(4).fill(gallery.value[0]).filter(Boolean))

const discount = computed(() => {
  const p = product.value
  if (!p) return null
  if (p.discountPercent) return p.discountPercent
  if (p.oldPrice && p.price) return Math.round((1 - p.price / p.oldPrice) * 100)
  return null
})

const sku = computed(() => product.value?.sku ?? `A${String(product.value?.id ?? '').toUpperCase()}`)
const availability = computed(() => product.value?.availability ?? (product.value?.soldOut ? 'Out of Stock' : 'In Stock'))
const category = computed(() => product.value?.category ?? 'Electronics Devices')
const feedback = computed(() => product.value?.userFeedback ?? product.value?.reviews)
const rating = computed(() => product.value?.rating ?? 5)
const colors = computed(() => product.value?.colors ?? ['#1F2937', '#9CA3AF'])
const isWished = computed(() => !!product.value && inWishlist(product.value.id))

const specs = computed(() => {
  const p = product.value
  if (!p) return []
  return [
    ['Brand', p.brand],
    ['SKU', sku.value],
    ['Category', category.value],
    ['Availability', availability.value],
    ['Memory', p.memoryOptions?.join(', ')],
    ['Storage', p.storageOptions?.join(', ')],
    ['Vendor', p.vendor],
  ].filter(([, v]) => !!v) as [string, string][]
})

const related = computed(() =>
  products.value.filter(p => p.id !== product.value?.id).slice(0, 5))

function addToCart() {
  if (product.value) { add(product.value, qty.value); openCart() }
}
function buyNow() {
  if (product.value) { add(product.value, qty.value); navigateTo('/checkout') }
}

const shares = [
  { icon: 'i-simple-icons-facebook', label: 'Facebook' },
  { icon: 'i-simple-icons-x', label: 'X' },
  { icon: 'i-simple-icons-pinterest', label: 'Pinterest' },
]
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[
      { label: 'Shop', to: '/shop' },
      { label: product?.title ?? 'Product' },
    ]" />

    <!-- Not found -->
    <section v-if="!product" class="pd-missing">
      <UIcon name="i-heroicons-cube" class="pd-missing__icon" />
      <p class="pd-missing__title">Product not found</p>
      <NuxtLink to="/shop" class="pd-missing__btn">Back to shop</NuxtLink>
    </section>

    <template v-else>
      <section class="pd">
        <div class="pd__inner">
          <!-- Gallery -->
          <div class="pd-gallery">
            <div class="pd-main">
              <img v-if="gallery[activeImg]" :src="gallery[activeImg]" :alt="product.title" class="pd-main__img" />
              <UIcon v-else name="i-heroicons-cube" class="pd-main__icon" />
              <span v-if="discount" class="pd-main__badge">{{ discount }}% OFF</span>
            </div>
            <div v-if="thumbs.length" class="pd-thumbs">
              <button
                v-for="(g, i) in thumbs" :key="i"
                class="pd-thumb" :class="{ 'pd-thumb--active': i === activeImg }"
                @click="activeImg = Math.min(i, gallery.length - 1)"
              >
                <img :src="g" :alt="`${product.title} ${i + 1}`" class="pd-thumb__img" />
              </button>
            </div>
          </div>

          <!-- Info -->
          <div class="pd-info">
            <div class="pd-rating">
              <span class="pd-stars">
                <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                       class="pd-star" :class="{ 'pd-star--on': n <= Math.round(rating) }" />
              </span>
              <span class="pd-rating__num">{{ rating }} Star Rating</span>
              <span v-if="feedback != null" class="pd-rating__count">({{ feedback }} reviews)</span>
            </div>

            <h1 class="pd-title">{{ product.title }}</h1>

            <div class="pd-meta">
              <p><span class="pd-meta__label">SKU:</span> <span class="pd-meta__val">{{ sku }}</span></p>
              <p><span class="pd-meta__label">Availability:</span>
                <span class="pd-meta__val" :class="product.soldOut ? 'pd-out' : 'pd-in'">{{ availability }}</span>
              </p>
              <p v-if="product.brand"><span class="pd-meta__label">Brand:</span> <span class="pd-meta__val">{{ product.brand }}</span></p>
              <p><span class="pd-meta__label">Category:</span> <span class="pd-meta__val">{{ category }}</span></p>
            </div>

            <div class="pd-price">
              <span class="pd-price__now">{{ format(product.price) }}</span>
              <span v-if="product.oldPrice != null" class="pd-price__old">{{ format(product.oldPrice) }}</span>
            </div>

            <hr class="pd-divider" />

            <!-- Options -->
            <div class="pd-options">
              <div class="pd-opt">
                <p class="pd-opt__label">Color</p>
                <div class="pd-swatches">
                  <button
                    v-for="(c, i) in colors" :key="i"
                    class="pd-swatch" :class="{ 'pd-swatch--active': i === activeColor }"
                    :style="{ background: c }" :aria-label="`Color ${i + 1}`"
                    @click="activeColor = i"
                  />
                </div>
              </div>
              <div v-if="product.sizes?.length" class="pd-opt">
                <p class="pd-opt__label">Size</p>
                <select class="pd-select"><option v-for="s in product.sizes" :key="s">{{ s }}</option></select>
              </div>
              <div v-if="product.memoryOptions?.length" class="pd-opt">
                <p class="pd-opt__label">Memory</p>
                <select class="pd-select"><option v-for="m in product.memoryOptions" :key="m">{{ m }}</option></select>
              </div>
              <div v-if="product.storageOptions?.length" class="pd-opt">
                <p class="pd-opt__label">Storage</p>
                <select class="pd-select"><option v-for="s in product.storageOptions" :key="s">{{ s }}</option></select>
              </div>
            </div>

            <!-- Buy row -->
            <div class="pd-buy">
              <div class="pd-qty">
                <button class="pd-qty__btn" aria-label="Decrease" @click="qty = Math.max(1, qty - 1)"><UIcon name="i-heroicons-minus" /></button>
                <span class="pd-qty__val">{{ String(qty).padStart(2, '0') }}</span>
                <button class="pd-qty__btn" aria-label="Increase" @click="qty++"><UIcon name="i-heroicons-plus" /></button>
              </div>
              <button class="pd-addcart" :disabled="product.soldOut" @click="addToCart">
                <UIcon name="i-heroicons-shopping-cart" class="pd-addcart__icon" /> ADD TO CART
              </button>
              <button class="pd-buynow" :disabled="product.soldOut" @click="buyNow">BUY NOW</button>
            </div>

            <!-- Secondary actions -->
            <div class="pd-foot">
              <button class="pd-link" :class="{ 'pd-link--on': isWished }" @click="toggleWishlist(product)">
                <UIcon :name="isWished ? 'i-heroicons-heart-solid' : 'i-heroicons-heart'" class="pd-link__icon" />
                {{ isWished ? 'In Wishlist' : 'Add to Wishlist' }}
              </button>
              <div class="pd-share">
                <span class="pd-share__label">Share:</span>
                <button v-for="s in shares" :key="s.label" class="pd-share__btn" :aria-label="s.label"><UIcon :name="s.icon" /></button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Tabs -->
      <section class="pd-tabs">
        <div class="pd-tabs__inner">
          <div class="pd-tabs__head">
            <button class="pd-tab" :class="{ 'pd-tab--on': activeTab === 'description' }" @click="activeTab = 'description'">Description</button>
            <button class="pd-tab" :class="{ 'pd-tab--on': activeTab === 'specs' }" @click="activeTab = 'specs'">Specifications</button>
            <button class="pd-tab" :class="{ 'pd-tab--on': activeTab === 'reviews' }" @click="activeTab = 'reviews'">Reviews</button>
          </div>

          <div class="pd-tabs__body">
            <div v-show="activeTab === 'description'" class="pd-desc">
              <p>
                The <strong>{{ product.title }}</strong> combines premium build quality with
                everyday reliability. Sourced from trusted vendors on Doch Market and delivered
                across Cameroon, it ships fast and is backed by our buyer-protection guarantee.
              </p>
              <ul class="pd-desc__list">
                <li>Genuine product with manufacturer warranty</li>
                <li>Fast, tracked delivery from {{ product.vendorLocation ?? 'local vendors' }}</li>
                <li>Secure payment — Mobile Money, card or cash on delivery</li>
                <li>Easy returns within 7 days</li>
              </ul>
            </div>

            <div v-show="activeTab === 'specs'" class="pd-specs">
              <table v-if="specs.length" class="pd-specs__table">
                <tr v-for="[k, v] in specs" :key="k">
                  <th>{{ k }}</th><td>{{ v }}</td>
                </tr>
              </table>
              <p v-else class="pd-specs__empty">No specifications available.</p>
            </div>

            <div v-show="activeTab === 'reviews'" class="pd-reviews">
              <div class="pd-reviews__summary">
                <span class="pd-reviews__score">{{ rating }}.0</span>
                <span class="pd-stars">
                  <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                         class="pd-star" :class="{ 'pd-star--on': n <= Math.round(rating) }" />
                </span>
                <span class="pd-reviews__count">Based on {{ feedback ?? 'recent' }} reviews</span>
              </div>
              <p class="pd-reviews__note">Sign in to leave a review for this product.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Related -->
      <section v-if="related.length" class="pd-related">
        <div class="pd-related__inner">
          <h2 class="pd-related__title">Related Products</h2>
          <div class="pd-related__grid">
            <MarketProductCard v-for="p in related" :key="p.id" :product="p" />
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* Not found */
.pd-missing { max-width: 1280px; margin: 0 auto; text-align: center; padding: 5rem 1.5rem; }
.pd-missing__icon { width: 3rem; height: 3rem; color: #D1D5DB; }
.pd-missing__title { font-size: 1.125rem; color: #374151; margin: 1rem 0 1.25rem; }
.pd-missing__btn { background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.6rem 1.5rem; font-size: 0.8125rem; font-weight: 700; }

.pd__inner {
  max-width: 1280px; margin: 0 auto; padding: 2rem 1.5rem;
  display: grid; grid-template-columns: 1fr 1.15fr; gap: 2.5rem; align-items: start;
}

/* Gallery */
.pd-main { position: relative; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; overflow: hidden; aspect-ratio: 1 / 1; display: flex; align-items: center; justify-content: center; background: #F9FAFB; }
.pd-main__img { width: 100%; height: 100%; object-fit: contain; }
.pd-main__icon { width: 5rem; height: 5rem; color: #CBD0D6; }
.pd-main__badge { position: absolute; top: 1rem; left: 1rem; background: var(--color-primary, #E8202A); color: #fff; font-size: 0.6875rem; font-weight: 700; padding: 0.25rem 0.6rem; border-radius: 0.25rem; }
.pd-thumbs { display: flex; gap: 0.625rem; margin-top: 0.875rem; }
.pd-thumb { width: 4rem; height: 4rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; cursor: pointer; padding: 0; background: #F9FAFB; flex-shrink: 0; }
.pd-thumb--active { border-color: var(--color-primary, #E8202A); }
.pd-thumb__img { width: 100%; height: 100%; object-fit: cover; }

/* Info */
.pd-rating { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.pd-stars { display: inline-flex; gap: 1px; }
.pd-star { width: 0.9375rem; height: 0.9375rem; color: #E5E7EB; }
.pd-star--on { color: #F59E0B; }
.pd-rating__num { font-size: 0.8125rem; font-weight: 600; color: #111827; }
.pd-rating__count { font-size: 0.8125rem; color: #9CA3AF; }
.pd-title { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0.75rem 0 1rem; line-height: 1.3; }

.pd-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 1rem; margin-bottom: 1.25rem; }
.pd-meta p { font-size: 0.8125rem; color: #6B7280; margin: 0; }
.pd-meta__label { color: #9CA3AF; }
.pd-meta__val { color: #111827; font-weight: 600; }
.pd-in { color: #16A34A; }
.pd-out { color: var(--color-primary, #E8202A); }

.pd-price { display: flex; align-items: baseline; gap: 0.875rem; margin-bottom: 1.25rem; }
.pd-price__now { font-size: 1.875rem; font-weight: 800; color: var(--color-primary, #E8202A); }
.pd-price__old { font-size: 1.0625rem; color: #9CA3AF; text-decoration: line-through; }

.pd-divider { border: none; border-top: 1px solid var(--color-border, #E5E7EB); margin: 0 0 1.25rem; }

.pd-options { display: flex; flex-wrap: wrap; gap: 1.25rem 1.5rem; margin-bottom: 1.5rem; }
.pd-opt__label { font-size: 0.75rem; color: #6B7280; margin: 0 0 0.5rem; }
.pd-swatches { display: flex; gap: 0.5rem; }
.pd-swatch { width: 1.6rem; height: 1.6rem; border-radius: 50%; cursor: pointer; border: 2px solid transparent; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.1); }
.pd-swatch--active { border-color: var(--color-primary, #E8202A); }
.pd-select { border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.5rem 0.625rem; font-size: 0.8125rem; color: #111827; background: #fff; outline: none; cursor: pointer; min-width: 8rem; }

.pd-buy { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
.pd-qty { display: flex; align-items: center; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; }
.pd-qty__btn { width: 2.25rem; height: 2.5rem; border: none; background: #fff; cursor: pointer; color: #6B7280; display: flex; align-items: center; justify-content: center; }
.pd-qty__btn:hover { background: #F3F4F6; }
.pd-qty__btn :deep(svg) { width: 0.875rem; height: 0.875rem; }
.pd-qty__val { min-width: 2.5rem; text-align: center; font-size: 0.9375rem; font-weight: 600; color: #111827; }
.pd-addcart { flex: 1; min-width: 160px; display: inline-flex; align-items: center; justify-content: center; gap: 0.5rem; background: #fff; color: var(--color-primary, #E8202A); border: 1px solid var(--color-primary, #E8202A); border-radius: 0.375rem; padding: 0.7rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.pd-addcart:hover:not(:disabled) { background: #FEF2F2; }
.pd-addcart__icon { width: 1rem; height: 1rem; }
.pd-buynow { background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem; padding: 0.7rem 2rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.pd-buynow:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.pd-addcart:disabled, .pd-buynow:disabled { opacity: 0.5; cursor: not-allowed; }

.pd-foot { display: flex; align-items: center; gap: 1.5rem; flex-wrap: wrap; border-top: 1px solid var(--color-border, #E5E7EB); padding-top: 1.25rem; }
.pd-link { display: inline-flex; align-items: center; gap: 0.4rem; background: none; border: none; cursor: pointer; font-size: 0.8125rem; color: #6B7280; }
.pd-link:hover, .pd-link--on { color: var(--color-primary, #E8202A); }
.pd-link__icon { width: 1.05rem; height: 1.05rem; }
.pd-share { display: inline-flex; align-items: center; gap: 0.5rem; margin-left: auto; }
.pd-share__label { font-size: 0.8125rem; color: #6B7280; }
.pd-share__btn { background: none; border: none; cursor: pointer; color: #6B7280; display: flex; }
.pd-share__btn:hover { color: var(--color-primary, #E8202A); }
.pd-share__btn :deep(svg) { width: 1rem; height: 1rem; }

/* Tabs */
.pd-tabs__inner { max-width: 1280px; margin: 0 auto; padding: 0 1.5rem 2rem; }
.pd-tabs__head { display: flex; gap: 1.75rem; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.pd-tab { background: none; border: none; cursor: pointer; font-size: 0.875rem; color: #6B7280; padding: 0.75rem 0; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.pd-tab--on { color: #111827; font-weight: 700; border-bottom-color: var(--color-primary, #E8202A); }
.pd-tabs__body { padding-top: 1.25rem; }
.pd-desc p { font-size: 0.875rem; color: #4B5563; line-height: 1.7; margin: 0 0 1rem; }
.pd-desc__list { margin: 0; padding-left: 1.1rem; color: #4B5563; font-size: 0.875rem; line-height: 1.9; }
.pd-specs__table { width: 100%; border-collapse: collapse; max-width: 640px; }
.pd-specs__table th, .pd-specs__table td { text-align: left; font-size: 0.8125rem; padding: 0.625rem 0.875rem; border: 1px solid var(--color-border, #E5E7EB); }
.pd-specs__table th { background: #F9FAFB; color: #6B7280; font-weight: 600; width: 35%; }
.pd-specs__table td { color: #111827; }
.pd-specs__empty { font-size: 0.875rem; color: #9CA3AF; }
.pd-reviews__summary { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
.pd-reviews__score { font-size: 1.5rem; font-weight: 800; color: #111827; }
.pd-reviews__count { font-size: 0.8125rem; color: #6B7280; }
.pd-reviews__note { font-size: 0.875rem; color: #9CA3AF; margin: 0; }

/* Related */
.pd-related__inner { max-width: 1280px; margin: 0 auto; padding: 1rem 1.5rem 3rem; }
.pd-related__title { font-size: 1.125rem; font-weight: 700; color: #111827; margin: 0 0 1.25rem; }
.pd-related__grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 1rem; }

@media (max-width: 1100px) { .pd-related__grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 900px) {
  .pd__inner { grid-template-columns: 1fr; gap: 1.5rem; }
}
@media (max-width: 640px) {
  .pd-meta { grid-template-columns: 1fr; }
  .pd-related__grid { grid-template-columns: 1fr 1fr; }
}
</style>
