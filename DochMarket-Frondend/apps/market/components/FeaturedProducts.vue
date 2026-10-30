<script setup lang="ts">
const tabs = ['All Product', 'Smart Phone', 'Laptop', 'Headphone', 'TV'] as const
type Tab = typeof tabs[number]
const activeTab = ref<Tab>('All Product')

// Products come from the data layer (mock now, live API once the backend is set).
const { products } = useProducts('featured')

const filtered = computed(() =>
  activeTab.value === 'All Product'
    ? products.value
    : products.value.filter(p => p.category === activeTab.value)
)
</script>

<template>
  <section class="featured">
    <div class="featured__inner">
      <!-- Yellow promo sidebar -->
      <aside class="promo">
        <p class="promo__eyebrow">COMPUTER &amp; ACCESSORIES</p>
        <p class="promo__discount">32% Discount</p>
        <p class="promo__for">For all electronics products</p>
        <p class="promo__offer">Offers ends in <span class="promo__offer-pill">ENDS OF CHRISTMAS</span></p>

        <div class="promo__img">
          <UIcon name="i-heroicons-squares-2x2" class="promo__img-icon" />
        </div>

        <NuxtLink to="/shop" class="promo__cta">
          SHOP NOW <UIcon name="i-heroicons-arrow-right" class="promo__cta-icon" />
        </NuxtLink>
      </aside>

      <!-- Products -->
      <div class="featured__main">
        <div class="featured__head">
          <h2 class="featured__title">Featured Products</h2>
          <div class="featured__tabs">
            <button
              v-for="t in tabs" :key="t"
              class="featured__tab" :class="{ 'featured__tab--active': activeTab === t }"
              @click="activeTab = t"
            >{{ t }}</button>
            <NuxtLink to="/shop" class="featured__browse">Browse All Product <UIcon name="i-heroicons-arrow-right" class="featured__browse-icon" /></NuxtLink>
          </div>
        </div>

        <div class="featured__grid">
          <MarketProductCard v-for="p in filtered" :key="p.id" :product="p" />
          <p v-if="filtered.length === 0" class="featured__empty">No products in this category.</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.featured { background: #fff; }
.featured__inner { max-width: 1280px; margin: 0 auto; padding: 1rem 1.5rem 2.5rem;
  display: grid; grid-template-columns: 230px 1fr; gap: 1.25rem; align-items: start; }

/* Promo */
.promo { background: #F4C430; border-radius: 0.5rem; padding: 1.5rem 1.25rem; }
.promo__eyebrow { font-size: 0.6875rem; font-weight: 700; color: var(--color-primary, #E8202A); margin: 0 0 0.5rem; letter-spacing: 0.03em; }
.promo__discount { font-size: 1.5rem; font-weight: 800; color: #111827; margin: 0 0 0.25rem; }
.promo__for { font-size: 0.8125rem; color: #4B5563; margin: 0 0 0.875rem; }
.promo__offer { font-size: 0.75rem; color: #4B5563; margin: 0 0 1.25rem; display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.promo__offer-pill { background: #fff; color: #111827; font-weight: 700; padding: 0.25rem 0.5rem; border-radius: 0.25rem; font-size: 0.6875rem; }
.promo__img { aspect-ratio: 1 / 1; background: rgba(0,0,0,0.06); border-radius: 0.5rem; display: flex; align-items: center; justify-content: center; margin-bottom: 1.25rem; }
.promo__img-icon { width: 3rem; height: 3rem; color: rgba(0,0,0,0.25); }
.promo__cta { display: inline-flex; align-items: center; gap: 0.5rem; background: var(--color-primary, #E8202A);
  color: #fff; text-decoration: none; font-weight: 700; font-size: 0.8125rem; padding: 0.625rem 1.25rem; border-radius: 0.375rem; }
.promo__cta:hover { background: var(--color-primary-hover, #C8181F); }
.promo__cta-icon { width: 1rem; height: 1rem; }

/* Header + tabs */
.featured__head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
.featured__title { font-size: 1.125rem; font-weight: 700; color: #111827; margin: 0; }
.featured__tabs { display: flex; align-items: center; gap: 1.25rem; flex-wrap: wrap; }
.featured__tab { background: none; border: none; cursor: pointer; font-size: 0.8125rem; color: #6B7280;
  padding: 0.25rem 0; border-bottom: 2px solid transparent; }
.featured__tab--active { color: #111827; font-weight: 600; border-bottom-color: var(--color-primary, #E8202A); }
.featured__browse { display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.8125rem; font-weight: 600;
  color: var(--color-primary, #E8202A); text-decoration: none; }
.featured__browse-icon { width: 0.875rem; height: 0.875rem; }

.featured__grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.featured__empty { grid-column: 1 / -1; text-align: center; color: #9CA3AF; padding: 2rem 0; font-size: 0.875rem; }

@media (max-width: 1023px) {
  .featured__inner { grid-template-columns: 1fr; }
  .promo { display: grid; grid-template-columns: 1fr 1fr; gap: 0 1.25rem; align-items: center; }
  .promo__img { grid-row: span 2; }
}
@media (max-width: 767px) {
  .featured__grid { grid-template-columns: 1fr 1fr; gap: 0.7rem; }
  .promo { grid-template-columns: 1fr; }
  .promo__img { grid-row: auto; }
}
</style>
