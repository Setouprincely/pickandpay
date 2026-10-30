<script setup lang="ts">
import type { Product } from '~/apps/market/types'

useHead({ title: 'Shop — Doch Market' })

const route = useRoute()
const router = useRouter()
const { format } = useCurrency()
const { products, categories, brands, load, pending } = useCatalog()
onMounted(load)
const { $t } = useNuxtApp()

// ── Filter state (category/search seeded from the URL query) ──
const selectedCategories = ref<string[]>(
  route.query.category ? [String(route.query.category)] : [],
)
const selectedBrands = ref<string[]>([])
const minRating = ref(0)
const maxPrice = ref<number | null>(null)
const query = ref(route.query.q ? String(route.query.q) : '')
const sort = ref<'featured' | 'price-asc' | 'price-desc' | 'rating' | 'discount'>('featured')

// Keep the search box in sync if the user navigates here from the header search.
watch(() => route.query.q, v => { query.value = v ? String(v) : '' })
watch(() => route.query.category, v => {
  selectedCategories.value = v ? [String(v)] : []
})

// Highest catalog price → upper bound for the price slider.
const priceCeiling = computed(() =>
  Math.max(1000, ...products.value.map(p => p.price)))

function toggle(list: Ref<string[]>, value: string) {
  list.value = list.value.includes(value)
    ? list.value.filter(v => v !== value)
    : [...list.value, value]
}

function discountOf(p: Product) {
  if (p.discountPercent) return p.discountPercent
  if (p.oldPrice && p.price) return Math.round((1 - p.price / p.oldPrice) * 100)
  return 0
}

const filtered = computed<Product[]>(() => {
  const q = query.value.trim().toLowerCase()
  let list = products.value.filter((p) => {
    if (selectedCategories.value.length && !selectedCategories.value.includes(p.category ?? '')) return false
    if (selectedBrands.value.length && !selectedBrands.value.includes(p.brand ?? '')) return false
    if (minRating.value && (p.rating ?? 0) < minRating.value) return false
    if (maxPrice.value != null && p.price > maxPrice.value) return false
    if (q && !(
      p.title.toLowerCase().includes(q) ||
      p.brand?.toLowerCase().includes(q) ||
      p.category?.toLowerCase().includes(q)
    )) return false
    return true
  })

  switch (sort.value) {
    case 'price-asc': list = [...list].sort((a, b) => a.price - b.price); break
    case 'price-desc': list = [...list].sort((a, b) => b.price - a.price); break
    case 'rating': list = [...list].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0)); break
    case 'discount': list = [...list].sort((a, b) => discountOf(b) - discountOf(a)); break
  }
  return list
})

const hasFilters = computed(() =>
  selectedCategories.value.length > 0 ||
  selectedBrands.value.length > 0 ||
  minRating.value > 0 ||
  maxPrice.value != null ||
  query.value.trim() !== '')

