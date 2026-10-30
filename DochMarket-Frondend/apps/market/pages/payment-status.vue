<script setup lang="ts">
// Fapshi sends the buyer back here after they complete (or abandon) checkout
// on their Mobile Money app. We don't rely on Fapshi's redirect carrying our
// paymentId reliably — checkout.vue stashes it in sessionStorage right
// before navigating away, and we read it back here.
definePageMeta({ layout: false })
useHead({ title: 'Payment Status — Doch Market' })

const route = useRoute()
const { get } = useApi()

type Phase = 'checking' | 'completed' | 'failed' | 'pending-timeout' | 'unknown'
const phase = ref<Phase>('checking')
const orderLink = ref('/orders')
const pollCount = ref(0)
const MAX_POLLS = 30 // ~5 minutes at 10s intervals (provider allows 6 status checks per minute)

function resolvePaymentId(): string | null {
  const fromQuery = String(route.query.paymentId || route.query.transId || route.query.id || '')
  if (fromQuery) return fromQuery
  if (process.client) return sessionStorage.getItem('doch_pending_payment_id')
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
    if (res?.orderId) orderLink.value = `/orders/${res.orderId}`

    if (status === 'COMPLETED') {
      phase.value = 'completed'
      if (process.client) {
        sessionStorage.removeItem('doch_pending_payment_id')
        sessionStorage.removeItem('doch_pending_payment_app')
      }
      setTimeout(() => navigateTo(orderLink.value), 1200)
      return
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      phase.value = 'failed'
      if (process.client) {
        sessionStorage.removeItem('doch_pending_payment_id')
        sessionStorage.removeItem('doch_pending_payment_app')
      }
      return
    }

    // still PENDING/PROCESSING — keep polling
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
  <div class="ps-page">
    <div class="ps-card">
      <img src="/images/logos/Logo-03.png" alt="Doch Market" class="ps-logo" />

      <template v-if="phase === 'checking'">
        <div class="ps-spinner" />
        <h1 class="ps-title">Confirming your payment…</h1>
        <p class="ps-sub">This can take a moment — please don't close this page.</p>
      </template>

      <template v-else-if="phase === 'completed'">
        <div class="ps-icon ps-icon--ok">✓</div>
        <h1 class="ps-title">Payment confirmed!</h1>
        <p class="ps-sub">Redirecting to your order…</p>
      </template>

      <template v-else-if="phase === 'failed'">
        <div class="ps-icon ps-icon--fail">✕</div>
        <h1 class="ps-title">Payment didn't go through</h1>
        <p class="ps-sub">Your order wasn't charged. Your products are reserved for 48 hours — retry the payment from your order.</p>
        <NuxtLink :to="orderLink" class="ps-btn">Retry payment</NuxtLink>
      </template>

      <template v-else-if="phase === 'pending-timeout'">
        <div class="ps-icon ps-icon--wait">⏳</div>
        <h1 class="ps-title">Still processing</h1>
        <p class="ps-sub">
          Your payment is taking longer than usual to confirm. Check your Mobile Money app —
          if you approved it, your order will update automatically. You can also check your
          order history in a few minutes.
        </p>
        <NuxtLink :to="orderLink" class="ps-btn">View my order</NuxtLink>
      </template>

      <template v-else>
        <div class="ps-icon ps-icon--wait">?</div>
        <h1 class="ps-title">We couldn't find that payment</h1>
        <p class="ps-sub">If you completed a payment, check your order history — it may already be confirmed.</p>
        <NuxtLink :to="orderLink" class="ps-btn">View my order</NuxtLink>
      </template>
    </div>
  </div>
</template>

<style scoped>
.ps-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #F9FAFB; padding: 2rem 1.5rem; }
.ps-card { width: 100%; max-width: 420px; background: #fff; border: 1px solid #E5E7EB; border-radius: 0.85rem; padding: 2.5rem 2rem; text-align: center; box-shadow: 0 20px 50px -20px rgba(0,0,0,0.15); }
.ps-logo { height: 2.2rem; margin: 0 auto 1.5rem; display: block; }
.ps-spinner { width: 2.75rem; height: 2.75rem; border: 3px solid #FCE7E8; border-top-color: #E8202A; border-radius: 50%; margin: 0 auto 1.25rem; animation: ps-spin 0.9s linear infinite; }
@keyframes ps-spin { to { transform: rotate(360deg); } }
.ps-icon { width: 3.25rem; height: 3.25rem; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem; font-size: 1.5rem; font-weight: 800; }
.ps-icon--ok { background: #DCFCE7; color: #16A34A; }
.ps-icon--fail { background: #FEE2E2; color: #E8202A; }
.ps-icon--wait { background: #FEF3C7; color: #D97706; }
.ps-title { font-size: 1.2rem; font-weight: 800; color: #111827; margin: 0 0 0.5rem; }
.ps-sub { font-size: 0.875rem; color: #6B7280; line-height: 1.55; margin: 0 0 1.5rem; }
.ps-btn { display: inline-block; background: #E8202A; color: #fff; text-decoration: none; padding: 0.75rem 1.5rem; border-radius: 0.5rem; font-size: 0.8125rem; font-weight: 700; }
.ps-btn:hover { background: #C8181F; }
</style>
