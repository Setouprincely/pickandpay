<script setup lang="ts">
import { useFormat } from '../../composables/useFormat'
import { STATUS_ORDER, STATUS_META, useShipments } from '../../stores/shipments'

const { $t } = useNuxtApp()
const route = useRoute()
const { fetchOne, paymentStatus } = useShipments()
const { notify } = useNotify()
const { formatXAF, formatDuration, formatPhone, timeAgo } = useFormat()

const id = computed(() => String(route.params.id))
const shipment = ref<any>(null)
const isNew = computed(() => route.query.new === '1')

async function loadShipment() {
  shipment.value = await fetchOne(id.value)
}

async function pollPayment(paymentId: string) {
  for (let tries = 0; tries < 30; tries++) {
    const res = await paymentStatus(paymentId)
    const st = res?.status
    if (st === 'COMPLETED') {
      notify({ title: 'Payment confirmed', message: 'Your package is waiting for a driver.', variant: 'success' })
      break
    }
    if (st === 'FAILED' || st === 'CANCELLED') {
      notify({ title: 'Payment failed', message: res?.failureReason || 'Please try paying again.', variant: 'error' })
      break
    }
    await new Promise(r => setTimeout(r, 10000))
  }
  await loadShipment()
}

onMounted(async () => {
  await loadShipment()
  const paymentId = route.query.paymentId as string | undefined
  if (paymentId) pollPayment(paymentId)
})

// Retry payment for an unpaid request (amount fixed by the server).
const { initiatePayment } = useShipments()
const retryMethod = ref<'mtn' | 'orange'>('mtn')
const retryPhone = ref('')
const retrying = ref(false)
const needsPayment = computed(() => shipment.value && shipment.value.status === 'finding_driver'
  && ['FAILED', 'CANCELLED'].includes(shipment.value.paymentStatus ?? ''))
async function retryPayment() {
  if (!shipment.value?.xenderRequestId) return
  if (retryPhone.value.replace(/\D/g, '').replace(/^237/, '').length !== 9) {
    notify({ title: 'Enter the 9-digit Mobile Money number to charge.', variant: 'error' })
    return
  }
  retrying.value = true
  const res = await initiatePayment(shipment.value.xenderRequestId, retryMethod.value, retryPhone.value)
  retrying.value = false
  if (!res.ok || !res.paymentId) {
    notify({ title: 'Payment could not start', message: res.error, variant: 'error' })
    return
  }
  notify({ title: 'Approve the payment on your phone', variant: 'success' })
  await pollPayment(res.paymentId)
}