function clearFilters() {
  selectedCategories.value = []
  selectedBrands.value = []
  minRating.value = 0
  maxPrice.value = null
  query.value = ''
  router.replace({ query: {} })
}
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.shop.breadcrumb') }]" />

    <section class="shop">
      <div class="shop__inner">
        <!-- Sidebar filters -->
        <aside class="filters">
          <div class="filters__head">
            <h2 class="filters__title">{{ $t('market.shop.filters') }}</h2>
            <button v-if="hasFilters" class="filters__clear" @click="clearFilters">{{ $t('market.shop.clearAll') }}</button>
          </div>

          <div class="filters__group">
            <p class="filters__label">{{ $t('market.shop.category') }}</p>
            <label v-for="c in categories" :key="c" class="check">
              <input type="checkbox" :checked="selectedCategories.includes(c)" @change="toggle(selectedCategories, c)" />
              <span>{{ c }}</span>
            </label>
            <p v-if="!categories.length" class="filters__none">{{ $t('market.shop.noCategories') }}</p>
          </div>

          <div v-if="brands.length" class="filters__group">
            <p class="filters__label">{{ $t('market.shop.brand') }}</p>
            <label v-for="b in brands" :key="b" class="check">
              <input type="checkbox" :checked="selectedBrands.includes(b)" @change="toggle(selectedBrands, b)" />
              <span>{{ b }}</span>
            </label>
          </div>

          <div class="filters__group">
            <p class="filters__label">{{ $t('market.shop.maxPrice') }}</p>
            <input
              v-model.number="maxPrice" type="range" min="1000" :max="priceCeiling" step="1000"
              class="filters__range"
            />
            <p class="filters__price">{{ maxPrice != null ? format(maxPrice) : $t('market.shop.anyPrice') }}</p>
          </div>

          <div class="filters__group">
            <p class="filters__label">{{ $t('market.shop.rating') }}</p>
            <button
              v-for="r in [4, 3, 2, 1]" :key="r"
              class="rate-row" :class="{ 'rate-row--on': minRating === r }"
              @click="minRating = minRating === r ? 0 : r"
            >
              <span class="rate-stars">
                <UIcon v-for="n in 5" :key="n" name="i-heroicons-star-solid"
                       class="rate-star" :class="{ 'rate-star--on': n <= r }" />
              </span>
              <span class="rate-text">{{ $t('market.shop.andUp') }}</span>
            </button>
          </div>
        </aside>

        <!-- Results -->
        <div class="results">
          <div class="results__bar">
            <p class="results__count">
              <strong>{{ filtered.length }}</strong> {{ filtered.length === 1 ? $t('market.shop.product') : $t('market.shop.products') }}
              <span v-if="query.trim()"> {{ $t('market.shop.forQuery', { q: query }) }}</span>
            </p>
            <label class="sort">
              {{ $t('market.shop.sortBy') }}
              <select v-model="sort" class="sort__select">
                <option value="featured">{{ $t('market.shop.sortFeatured') }}</option>
                <option value="price-asc">{{ $t('market.shop.sortPriceAsc') }}</option>
                <option value="price-desc">{{ $t('market.shop.sortPriceDesc') }}</option>
                <option value="rating">{{ $t('market.shop.sortRating') }}</option>
                <option value="discount">{{ $t('market.shop.sortDiscount') }}</option>
              </select>
            </label>
          </div>

          <div v-if="filtered.length" class="results__grid">
            <MarketProductCard v-for="p in filtered" :key="p.id" :product="p" />
          </div>

          <div v-else class="results__empty">
            <UIcon name="i-heroicons-magnifying-glass" class="results__empty-icon" />
            <p class="results__empty-title">{{ $t('market.shop.noMatch') }}</p>
            <button class="results__empty-btn" @click="clearFilters">{{ $t('market.shop.clearFilters') }}</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.shop__inner {
  max-width: 1280px; margin: 0 auto; padding: 1.75rem 1.5rem 3rem;
  display: grid; grid-template-columns: 250px 1fr; gap: 1.5rem; align-items: start;
}

/* Filters */
.filters { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; position: sticky; top: 1rem; }
.filters__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.filters__title { font-size: 1rem; font-weight: 700; color: #111827; margin: 0; }
.filters__clear { background: none; border: none; cursor: pointer; color: var(--color-primary, #E8202A); font-size: 0.75rem; font-weight: 600; }
.filters__group { padding: 1rem 0; border-top: 1px solid var(--color-border, #E5E7EB); }
.filters__label { font-size: 0.8125rem; font-weight: 700; color: #111827; margin: 0 0 0.625rem; }
.filters__none { font-size: 0.75rem; color: #9CA3AF; margin: 0; }

.check { display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem; color: #4B5563; padding: 0.2rem 0; cursor: pointer; }
.check input { accent-color: var(--color-primary, #E8202A); width: 0.9rem; height: 0.9rem; cursor: pointer; }

.filters__range { width: 100%; accent-color: var(--color-primary, #E8202A); cursor: pointer; }
.filters__price { font-size: 0.8125rem; font-weight: 600; color: var(--color-primary, #E8202A); margin: 0.4rem 0 0; }

.rate-row { display: flex; align-items: center; gap: 0.4rem; width: 100%; background: none; border: none; cursor: pointer; padding: 0.25rem 0; }
.rate-row--on .rate-text { color: var(--color-primary, #E8202A); font-weight: 600; }
.rate-stars { display: inline-flex; gap: 1px; }
.rate-star { width: 0.8125rem; height: 0.8125rem; color: #E5E7EB; }
.rate-star--on { color: #F59E0B; }
.rate-text { font-size: 0.75rem; color: #6B7280; }

/* Results */
.results__bar {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem;
  padding: 0.75rem 1rem; margin-bottom: 1.25rem;
}
.results__count { font-size: 0.8125rem; color: #6B7280; margin: 0; }
.results__count strong { color: #111827; }
.sort { font-size: 0.8125rem; color: #6B7280; display: inline-flex; align-items: center; gap: 0.5rem; }
.sort__select { border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.4rem 0.625rem; font-size: 0.8125rem; color: #111827; background: #fff; cursor: pointer; outline: none; }

.results__grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }

.results__empty { text-align: center; padding: 4rem 1rem; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; }
.results__empty-icon { width: 2.5rem; height: 2.5rem; color: #D1D5DB; }
.results__empty-title { font-size: 0.9375rem; color: #374151; margin: 0.75rem 0 1rem; }
.results__empty-btn { background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem; padding: 0.5rem 1.25rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }

@media (max-width: 1100px) { .results__grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 900px) {
  .shop__inner { grid-template-columns: 1fr; }
  .filters { position: static; }
}
@media (max-width: 640px) { .results__grid { grid-template-columns: 1fr 1fr; gap: 0.7rem; } }
</style>
