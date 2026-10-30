<script setup lang="ts">
useHead({ title: 'My Shipments — DOCH Xender' })

const { $t } = useNuxtApp()
const { user } = useAuth()
const { active, past, load, loading } = useShipments()
onMounted(load)
const { formatXAF, timeAgo } = useFormat()

const firstName = computed(() => user.value?.name?.split(/\s+/)[0] || 'there')
const hasAny = computed(() => active.value.length + past.value.length > 0)
</script>

<template>
  <div class="dash">
    <!-- Greeting + send CTA -->
    <section class="dash-hero">
      <div class="dash-hero__text">
        <h1 class="dash-hero__title">{{ $t('xender.dash.greeting', { name: firstName }) }}</h1>
        <p class="dash-hero__sub">{{ $t('xender.dash.subtitle') }}</p>
      </div>
      <NuxtLink to="/send" class="dash-hero__cta">
        <UIcon name="i-heroicons-paper-airplane" /> {{ $t('xender.dash.sendCta') }}
      </NuxtLink>
    </section>

    <!-- Empty state -->
    <section v-if="!hasAny" class="dash-empty">
      <div class="dash-empty__icon"><UIcon name="i-heroicons-cube" /></div>
      <h2 class="dash-empty__title">{{ $t('xender.dash.emptyTitle') }}</h2>
      <p class="dash-empty__sub">{{ $t('xender.dash.emptySub') }}</p>
      <NuxtLink to="/send" class="dash-empty__btn">
        <UIcon name="i-heroicons-plus" /> {{ $t('xender.dash.emptyBtn') }}
      </NuxtLink>
    </section>

    <!-- Active shipments -->
    <section v-if="active.length" class="dash-sec">
      <h2 class="dash-sec__title">{{ $t('xender.dash.active') }} <span class="dash-sec__count">{{ active.length }}</span></h2>
      <div class="dash-list">
        <NuxtLink v-for="s in active" :key="s.id" :to="`/shipments/${s.id}`" class="ship">
          <div class="ship__thumb">
            <img v-if="s.photo" :src="s.photo" alt="" />
            <UIcon v-else name="i-heroicons-cube" />
          </div>
          <div class="ship__body">
            <div class="ship__top">
              <span class="ship__ref">{{ s.ref }}</span>
              <XenderStatusBadge :status="s.status" size="sm" />
            </div>
            <p class="ship__route">
              <UIcon name="i-heroicons-map-pin" /> {{ s.pickup.name }}
              <UIcon name="i-heroicons-arrow-long-right" class="ship__arrow" /> {{ s.destination.name }}
            </p>
            <p class="ship__meta">{{ $t('xender.dash.to', { name: s.receiver.name }) }} · {{ formatXAF(s.price.total) }} · {{ timeAgo(s.createdAt) }}</p>
          </div>
          <UIcon name="i-heroicons-chevron-right" class="ship__chev" />
        </NuxtLink>
      </div>
    </section>

    <!-- Delivered history -->
    <section v-if="past.length" class="dash-sec">
      <h2 class="dash-sec__title">{{ $t('xender.dash.delivered') }}</h2>
      <div class="dash-list">
        <NuxtLink v-for="s in past" :key="s.id" :to="`/shipments/${s.id}`" class="ship ship--done">
          <div class="ship__thumb">
            <img v-if="s.photo" :src="s.photo" alt="" />
            <UIcon v-else name="i-heroicons-cube" />
          </div>
          <div class="ship__body">
            <div class="ship__top">
              <span class="ship__ref">{{ s.ref }}</span>
              <XenderStatusBadge :status="s.status" size="sm" />
            </div>
            <p class="ship__route">
              <UIcon name="i-heroicons-map-pin" /> {{ s.pickup.name }}
              <UIcon name="i-heroicons-arrow-long-right" class="ship__arrow" /> {{ s.destination.name }}
            </p>
            <p class="ship__meta">{{ $t('xender.dash.to', { name: s.receiver.name }) }} · {{ formatXAF(s.price.total) }} · {{ timeAgo(s.createdAt) }}</p>
          </div>
          <UIcon name="i-heroicons-chevron-right" class="ship__chev" />
        </NuxtLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 2rem; }