async function navigate() {
  const d = shipment.value?.destination
  const url = d?.lat != null && d?.lng != null
    ? `https://www.google.com/maps/dir/?api=1&destination=${d.lat},${d.lng}`
    : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(d?.name ?? '')}`
  window.open(url, '_blank')
}

useHead({ title: () => (shipment.value ? `${shipment.value.ref} — DOCH Xender` : 'Shipment — DOCH Xender') })

// Timeline: each lifecycle stage with its completion time (if reached).
const timeline = computed(() => {
  const s = shipment.value
  if (!s) return []
  const reachedIdx = STATUS_ORDER.indexOf(s.status)
  return STATUS_ORDER.map((st, i) => {
    const evt = s.events.find((e: { status: string }) => e.status === st)
    return {
      status: st,
      meta: STATUS_META[st],
      done: i < reachedIdx,
      current: i === reachedIdx,
      at: evt?.at ?? null,
    }
  })
})

// Delivery completion is performed by the DRIVER in the driver app (they scan
// the receiver's OTP). The sender just shows the code to the receiver.

// DOCH customer support — the sender's single point of contact for any delivery
// problem. They never reach the driver directly; support coordinates with the
// driver on their behalf. PLACEHOLDER details — swap for the real support line.
const SUPPORT = {
  phone: '+237620000000',
  whatsapp: '237620000000',
  email: 'support@doch.cm',
}
const supportMailto = computed(() => {
  const ref = shipment.value?.ref ?? ''
  return `mailto:${SUPPORT.email}?subject=${encodeURIComponent(`Help with delivery ${ref}`)}`
})

const trackUrl = computed(() => (shipment.value ? `/track/${shipment.value.ref}` : '/'))

function copyTrack() {
  if (!import.meta.client || !shipment.value) return
  const url = `${location.origin}/track/${shipment.value.ref}`
  navigator.clipboard?.writeText(url)
  notify({ title: 'Link copied', message: 'Share it with your receiver.', variant: 'info' })
}

function copyOtp() {
  if (!import.meta.client || !shipment.value) return
  navigator.clipboard?.writeText(shipment.value.otp)
  notify({ title: 'Code copied', variant: 'info' })
}
</script>

<template>
  <div v-if="shipment" class="trk">
    <NuxtLink to="/" class="trk-back"><UIcon name="i-heroicons-arrow-left" /> {{ $t('xender.track.allShipments') }}</NuxtLink>

    <!-- New-order banner -->
    <div v-if="isNew" class="trk-new">
      <UIcon name="i-heroicons-check-circle" />
      <div>
        <p class="trk-new__title">{{ $t('xender.track.createdTitle') }}</p>
        <p class="trk-new__sub">{{ $t('xender.track.createdSub') }}</p>
      </div>
    </div>

    <!-- Header -->
    <header class="trk-head">
      <div>
        <h1 class="trk-head__ref">{{ shipment.ref }}</h1>
        <p class="trk-head__route">
          {{ shipment.pickup.name }} <UIcon name="i-heroicons-arrow-long-right" /> {{ shipment.destination.name }}
          · {{ shipment.distanceKm.toFixed(1) }} km
        </p>
      </div>
      <XenderStatusBadge :status="shipment.status" />
    </header>

    <div class="trk-grid">
      <!-- LEFT: timeline + confirm -->
      <div class="trk-col">
        <section class="card">
          <h2 class="card__title">{{ $t('xender.track.tracking') }}</h2>
          <ol class="tl">
            <li v-for="t in timeline" :key="t.status" class="tl__item" :class="{ 'tl__item--done': t.done, 'tl__item--current': t.current }">
              <span class="tl__dot" :style="t.done || t.current ? { background: t.meta.color, borderColor: t.meta.color } : {}">
                <UIcon v-if="t.done" name="i-heroicons-check" />
                <UIcon v-else-if="t.current" :name="t.meta.icon" />
              </span>
              <div class="tl__body">
                <p class="tl__label" :class="{ 'tl__label--muted': !t.done && !t.current }">{{ t.meta.label }}</p>
                <p v-if="t.at" class="tl__time">{{ timeAgo(t.at) }}</p>
                <p v-else-if="t.current" class="tl__time tl__time--live">In progress…</p>
              </div>
            </li>
          </ol>
        </section>
        <section v-if="needsPayment" class="card">
          <h2 class="card__title"><UIcon name="i-heroicons-credit-card" /> Payment required</h2>
          <p class="card__sub">{{ shipment.failureReason || 'The last payment attempt did not succeed.' }} A driver is assigned only after payment.</p>
          <div style="display:flex;gap:0.5rem;flex-wrap:wrap;margin-top:0.75rem">
            <select v-model="retryMethod" class="field__input" style="max-width:10rem">
              <option value="mtn">MTN MoMo</option>
              <option value="orange">Orange Money</option>
            </select>
            <input v-model="retryPhone" type="tel" class="field__input" style="max-width:12rem" placeholder="6XXXXXXXX" />
            <button class="trk-confirm" :disabled="retrying" @click="retryPayment">{{ retrying ? 'Waiting for approval…' : `Pay ${formatXAF(shipment.price.total)}` }}</button>
          </div>
        </section>
        <section v-else-if="shipment.status === 'delivered'" class="card card--done">
          <UIcon name="i-heroicons-check-badge" class="trk-done__icon" />
          <div>
            <p class="trk-done__title">{{ $t('xender.track.deliveredTitle') }}</p>
            <p class="trk-done__sub">{{ $t('xender.dash.to', { name: shipment.receiver.name }) }} · {{ shipment.events.length ? timeAgo(shipment.events[shipment.events.length - 1].at) : '' }}</p>
          </div>
        </section>
      </div>

      <!-- RIGHT: details -->
      <div class="trk-col">
        <!-- OTP -->
        <section v-if="shipment.status !== 'delivered'" class="card card--otp">
          <p class="otp__label">{{ $t('xender.track.deliveryCode') }}</p>
          <p class="otp__code" @click="copyOtp">{{ shipment.otp }} <UIcon name="i-heroicons-clipboard-document" /></p>
          <p class="otp__hint">{{ $t('xender.track.otpHint', { name: shipment.receiver.name }) }}</p>
        </section>

        <!-- Parcel -->
        <section class="card">
          <h2 class="card__title">{{ $t('xender.track.parcel') }}</h2>
          <div class="pc">
            <div class="pc__thumb"><img v-if="shipment.photo" :src="shipment.photo" alt="" /><UIcon v-else name="i-heroicons-cube" /></div>
            <div>
              <p class="pc__name">{{ shipment.description || $t('xender.track.parcel') }}</p>
              <p class="pc__sub">{{ shipment.weightKg }} kg · ETA {{ formatDuration(shipment.etaMin) }}</p>
            </div>
          </div>
          <div class="pc__row"><span>{{ $t('xender.track.receiver') }}</span><strong>{{ shipment.receiver.name }}</strong></div>
          <div class="pc__row"><span>{{ $t('xender.track.phone') }}</span><strong>{{ formatPhone(shipment.receiver.phone) }}</strong></div>
        </section>

        <!-- Price -->
        <section class="card">
          <h2 class="card__title">{{ $t('xender.track.payment') }}</h2>
          <div class="pc__row"><span>{{ $t('xender.track.zone') }}</span><strong>{{ shipment.price.zone || '—' }}</strong></div>
        </section>

        <!-- Customer support -->
        <section class="card card--support">
          <h2 class="card__title"><UIcon name="i-heroicons-lifebuoy" /> {{ $t('xender.track.needHelp') }}</h2>
          <p class="sup__intro">{{ $t('xender.track.supportIntro') }}</p>
          <a :href="`tel:${SUPPORT.phone}`" class="sup__btn sup__btn--call"><UIcon name="i-heroicons-phone" /> {{ $t('xender.track.callSupport') }}</a>
          <a :href="`https://wa.me/${SUPPORT.whatsapp}`" target="_blank" rel="noopener" class="sup__btn"><UIcon name="i-simple-icons-whatsapp" /> {{ $t('xender.track.whatsapp') }}</a>
          <a :href="supportMailto" class="sup__btn"><UIcon name="i-heroicons-envelope" /> {{ $t('xender.track.email') }}</a>
          <p class="sup__ref" v-html="$t('xender.track.supportRef', { ref: `<strong>${shipment.ref}</strong>` })"></p>
        </section>

        <button class="trk-share" @click="copyTrack">
          <UIcon name="i-heroicons-link" /> {{ $t('xender.track.copyLink') }}
        </button>
        <NuxtLink :to="trackUrl" class="trk-share trk-share--ghost">
          <UIcon name="i-heroicons-eye" /> {{ $t('xender.track.previewReceiver') }}
        </NuxtLink>
      </div>
    </div>
  </div>

  <!-- Not found -->
  <div v-else class="trk-missing">
    <UIcon name="i-heroicons-question-mark-circle" />
    <h1>{{ $t('xender.track.notFoundTitle') }}</h1>
    <p>{{ $t('xender.track.notFoundSub') }}</p>
    <NuxtLink to="/" class="trk-missing__btn">{{ $t('xender.track.backBtn') }}</NuxtLink>
  </div>
