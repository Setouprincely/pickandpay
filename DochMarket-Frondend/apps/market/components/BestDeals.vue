<script setup lang="ts">
// Newest published products from the real catalogue (no promotional countdown: DOCH has no deal end date).
const { products } = useProducts()
const featured = computed(() => products.value[0])
const deals = computed(() => products.value.slice(1, 9))
</script>

<template>
  <section class="deals">
    <div class="deals__inner">
      <!-- Header -->
      <div class="deals__head">
        <h2 class="deals__title">Latest products</h2>
        <NuxtLink to="/shop" class="deals__browse">Browse All Product <UIcon name="i-heroicons-arrow-right" class="deals__browse-icon" /></NuxtLink>
      </div>

      <!-- Grid: featured + 8 deals -->
      <div class="deals__grid">
        <MarketProductCard
          v-if="featured"
          class="deals__featured"
          detailed
          :product="featured"
        />
        <div class="deals__list">
          <MarketProductCard v-for="d in deals" :key="d.id" :product="d" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.deals { background: #fff; }
.deals__inner { max-width: 1280px; margin: 0 auto; padding: 2rem 1.5rem; }

.deals__head { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
.deals__title { font-size: 1.125rem; font-weight: 700; color: #111827; margin: 0; }
.deals__ends { font-size: 0.8125rem; color: #6B7280; }
.deals__timer {
  background: var(--color-primary, #E8202A); color: #fff; font-weight: 700; font-size: 0.8125rem;
  padding: 0.35rem 0.75rem; border-radius: 0.375rem; font-variant-numeric: tabular-nums;
}
.deals__browse { margin-left: auto; display: inline-flex; align-items: center; gap: 0.375rem;
  font-size: 0.8125rem; font-weight: 600; color: #2563EB; text-decoration: none; }
.deals__browse:hover { text-decoration: underline; }
.deals__browse-icon { width: 0.875rem; height: 0.875rem; }

.deals__grid { display: grid; grid-template-columns: 280px 1fr; gap: 1rem; align-items: start; }
.deals__list { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }

@media (max-width: 1100px) { .deals__list { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 900px) {
  .deals__grid { grid-template-columns: 1fr; }
  .deals__featured { max-width: 320px; }
  .deals__list { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 520px) { .deals__list { grid-template-columns: 1fr 1fr; } }
</style>