.dash-hero { position: relative; overflow: hidden; background: linear-gradient(135deg, #E8202A 0%, #B3151D 100%); border-radius: 1rem; padding: 1.75rem; color: #fff; display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.dash-hero::after { content: ''; position: absolute; width: 14rem; height: 14rem; border-radius: 50%; background: rgba(255,255,255,0.08); top: -5rem; right: -3rem; }
.dash-hero__text { position: relative; z-index: 1; }
.dash-hero__title { font-size: 1.5rem; font-weight: 800; margin: 0 0 0.3rem; }
.dash-hero__sub { font-size: 0.9rem; color: rgba(255,255,255,0.9); margin: 0; max-width: 32ch; }
.dash-hero__cta { position: relative; z-index: 1; display: inline-flex; align-items: center; gap: 0.5rem; background: #fff; color: #E8202A; font-weight: 700; font-size: 0.85rem; padding: 0.75rem 1.25rem; border-radius: 0.6rem; text-decoration: none; }
.dash-hero__cta:hover { background: #FFF1F1; }
.dash-hero__cta :deep(svg) { width: 1.1rem; height: 1.1rem; }

.dash-empty { text-align: center; background: #fff; border: 1px dashed #E5E7EB; border-radius: 1rem; padding: 3rem 1.5rem; }
.dash-empty__icon { width: 3.5rem; height: 3.5rem; margin: 0 auto 1rem; border-radius: 50%; background: #FEF2F2; color: #E8202A; display: flex; align-items: center; justify-content: center; }
.dash-empty__icon :deep(svg) { width: 1.8rem; height: 1.8rem; }
.dash-empty__title { font-size: 1.1rem; font-weight: 800; color: #111827; margin: 0 0 0.35rem; }
.dash-empty__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; }
.dash-empty__btn { display: inline-flex; align-items: center; gap: 0.5rem; background: #E8202A; color: #fff; font-weight: 700; font-size: 0.82rem; padding: 0.7rem 1.2rem; border-radius: 0.5rem; text-decoration: none; }
.dash-empty__btn:hover { background: #C8181F; }
.dash-empty__btn :deep(svg) { width: 1rem; height: 1rem; }

.dash-sec__title { font-size: 0.95rem; font-weight: 800; color: #111827; margin: 0 0 0.9rem; display: flex; align-items: center; gap: 0.5rem; }
.dash-sec__count { font-size: 0.7rem; font-weight: 700; color: #E8202A; background: #FEF2F2; border-radius: 999px; padding: 0.1rem 0.5rem; }
.dash-list { display: flex; flex-direction: column; gap: 0.75rem; }

.ship { display: flex; align-items: center; gap: 1rem; background: #fff; border: 1px solid #E5E7EB; border-radius: 0.75rem; padding: 0.9rem; text-decoration: none; transition: box-shadow 0.15s, border-color 0.15s; }
.ship:hover { border-color: #E8202A; box-shadow: 0 8px 20px -12px rgba(232,32,42,0.4); }
.ship--done { opacity: 0.85; }
.ship__thumb { flex-shrink: 0; width: 3.25rem; height: 3.25rem; border-radius: 0.6rem; background: #F3F4F6; display: flex; align-items: center; justify-content: center; overflow: hidden; color: #9CA3AF; }
.ship__thumb img { width: 100%; height: 100%; object-fit: cover; }
.ship__thumb :deep(svg) { width: 1.5rem; height: 1.5rem; }
.ship__body { flex: 1; min-width: 0; }
.ship__top { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.3rem; }
.ship__ref { font-size: 0.85rem; font-weight: 800; color: #111827; }
.ship__route { display: flex; align-items: center; gap: 0.3rem; font-size: 0.82rem; font-weight: 600; color: #374151; margin: 0 0 0.2rem; }
.ship__route :deep(svg) { width: 0.95rem; height: 0.95rem; color: #9CA3AF; }
.ship__arrow { color: #E8202A !important; }
.ship__meta { font-size: 0.73rem; color: #9CA3AF; margin: 0; }
.ship__chev { flex-shrink: 0; width: 1.2rem; height: 1.2rem; color: #D1D5DB; }

@media (max-width: 640px) {
  .dash { gap: 1.25rem; }
  .dash-hero { padding: 1.25rem; border-radius: 0.85rem; }
  .dash-hero__title { font-size: 1.2rem; }
  .dash-hero__sub { font-size: 0.8rem; }
  .dash-hero__cta { font-size: 0.8rem; padding: 0.6rem 1rem; }
  .dash-empty { padding: 2.25rem 1.25rem; }
  .dash-sec__title { font-size: 0.88rem; margin-bottom: 0.7rem; }
  .ship { padding: 0.7rem; gap: 0.7rem; border-radius: 0.65rem; }
  .ship__thumb { width: 2.75rem; height: 2.75rem; }
  .ship__ref { font-size: 0.8rem; }
  .ship__route { font-size: 0.76rem; }
  .ship__meta { font-size: 0.7rem; }
  .ship__chev { width: 1rem; height: 1rem; }
}
</style>