</template>

<style scoped>
.trk { display: flex; flex-direction: column; gap: 1.25rem; }
.trk-back { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.8rem; font-weight: 600; color: #6B7280; text-decoration: none; }
.trk-back:hover { color: #E8202A; }
.trk-back :deep(svg) { width: 1rem; height: 1rem; }

.trk-new { display: flex; align-items: center; gap: 0.75rem; background: #ECFDF5; border: 1px solid #A7F3D0; border-radius: 0.75rem; padding: 1rem; }
.trk-new :deep(svg) { width: 1.6rem; height: 1.6rem; color: #16A34A; flex-shrink: 0; }
.trk-new__title { font-size: 0.9rem; font-weight: 800; color: #065F46; margin: 0; }
.trk-new__sub { font-size: 0.8rem; color: #047857; margin: 0.1rem 0 0; }

.trk-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.trk-head__ref { font-size: 1.4rem; font-weight: 800; color: #111827; margin: 0; }
.trk-head__route { display: flex; align-items: center; gap: 0.3rem; font-size: 0.85rem; font-weight: 600; color: #6B7280; margin: 0.25rem 0 0; }
.trk-head__route :deep(svg) { width: 1rem; height: 1rem; color: #E8202A; }

.trk-grid { display: grid; grid-template-columns: 1fr 320px; gap: 1.25rem; align-items: start; }
.trk-col { display: flex; flex-direction: column; gap: 1.25rem; }

.card { background: #fff; border: 1px solid #E5E7EB; border-radius: 0.85rem; padding: 1.25rem; }
.card__title { display: flex; align-items: center; gap: 0.4rem; font-size: 0.9rem; font-weight: 800; color: #111827; margin: 0 0 1rem; }
.card__title :deep(svg) { width: 1.1rem; height: 1.1rem; color: #E8202A; }
.card__sub { font-size: 0.8rem; color: #6B7280; margin: -0.5rem 0 1rem; }
.card--sim { border: 1px dashed #9CA3AF; background: #F9FAFB; }
.sim-tag { display: inline-flex; align-items: center; gap: 0.3rem; font-size: 0.65rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; color: #6B7280; background: #E5E7EB; border-radius: 999px; padding: 0.2rem 0.55rem; margin-bottom: 0.85rem; }
.sim-tag :deep(svg) { width: 0.8rem; height: 0.8rem; }
.card--done { display: flex; align-items: center; gap: 0.85rem; border-color: #A7F3D0; background: #ECFDF5; }
.trk-done__icon { width: 2.2rem; height: 2.2rem; color: #16A34A; flex-shrink: 0; }
.trk-done__title { font-size: 0.95rem; font-weight: 800; color: #065F46; margin: 0; }
.trk-done__sub { font-size: 0.8rem; color: #047857; margin: 0.1rem 0 0; }

/* timeline */
.tl { list-style: none; margin: 0; padding: 0; }
.tl__item { position: relative; display: flex; gap: 0.85rem; padding-bottom: 1.4rem; }
.tl__item:not(:last-child)::before { content: ''; position: absolute; left: 0.72rem; top: 1.6rem; bottom: 0; width: 2px; background: #E5E7EB; }
.tl__item--done:not(:last-child)::before { background: #16A34A; }
.tl__dot { z-index: 1; flex-shrink: 0; width: 1.5rem; height: 1.5rem; border-radius: 50%; background: #fff; border: 2px solid #E5E7EB; display: flex; align-items: center; justify-content: center; color: #fff; }
.tl__dot :deep(svg) { width: 0.85rem; height: 0.85rem; }
.tl__item--current .tl__dot { box-shadow: 0 0 0 4px rgba(217,119,6,0.15); }
.tl__label { font-size: 0.85rem; font-weight: 700; color: #111827; margin: 0; line-height: 1.5rem; }
.tl__label--muted { color: #9CA3AF; font-weight: 600; }
.tl__time { font-size: 0.73rem; color: #9CA3AF; margin: 0; }
.tl__time--live { color: #D97706; font-weight: 600; }

.trk-confirm { width: 100%; margin-top: 1rem; display: inline-flex; align-items: center; justify-content: center; gap: 0.4rem; background: #D97706; color: #fff; border: none; border-radius: 0.5rem; padding: 0.8rem; font-size: 0.82rem; font-weight: 700; cursor: pointer; }
.trk-confirm:hover:not(:disabled) { background: #B45309; }
.trk-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.trk-confirm :deep(svg) { width: 1.05rem; height: 1.05rem; }
.trk-spin { animation: trk-rot 0.8s linear infinite; }
@keyframes trk-rot { to { transform: rotate(360deg); } }

/* otp */
.card--otp { background: linear-gradient(135deg, #E8202A, #B3151D); border: none; color: #fff; text-align: center; }
.otp__label { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: rgba(255,255,255,0.8); margin: 0 0 0.5rem; }
.otp__code { display: inline-flex; align-items: center; gap: 0.5rem; font-size: 1.9rem; font-weight: 800; letter-spacing: 0.25em; margin: 0 0 0.6rem; cursor: pointer; }
.otp__code :deep(svg) { width: 1.2rem; height: 1.2rem; opacity: 0.8; letter-spacing: normal; }
.otp__hint { font-size: 0.75rem; color: rgba(255,255,255,0.85); margin: 0; line-height: 1.5; }

/* customer support */
.card--support { border-color: #FECACA; background: #FFF7F7; }
.sup__intro { font-size: 0.8rem; color: #6B7280; line-height: 1.5; margin: -0.5rem 0 1rem; }
.sup__btn { width: 100%; display: inline-flex; align-items: center; justify-content: center; gap: 0.45rem; border: 1px solid #E5E7EB; background: #fff; border-radius: 0.5rem; padding: 0.7rem; font-size: 0.82rem; font-weight: 700; color: #374151; text-decoration: none; margin-bottom: 0.5rem; }
.sup__btn:hover { border-color: #E8202A; color: #E8202A; }
.sup__btn :deep(svg) { width: 1.05rem; height: 1.05rem; }
.sup__btn--call { background: #E8202A; border-color: #E8202A; color: #fff; }
.sup__btn--call:hover { background: #C8181F; color: #fff; }
.sup__ref { font-size: 0.72rem; color: #9CA3AF; text-align: center; margin: 0.5rem 0 0; }
.sup__ref strong { color: #6B7280; }

/* parcel + price rows */
.pc { display: flex; align-items: center; gap: 0.75rem; padding-bottom: 0.85rem; margin-bottom: 0.5rem; border-bottom: 1px solid #F3F4F6; }
.pc__thumb { flex-shrink: 0; width: 3rem; height: 3rem; border-radius: 0.5rem; background: #F3F4F6; display: flex; align-items: center; justify-content: center; overflow: hidden; color: #9CA3AF; }
.pc__thumb img { width: 100%; height: 100%; object-fit: cover; }
.pc__thumb :deep(svg) { width: 1.4rem; height: 1.4rem; }
.pc__name { font-size: 0.85rem; font-weight: 700; color: #111827; margin: 0; }
.pc__sub { font-size: 0.75rem; color: #6B7280; margin: 0.1rem 0 0; }
.pc__row { display: flex; align-items: center; justify-content: space-between; font-size: 0.8rem; padding: 0.4rem 0; }
.pc__row span { color: #9CA3AF; }
.pc__row strong { color: #111827; font-weight: 600; }
.pc__row--total { border-top: 1px dashed #D1D5DB; margin-top: 0.4rem; padding-top: 0.7rem; }
.pc__row--total strong { color: #E8202A; font-size: 0.9rem; }

.trk-share { width: 100%; display: inline-flex; align-items: center; justify-content: center; gap: 0.45rem; background: #fff; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.7rem; font-size: 0.8rem; font-weight: 700; color: #4B5563; cursor: pointer; text-decoration: none; }
.trk-share:hover { border-color: #E8202A; color: #E8202A; }
.trk-share :deep(svg) { width: 1rem; height: 1rem; }
.trk-share--ghost { color: #9CA3AF; }

.trk-missing { text-align: center; padding: 4rem 1.5rem; }
.trk-missing :deep(svg) { width: 3rem; height: 3rem; color: #D1D5DB; }
.trk-missing h1 { font-size: 1.2rem; font-weight: 800; color: #111827; margin: 1rem 0 0.4rem; }
.trk-missing p { font-size: 0.85rem; color: #6B7280; margin: 0 0 1.5rem; }
.trk-missing__btn { display: inline-block; background: #E8202A; color: #fff; font-weight: 700; font-size: 0.82rem; padding: 0.7rem 1.3rem; border-radius: 0.5rem; text-decoration: none; }

@media (max-width: 860px) {
  .trk-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .trk { gap: 1rem; }
  .trk-grid { gap: 1rem; }
  .trk-col { gap: 1rem; }
  .card { padding: 1rem; border-radius: 0.75rem; }
  .card__title { font-size: 0.85rem; margin-bottom: 0.85rem; }
  .trk-head__ref { font-size: 1.2rem; }
  .trk-head__route { font-size: 0.8rem; }
  .trk-new { padding: 0.85rem; }
  .otp__code { font-size: 1.6rem; }
  .tl__label { font-size: 0.82rem; }
  .pc__row { font-size: 0.78rem; }
  .sup__btn { font-size: 0.8rem; padding: 0.65rem; }
  .trk-share { font-size: 0.78rem; padding: 0.65rem; }
}
</style>