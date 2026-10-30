<script setup lang="ts">
import type { Product } from '~/apps/market/types'

const query = ref('')
const showResults = ref(false)
const rootEl = ref<HTMLElement | null>(null)

const { format } = useCurrency()
const { open: openQuickView } = useQuickView()
const { $t } = useNuxtApp()

// Search over the products currently loaded (Best Deals + Featured). When the
// backend is wired, these lists come from the API, so this covers the catalog.
const { products: bestDeals } = useProducts('bestDeals')
const { products: featured } = useProducts('featured')

const allProducts = computed<Product[]>(() => {
  const byId = new Map<Product['id'], Product>()
  for (const p of [...bestDeals.value, ...featured.value]) byId.set(p.id, p)
  return [...byId.values()]
})

const results = computed<Product[]>(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return []
  return allProducts.value
    .filter(p =>
      p.title.toLowerCase().includes(q) ||
      p.brand?.toLowerCase().includes(q) ||
      p.category?.toLowerCase().includes(q),
    )
    .slice(0, 6)
})

function selectProduct(p: Product) {
  openQuickView(p)
  query.value = ''
  showResults.value = false
}

function onSubmit() {
  const q = query.value.trim()
  if (!q) return
  showResults.value = false
  navigateTo({ path: '/shop', query: { q } })
}

// Close the dropdown when clicking outside the search box.
onMounted(() => {
  const handler = (e: PointerEvent) => {
    if (rootEl.value && !rootEl.value.contains(e.target as Node)) showResults.value = false
  }
  window.addEventListener('pointerdown', handler)
  onBeforeUnmount(() => window.removeEventListener('pointerdown', handler))
})
</script>

<template>
  <div class="search-header">
    <NuxtLink to="/" class="brand-logo" aria-label="Doch Market — home">
      <img src="/images/logos/Logo-03-trim.png" alt="Doch Market" class="brand-logo__img" />
    </NuxtLink>

    <div ref="rootEl" class="search-box">
      <form class="search-wrap" @submit.prevent="onSubmit">
        <input
          v-model="query"
          type="text"
          class="search-input"
          :placeholder="$t('market.search.placeholder')"
          :aria-label="$t('market.search.placeholder')"
          @focus="showResults = true"
          @input="showResults = true"
          @keydown.escape="showResults = false"
        />
        <button type="submit" class="search-btn" aria-label="Search">
          <UIcon name="i-heroicons-magnifying-glass" class="search-btn__icon" />
        </button>
      </form>

      <!-- Live results -->
      <div v-if="showResults && query.trim()" class="search-results">
        <button
          v-for="p in results" :key="p.id"
          type="button" class="search-result"
          @click="selectProduct(p)"
        >
          <span class="search-result__thumb">
            <img v-if="p.image" :src="p.image" :alt="p.title" class="search-result__img" />
          </span>
          <span class="search-result__text">
            <span class="search-result__title">{{ p.title }}</span>
            <span class="search-result__price">{{ format(p.price) }}</span>
          </span>
        </button>

        <p v-if="!results.length" class="search-empty">{{ $t('market.search.noResults', { q: query }) }}</p>
      </div>
    </div>

    <!-- Account actions -->
    <div class="account-actions">
      <MarketCartMenu />
      <MarketWishlistMenu />
      <NuxtLink to="/account" class="account-btn" aria-label="Account">
        <UIcon name="i-heroicons-user" class="account-btn__icon" />
      </NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.search-header {
  background: var(--color-primary, #E8202A);
  display: flex; align-items: center; justify-content: space-between; gap: 1.5rem;
  padding: 1rem 1.5rem 1.25rem;
}

/* Brand logo — white logo variant reads directly on the red bar */
.brand-logo { display: flex; align-items: center; flex-shrink: 0; }
.brand-logo__img { height: 2.75rem; width: auto; display: block; }

/* Search box (relative anchor for the dropdown) */
.search-box { position: relative; width: 100%; max-width: 520px; }
.search-wrap {
  display: flex; align-items: stretch;
  width: 100%;
  background: #fff; border-radius: 0.375rem; overflow: hidden;
}
.search-input {
  flex: 1; border: none; outline: none;
  padding: 0.625rem 1rem; font-size: 0.875rem; color: #111; min-width: 0;
}
.search-input::placeholder { color: #9CA3AF; }
.search-btn {
  background: #fff; border: none; cursor: pointer;
  padding: 0 0.875rem; display: flex; align-items: center; justify-content: center;
}
.search-btn__icon { width: 1.125rem; height: 1.125rem; color: #6B7280; }

/* Results dropdown */
.search-results {
  position: absolute; top: calc(100% + 0.5rem); left: 0; right: 0; z-index: 50;
  background: #fff; border-radius: 0.5rem; box-shadow: 0 12px 32px rgba(0,0,0,0.18);
  padding: 0.375rem; max-height: 22rem; overflow-y: auto;
}
.search-result {
  display: flex; align-items: center; gap: 0.75rem; width: 100%; text-align: left;
  background: none; border: none; cursor: pointer; padding: 0.5rem; border-radius: 0.375rem;
}
.search-result:hover { background: #F3F4F6; }
.search-result__thumb {
  width: 2.75rem; height: 2.75rem; border-radius: 0.375rem; background: #F3F4F6; overflow: hidden; flex-shrink: 0;
}
.search-result__img { width: 100%; height: 100%; object-fit: cover; }
.search-result__text { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
.search-result__title {
  font-size: 0.8125rem; color: #111827; line-height: 1.3;
  display: -webkit-box; -webkit-line-clamp: 1; line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden;
}
.search-result__price { font-size: 0.8125rem; font-weight: 700; color: var(--color-primary, #E8202A); }
.search-empty { font-size: 0.8125rem; color: #6B7280; text-align: center; padding: 1rem 0.5rem; margin: 0; }

/* Account */
.account-actions { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.account-btn {
  background: transparent; border: none; cursor: pointer;
  width: 2.25rem; height: 2.25rem; border-radius: 0.5rem;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.account-btn:hover { background: rgba(255,255,255,0.15); }
.account-btn__icon { width: 1.25rem; height: 1.25rem; color: #fff; }

@media (max-width: 767px) {
  /* Wrap: row 1 = small logo + account actions; row 2 = full-width search.
     This keeps the logo, cart, wishlist and profile all visible on phones
     (previously the full-width search box pushed the actions off-screen). */
  .search-header { flex-wrap: wrap; gap: 0.55rem 0.75rem; padding: 0.65rem 1rem 0.8rem; }
  .brand-logo { display: flex; }
  .brand-logo__img { height: 1.7rem; }
  .account-actions { order: 2; margin-left: auto; }
  .search-box { order: 3; flex-basis: 100%; width: 100%; max-width: none; }
}
</style>
