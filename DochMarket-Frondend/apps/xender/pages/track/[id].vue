<script setup lang="ts">
// Public tracking by tracking number (DCH-...) — no account needed.
// GET /api/tracking/{trackingNumber} returns status and customer-safe history only:
// no names, addresses, phone numbers or delivery code (the code is only shown to the
// signed-in sender in their shipment page).
definePageMeta({ layout: 'blank' })

const route = useRoute()
const config = useRuntimeConfig()
const tracking = computed(() => String(route.params.id).toUpperCase())

const data = ref<any | null>(null)
const loading = ref(true)
const notFound = ref(false)

const label = (s?: string) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')
const dt = (v?: string) => (v ? new Date(v).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '')

async function load() {
  loading.value = true
  try {
    data.value = await $fetch(`${config.public.apiBase}/api/tracking/${encodeURIComponent(tracking.value)}`)
    notFound.value = false
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)

useHead({ title: () => `Track ${tracking.value} — DOCH` })
</script>

<template>
  <div class="rt">
    <div v-if="loading" class="rt__missing"><p>Loading…</p></div>
    <div v-else-if="data" class="rt__card">
      <header class="rt-head">
        <img src="/images/logos/Logo-01-trim.png" alt="DOCH" class="rt-head__logo" />
        <span class="rt-status">{{ label(data.status) }}</span>
      </header>

      <h1 class="rt-title">Package {{ data.trackingNumber }}</h1>
      <p class="rt-sub">Last update {{ dt(data.lastUpdated) }}</p>

      <div v-if="data.status === 'DELIVERED'" class="rt-delivered">
        <UIcon name="i-heroicons-check-badge" />
        <p>Delivered</p>
      </div>

      <div class="rt-info">
        <div v-if="data.estimatedDeliveryTime && data.status !== 'DELIVERED'" class="rt-info__row"><span>Estimated delivery</span><strong>{{ dt(data.estimatedDeliveryTime) }}</strong></div>
        <div v-if="data.vehicleCategory" class="rt-info__row"><span>Vehicle</span><strong>{{ data.vehicleCategory }}</strong></div>
      </div>

      <ol class="rt-tl">
        <li v-for="(e, i) in data.events" :key="i" class="rt-tl__item rt-tl__item--done">
          <span class="rt-tl__dot" style="background:#16A34A;border-color:#16A34A"><UIcon name="i-heroicons-check" /></span>
          <span class="rt-tl__label">{{ e.message }}<template v-if="e.hubName"> ({{ e.hubName }})</template></span>
          <span class="rt-tl__time">{{ dt(e.occurredAt) }}</span>
        </li>
      </ol>

      <p class="rt-foot">The delivery code is shared only with the sender in their DOCH account. Give it to the driver only when you receive the package.</p>
      <p class="rt-foot">Powered by <strong>DOCH</strong></p>
    </div>

    <div v-else class="rt__missing">
      <UIcon name="i-heroicons-question-mark-circle" />
      <h1>No package found</h1>
      <p>Check the tracking number {{ tracking }} and try again.</p>
    </div>
  </div>
</template>

<style scoped>
.rt { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 1.5rem; }
.rt__card { width: 100%; max-width: 440px; background: #fff; border: 1px solid #E5E7EB; border-radius: 1rem; padding: 1.75rem; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.15); }
.rt-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
.rt-head__logo { height: 1.8rem; width: auto; }
.rt-title { font-size: 1.25rem; font-weight: 800; color: #111827; margin: 0 0 0.25rem; }
.rt-status { font-size: 0.75rem; font-weight: 700; background: #EFF6FF; color: #1D4ED8; padding: 0.2rem 0.6rem; border-radius: 999px; }
.rt-sub { font-size: 0.82rem; color: #6B7280; margin: 0 0 1.5rem; }

.rt-otp { background: linear-gradient(135deg, #E8202A, #B3151D); border-radius: 0.85rem; padding: 1.25rem; text-align: center; color: #fff; margin-bottom: 1.5rem; }
.rt-otp__label { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: rgba(255,255,255,0.8); margin: 0 0 0.5rem; }
.rt-otp__code { font-size: 2.1rem; font-weight: 800; letter-spacing: 0.3em; margin: 0 0 0.6rem; }
.rt-otp__hint { font-size: 0.73rem; color: rgba(255,255,255,0.85); margin: 0; line-height: 1.5; }

.rt-delivered { display: flex; flex-direction: column; align-items: center; gap: 0.4rem; background: #ECFDF5; border-radius: 0.85rem; padding: 1.5rem; margin-bottom: 1.5rem; }
.rt-delivered :deep(svg) { width: 2.4rem; height: 2.4rem; color: #16A34A; }
.rt-delivered p { font-size: 0.95rem; font-weight: 800; color: #065F46; margin: 0; }

.rt-info { border: 1px solid #F3F4F6; border-radius: 0.6rem; padding: 0.5rem 0.9rem; margin-bottom: 1.5rem; }
.rt-info__row { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; font-size: 0.8rem; padding: 0.5rem 0; border-bottom: 1px solid #F3F4F6; }
.rt-info__row:last-child { border-bottom: none; }
.rt-info__row span { color: #9CA3AF; }
.rt-info__row strong { color: #111827; font-weight: 600; text-align: right; }

.rt-tl { list-style: none; margin: 0 0 1.25rem; padding: 0; }
.rt-tl__item { display: flex; align-items: center; gap: 0.6rem; padding: 0.4rem 0; }
.rt-tl__dot { flex-shrink: 0; width: 1.2rem; height: 1.2rem; border-radius: 50%; background: #fff; border: 2px solid #E5E7EB; display: flex; align-items: center; justify-content: center; color: #fff; }
.rt-tl__dot :deep(svg) { width: 0.7rem; height: 0.7rem; }
.rt-tl__label { flex: 1; font-size: 0.8rem; font-weight: 600; color: #111827; }
.rt-tl__label--muted { color: #9CA3AF; }
.rt-tl__time { font-size: 0.7rem; color: #9CA3AF; }

.rt-foot { text-align: center; font-size: 0.72rem; color: #9CA3AF; margin: 0; }
.rt-foot strong { color: #E8202A; }

.rt__missing { text-align: center; color: #6B7280; }
.rt__missing :deep(svg) { width: 3rem; height: 3rem; color: #D1D5DB; }
.rt__missing h1 { font-size: 1.15rem; font-weight: 800; color: #111827; margin: 1rem 0 0.4rem; }
.rt__missing p { font-size: 0.85rem; margin: 0; }

@media (max-width: 640px) {
  .rt { padding: 0.85rem; }
  .rt__card { padding: 1.25rem; border-radius: 0.85rem; }
  .rt-title { font-size: 1.1rem; }
  .rt-sub { font-size: 0.78rem; margin-bottom: 1.1rem; }
  .rt-otp { padding: 1rem; margin-bottom: 1.1rem; }
  .rt-otp__code { font-size: 1.75rem; letter-spacing: 0.2em; }
  .rt-info { margin-bottom: 1.1rem; }
  .rt-info__row { font-size: 0.76rem; }
  .rt-tl__label { font-size: 0.76rem; }
}
</style>
