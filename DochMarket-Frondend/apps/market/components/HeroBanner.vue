<script setup lang="ts">
// Swap these for a real <img :src="..."> once product imagery is available.
const slides = [
  {
    eyebrow: 'THE BEST PLACE TO VIEW',
    title: 'KOORUI 27-inch\nComputer Monitor',
    text: 'Save up to 50% on select Xbox games. Get 3 months of PC Game Pass for $2 USD.',
    discount: '29% OFF',
    oldPrice: 57000,
    price: 40470,
    image: '/images/market/products/hero-monitor.jpg',
  },
]
const active = ref(0)
const { format } = useCurrency()
</script>

<template>
  <section class="hero">
    <div class="hero__inner">
      <!-- Left: copy -->
      <div class="hero__copy">
        <p class="hero__eyebrow"><span class="hero__eyebrow-line" /> {{ slides[active].eyebrow }}</p>
        <h1 class="hero__title">{{ slides[active].title }}</h1>
        <p class="hero__text">{{ slides[active].text }}</p>

        <NuxtLink to="/shop" class="hero__cta">
          SHOP NOW
          <UIcon name="i-heroicons-arrow-right" class="hero__cta-icon" />
        </NuxtLink>

        <div class="hero__dots">
          <button
            v-for="(s, i) in slides.length === 1 ? 3 : slides.length" :key="i"
            class="hero__dot" :class="{ 'hero__dot--on': i === active }"
            :aria-label="`Slide ${i + 1}`"
            @click="active = Math.min(i, slides.length - 1)"
          />
        </div>
      </div>

      <!-- Right: product image (placeholder) -->
      <div class="hero__media">
        <span class="hero__discount">{{ slides[active].discount }}</span>

        <div class="hero__img-placeholder">
          <img
            v-if="slides[active].image"
            :src="slides[active].image"
            :alt="slides[active].title"
            class="hero__img"
          />
          <template v-else>
            <UIcon name="i-heroicons-computer-desktop" class="hero__img-icon" />
            <span class="hero__img-hint">Product image</span>
          </template>
        </div>

        <div class="hero__prices">
          <span class="hero__old">{{ format(slides[active].oldPrice) }}</span>
          <span class="hero__price">{{ format(slides[active].price) }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero { background: #ECEDEF; }
.hero__inner {
  max-width: 1280px; margin: 0 auto; padding: 2rem 1.5rem;
  display: grid; grid-template-columns: 1fr 1.2fr; gap: 1.5rem; align-items: center;
}

/* Copy */
.hero__eyebrow {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.05em;
  color: var(--color-primary, #E8202A); margin: 0 0 0.75rem; text-transform: uppercase;
}
.hero__eyebrow-line { width: 1.75rem; height: 2px; background: var(--color-primary, #E8202A); }
.hero__title {
  font-size: 2rem; font-weight: 800; line-height: 1.15; color: #111827; margin: 0 0 0.875rem;
  white-space: pre-line;
}
.hero__text { font-size: 0.875rem; color: #6B7280; line-height: 1.5; margin: 0 0 1.25rem; max-width: 22rem; }

.hero__cta {
  display: inline-flex; align-items: center; gap: 0.5rem;
  border: 1px solid var(--color-primary, #E8202A); color: var(--color-primary, #E8202A);
  background: #fff; text-decoration: none; font-weight: 700; font-size: 0.8125rem;
  padding: 0.625rem 1.25rem; border-radius: 0.375rem; transition: all 0.15s;
}
.hero__cta:hover { background: var(--color-primary, #E8202A); color: #fff; }
.hero__cta-icon { width: 1rem; height: 1rem; }

.hero__dots { display: flex; gap: 0.4rem; margin-top: 1.5rem; }
.hero__dot { width: 8px; height: 8px; border-radius: 50%; border: none; background: #C7CDD4; cursor: pointer; padding: 0; }
.hero__dot--on { background: #111827; }

/* Media */
.hero__media { position: relative; display: flex; flex-direction: column; align-items: flex-end; }
.hero__discount {
  position: absolute; top: 0; right: 0; z-index: 2;
  background: var(--color-primary, #E8202A); color: #fff; font-weight: 700; font-size: 0.875rem;
  padding: 0.5rem 1rem; border-radius: 0.25rem;
}
.hero__img-placeholder {
  width: 100%; aspect-ratio: 16 / 10; border-radius: 0.5rem; overflow: hidden;
  background: linear-gradient(135deg, #d9dbe0, #c3c6cd);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.5rem;
}
.hero__img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero__img-icon { width: 4rem; height: 4rem; color: #9aa0a8; }
.hero__img-hint { font-size: 0.75rem; color: #8b9099; }

.hero__prices { display: flex; align-items: baseline; gap: 0.625rem; margin-top: 0.5rem; }
.hero__old { font-size: 0.8125rem; color: #9CA3AF; text-decoration: line-through; }
.hero__price { font-size: 1.125rem; font-weight: 700; color: var(--color-primary, #E8202A); }

@media (max-width: 900px) {
  .hero__inner { grid-template-columns: 1fr; }
  .hero__title { font-size: 1.625rem; }
}
</style>
