<script setup lang="ts">
// Market storefront shell. The configurable Black Friday bar sits at the very top
// (toggle it in apps/market/config/announcement.ts).

// Load live exchange rates once (base = XAF, free no-key API). Prices across the
// store convert to the shopper's selected currency via useCurrency().
const { rates, ratesLoaded } = useCurrency()
const { data: fx } = useAsyncData(
  'market-fx-rates',
  () => $fetch<{ rates?: Record<string, number> }>('https://open.er-api.com/v6/latest/XAF'),
  { server: false, lazy: true },
)
watch(fx, (v) => {
  if (v?.rates) {
    rates.value = { ...v.rates, XAF: 1 }
    ratesLoaded.value = true
  }
})

// Persist the shopping cart + wishlist in the browser (survive reloads).
const { items: cartItems } = useCart()
const { items: wishlistItems } = useWishlist()
onMounted(() => {
  restore('market-cart', cartItems)
  restore('market-wishlist', wishlistItems)
})

function restore<T>(key: string, state: Ref<T>) {
  try {
    const saved = localStorage.getItem(key)
    if (saved) state.value = JSON.parse(saved)
  } catch { /* ignore */ }
  watch(state, (v) => {
    try { localStorage.setItem(key, JSON.stringify(v)) } catch { /* ignore */ }
  }, { deep: true })
}
</script>

<template>
  <div class="market-shell">
    <MarketAnnouncementBar />
    <MarketTopInfoBar />
    <MarketSearchHeader />
    <MarketCategoryNav />

    <main class="market-main">
      <slot />
    </main>

    <MarketSiteFooter />

    <!-- Product quick-view modal (opens when a product image is clicked) -->
    <MarketProductQuickView />

    <!-- Toast notifications (add-to-cart, wishlist, etc.) -->
    <MarketToaster />

    <!-- EN/FR language toggle (UI chrome only — product catalogue stays as sellers entered it) -->
    <DochLanguageToggle />
  </div>
</template>

<style scoped>
.market-shell { min-height: 100vh; display: flex; flex-direction: column; background: #fff; }
.market-main { flex: 1; }
</style>
