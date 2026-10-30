<script setup lang="ts">
// Fapshi sends the sender back here after they complete (or abandon) payment
// on their Mobile Money app. send.vue stashes our paymentId in sessionStorage
// right before navigating away — we read it back here rather than trusting
// Fapshi's redirect to carry it reliably.
useHead({ title: 'Payment Status — DOCH Xender' })

const route = useRoute()
const { get } = useApi()

type Phase = 'checking' | 'completed' | 'failed' | 'pending-timeout' | 'unknown'
const phase = ref<Phase>('checking')
const pollCount = ref(0)
const MAX_POLLS = 20 // ~2 minutes at 6s intervals

function resolvePaymentId(): string | null {
  const fromQuery = String(route.query.paymentId || route.query.transId || route.query.id || '')
  if (fromQuery) return fromQuery
  if (typeof window !== 'undefined') return sessionStorage.getItem('doch_pending_payment_id')
  return null
}

function resolveShipmentId(): string | null {
  if (typeof window !== 'undefined') return sessionStorage.getItem('doch_pending_shipment_id')
  return null
}

async function poll() {
  const paymentId = resolvePaymentId()
  if (!paymentId) {
    phase.value = 'unknown'
    return
  }

  try {
    const res: any = await get(`/api/payments/${paymentId}/status`)
    const status = res?.status

    if (status === 'COMPLETED') {
      phase.value = 'completed'
      const shipmentId = resolveShipmentId()
      if (typeof window !== 'undefined') {
        sessionStorage.removeItem('doch_pending_payment_id')
        sessionStorage.removeItem('doch_pending_payment_app')
        sessionStorage.removeItem('doch_pending_shipment_id')
      }
      setTimeout(() => navigateTo(shipmentId ? `/shipments/${shipmentId}` : '/'), 1200)
      return
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      phase.value = 'failed'
      if (typeof window !== 'undefined') {
        sessionStorage.removeItem('doch_pending_payment_id')
        sessionStorage.removeItem('doch_pending_payment_app')
      }
      return
    }

    pollCount.value++
    if (pollCount.value >= MAX_POLLS) {
      phase.value = 'pending-timeout'
      return
    }
    setTimeout(poll, 10000)
  } catch {
    pollCount.value++
    if (pollCount.value >= MAX_POLLS) {
      phase.value = 'unknown'
      return
    }
    setTimeout(poll, 10000)
  }
}

onMounted(poll)
</script>

<template>
  <section class="au">
    <div class="au__card au__card--single">
      <div class="au-form-wrap">
        <template v-if="phase === 'checking'">
          <div class="ps-spinner" />
          <h1 class="au-head__title">Confirming your payment…</h1>
          <p class="au-head__sub">This can take a moment — please don't close this page.</p>
        </template>

        <template v-else-if="phase === 'completed'">
          <div class="ps-icon ps-icon--ok">✓</div>
          <h1 class="au-head__title">Payment confirmed!</h1>
          <p class="au-head__sub">Redirecting to your shipment…</p>
        </template>

        <template v-else-if="phase === 'failed'">
          <div class="ps-icon ps-icon--fail">✕</div>
          <h1 class="au-head__title">Payment didn't go through</h1>
          <p class="au-head__sub">Your delivery wasn't charged. You can retry payment from the shipment page.</p>
          <NuxtLink :to="resolveShipmentId() ? `/shipments/${resolveShipmentId()}` : '/'" class="au-submit-link">View my shipments</NuxtLink>
        </template>

        <template v-else-if="phase === 'pending-timeout'">
          <div class="ps-icon ps-icon--wait">⏳</div>
          <h1 class="au-head__title">Still processing</h1>
          <p class="au-head__sub">
            Your payment is taking longer than usual to confirm. Check your Mobile Money app —
            if you approved it, this will update automatically.
          </p>
          <NuxtLink :to="resolveShipmentId() ? `/shipments/${resolveShipmentId()}` : '/'" class="au-submit-link">View my shipments</NuxtLink>
        </template>

        <template v-else>
          <div class="ps-icon ps-icon--wait">?</div>
          <h1 class="au-head__title">We couldn't find that payment</h1>
          <p class="au-head__sub">If you completed a payment, check your shipments — it may already be confirmed.</p>
          <NuxtLink :to="resolveShipmentId() ? `/shipments/${resolveShipmentId()}` : '/'" class="au-submit-link">View my shipments</NuxtLink>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.au { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.au__card--single { width: 100%; max-width: 420px; border-radius: 0.85rem; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.2); background: #fff; }
.au-form-wrap { padding: 2.5rem 2rem; text-align: center; }
.au-head__title { font-size: 1.25rem; font-weight: 800; color: #111827; margin: 0 0 0.5rem; }
.au-head__sub { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; line-height: 1.55; }
.ps-spinner { width: 2.75rem; height: 2.75rem; border: 3px solid #FCE7E8; border-top-color: #E8202A; border-radius: 50%; margin: 0 auto 1.25rem; animation: ps-spin 0.9s linear infinite; }
@keyframes ps-spin { to { transform: rotate(360deg); } }
.ps-icon { width: 3.25rem; height: 3.25rem; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem; font-size: 1.5rem; font-weight: 800; }
.ps-icon--ok { background: #DCFCE7; color: #16A34A; }
.ps-icon--fail { background: #FEE2E2; color: #E8202A; }
.ps-icon--wait { background: #FEF3C7; color: #D97706; }
.au-submit-link { display: inline-block; background: #E8202A; color: #fff; text-decoration: none; padding: 0.75rem 1.5rem; border-radius: 0.5rem; font-size: 0.8125rem; font-weight: 700; }
.au-submit-link:hover { background: #C8181F; }
</style>
