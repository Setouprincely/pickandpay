<script setup lang="ts">
// Customer order detail: payment, per-package tracking timeline, delivery code, cancel and rating.
const route = useRoute()
const orderId = computed(() => String(route.params.id))
useHead({ title: 'Order — Doch Market' })

const { get, pay, paymentStatus, cancel, review, errText } = useOrder()
const { format } = useCurrency()
const { notify } = useNotify()

const order = ref<any | null>(null)
const loading = ref(true)
const error = ref('')
const busy = ref(false)

const label = (s?: string) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')
const dt = (v?: string) => (v ? new Date(v).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '—')
const badge = (s?: string) =>
  s === 'DELIVERED' || s === 'COMPLETED' ? 'bg-green-100 text-green-800'
    : ['PAYMENT_FAILED', 'CANCELLED', 'RETURNED', 'FAILED'].includes(s ?? '') ? 'bg-red-100 text-red-800'
      : s === 'PENDING' || s === 'PENDING_PAYMENT' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'

const STEPS = ['PENDING_PREPARATION', 'PENDING_ASSIGNMENT', 'DRIVER_ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED']
const stepIndex = (s: string) => (s === 'AT_HUB' ? 3 : s === 'FAILED' ? 4 : STEPS.indexOf(s))

async function load() {
  try {
    order.value = await get(orderId.value)
    error.value = ''
  } catch (e: any) {
    error.value = errText(e, 'Could not load this order')
  } finally {
    loading.value = false
  }
}

// --- payment retry ---
const method = ref<'MTN_MOMO' | 'ORANGE_MONEY'>('MTN_MOMO')
const momo = ref('')
const polling = ref(false)
async function retryPayment(mode: 'prompt' | 'page') {
  if (mode === 'prompt' && !/^6\d{8}$/.test(momo.value.replace(/\D/g, ''))) {
    notify({ title: 'Enter a 9-digit Mobile Money number starting with 6.', variant: 'error' })
    return
  }
  busy.value = true
  try {
    const res = await pay(order.value.orderId, method.value, mode === 'prompt' ? momo.value.replace(/\D/g, '') : undefined)
    if (res.status === 'FAILED') {
      notify({ title: res.message || 'Payment could not be started.', variant: 'error' })
    } else if (mode === 'page' && res.paymentUrl) {
      sessionStorage.setItem('doch_pending_payment_id', res.paymentId)
      window.location.href = res.paymentUrl
      return
    } else {
      notify({ title: 'Payment request sent. Approve it on your phone.' })
      await pollPayment(res.paymentId)
    }
    await load()
  } catch (e: any) {
    notify({ title: errText(e, 'Payment failed'), variant: 'error' })
  } finally {
    busy.value = false
  }
}

async function pollPayment(paymentId: string) {
  polling.value = true
  for (let i = 0; i < 30; i++) {
    await new Promise(r => setTimeout(r, 10000))
    try {
      const st = await paymentStatus(paymentId)
      if (st.status !== 'PENDING') {
        notify({ title: st.status === 'COMPLETED' ? 'Payment confirmed!' : (st.failureReason || 'Payment failed'), variant: st.status === 'COMPLETED' ? 'success' : 'error' })
        break
      }
    } catch { /* keep polling */ }
  }
  polling.value = false
}

async function cancelOrder() {
  if (!confirm(`Cancel order ${order.value.reference}? Reserved products will be released.`)) return
  busy.value = true
  try {
    order.value = await cancel(order.value.orderId, 'Cancelled by customer')
    notify({ title: 'Order cancelled.' })
  } catch (e: any) {
    notify({ title: errText(e, 'Could not cancel'), variant: 'error' })
  } finally {
    busy.value = false
  }
}

// --- rating ---
const rating = ref(0)
const deliveryRating = ref(0)
const comment = ref('')
async function submitReview() {
  if (!rating.value) {
    notify({ title: 'Choose a rating from 1 to 5 stars.', variant: 'error' })
    return
  }
  busy.value = true
  try {
    order.value = await review(order.value.orderId, {
      rating: rating.value, deliveryRating: deliveryRating.value || undefined, comment: comment.value.trim() || undefined,
    })
    notify({ title: 'Thank you for your review!' })
  } catch (e: any) {
    notify({ title: errText(e, 'Could not submit review'), variant: 'error' })
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: 'My orders', to: '/orders' }, { label: order?.reference ?? 'Order' }]" />
    <section class="max-w-5xl mx-auto px-4 py-6 space-y-4">
      <div v-if="loading" class="text-gray-500">Loading…</div>
      <div v-else-if="error" class="rounded bg-red-50 text-red-700 p-3">{{ error }}</div>
      <template v-else-if="order">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 class="text-2xl font-extrabold font-mono">{{ order.reference }}</h1>
            <p class="text-sm text-gray-500">Placed {{ dt(order.createdAt) }}<span v-if="order.paidAt"> · paid {{ dt(order.paidAt) }}</span></p>
          </div>
          <span class="px-3 py-1 rounded text-sm font-semibold" :class="badge(order.status)">{{ label(order.status) }}</span>
        </div>

        <!-- Payment required -->
        <div v-if="order.canPay" class="rounded-lg border-2 border-red-200 bg-red-50 p-4 space-y-3">
          <h2 class="font-bold">Payment {{ order.status === 'PAYMENT_FAILED' ? 'failed' : 'not completed' }}</h2>
          <p v-if="order.paymentDetails?.failureReason" class="text-sm text-red-700">{{ order.paymentDetails.failureReason }}</p>
          <p v-if="order.paymentDetails?.status === 'PENDING'" class="text-sm">A payment request is pending. If you approved it, the order will update shortly.</p>
          <div class="flex flex-wrap gap-2 items-center">
            <select v-model="method" class="border rounded px-2 py-2 text-sm bg-white">
              <option value="MTN_MOMO">MTN Mobile Money</option>
              <option value="ORANGE_MONEY">Orange Money</option>
            </select>
            <input v-model="momo" type="tel" class="border rounded px-3 py-2 text-sm bg-white" placeholder="6XXXXXXXX" />
            <button class="bg-red-600 text-white rounded px-4 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busy || polling" @click="retryPayment('prompt')">
              {{ polling ? 'Waiting for approval…' : 'Send payment request' }}
            </button>
            <button class="border border-red-600 text-red-700 rounded px-4 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busy || polling" @click="retryPayment('page')">Pay on payment page</button>
          </div>
        </div>

        <div class="grid md:grid-cols-[1fr_320px] gap-4">
          <div class="space-y-4">
            <!-- Packages & tracking -->
            <div v-for="l in order.deliveryLegs" :key="l.deliveryId" class="rounded-lg border bg-white p-4 space-y-3">
              <div class="flex flex-wrap justify-between gap-2">
                <div>
                  <div class="text-xs text-gray-500">Package from {{ l.sellerName }}</div>
                  <div class="font-mono font-bold">{{ l.trackingNumber }}</div>
                </div>
                <span class="px-2 py-0.5 rounded text-xs h-fit" :class="badge(l.status)">{{ label(l.status) }}</span>
              </div>

              <div v-if="stepIndex(l.status) >= 0" class="flex gap-1">
                <div v-for="(s, i) in STEPS" :key="s" class="h-1.5 flex-1 rounded"
                     :class="i <= stepIndex(l.status) ? (l.status === 'FAILED' ? 'bg-red-500' : 'bg-green-500') : 'bg-gray-200'" />
              </div>

              <div v-if="l.deliveryOtp" class="rounded bg-amber-50 border border-amber-200 p-3 text-sm">
                Delivery code: <strong class="text-xl tracking-widest font-mono">{{ l.deliveryOtp }}</strong>
                <div class="text-xs text-gray-600">Give it to the DOCH driver only when you receive this package. Never share it by phone.</div>
              </div>
              <div v-if="l.driverName" class="text-sm">Driver: {{ l.driverName }} · {{ l.driverPhone }}<span v-if="l.vehicleName"> · {{ l.vehicleName }} ({{ l.vehiclePlate }})</span></div>
              <div v-if="l.failureReason" class="text-sm text-red-700">{{ l.failureReason }}</div>
              <div v-if="l.deliveredAt" class="text-sm text-green-700">Delivered {{ dt(l.deliveredAt) }}</div>

              <ol class="border-l-2 border-gray-200 pl-3 space-y-1">
                <li v-for="(e, i) in (l.events ?? []).filter((x: any) => x.message)" :key="i" class="text-sm">
                  <span class="text-gray-500 text-xs">{{ dt(e.occurredAt) }}</span><br>{{ e.message }}<span v-if="e.hubName"> ({{ e.hubName }})</span>
                </li>
              </ol>
            </div>

            <!-- Rating -->
            <div v-if="order.canReview" class="rounded-lg border bg-white p-4 space-y-3">
              <h2 class="font-bold">Rate your order</h2>
              <div class="text-sm">Products &amp; overall experience</div>
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" type="button" class="text-2xl" :class="n <= rating ? 'text-amber-500' : 'text-gray-300'" @click="rating = n">★</button>
              </div>
              <div class="text-sm">Delivery service (driver)</div>
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" type="button" class="text-2xl" :class="n <= deliveryRating ? 'text-amber-500' : 'text-gray-300'" @click="deliveryRating = n">★</button>
              </div>
              <textarea v-model="comment" rows="3" maxlength="1000" class="w-full border rounded p-2 text-sm" placeholder="Tell us more (optional)" />
              <button class="bg-red-600 text-white rounded px-4 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busy" @click="submitReview">Submit review</button>
            </div>
            <div v-else-if="order.review" class="rounded-lg border bg-white p-4 text-sm">
              <h2 class="font-bold">Your review</h2>
              <div class="text-amber-500">{{ '★'.repeat(order.review.rating) }}<span class="text-gray-300">{{ '★'.repeat(5 - order.review.rating) }}</span></div>
              <div v-if="order.review.deliveryRating">Delivery: {{ order.review.deliveryRating }}/5</div>
              <p v-if="order.review.comment" class="italic">“{{ order.review.comment }}”</p>
            </div>
          </div>

          <aside class="rounded-lg border bg-white p-4 space-y-2 h-fit text-sm">
            <h2 class="font-bold">Summary</h2>
            <div v-for="i in order.items" :key="i.productId" class="flex justify-between gap-2">
              <span>{{ i.quantity }} × {{ i.productName }}</span><span>{{ format(i.total_price) }}</span>
            </div>
            <div class="flex justify-between border-t pt-2"><span>Products</span><span>{{ format(order.productTotal) }}</span></div>
            <div class="flex justify-between"><span>Delivery</span><span>{{ format(order.deliveryFee) }}</span></div>
            <div class="flex justify-between font-bold text-base border-t pt-2"><span>Total</span><span>{{ format(order.totalAmount) }}</span></div>
            <div class="pt-2 text-gray-600">Deliver to: {{ order.deliveryAddress }}</div>
            <div v-if="order.paymentDetails" class="text-gray-600">
              Payment: {{ label(order.paymentDetails.status) }}
              <div v-if="order.paymentDetails.providerReference" class="font-mono text-xs">Ref {{ order.paymentDetails.providerReference }}</div>
            </div>
            <div v-if="order.cancellationReason" class="text-red-700">Cancelled: {{ order.cancellationReason }}</div>
            <button v-if="order.canCancel" class="w-full border border-gray-300 rounded px-3 py-2 mt-2 disabled:opacity-50" :disabled="busy" @click="cancelOrder">Cancel order</button>
            <button class="w-full text-gray-500 text-xs mt-1" @click="load">Refresh</button>
          </aside>
        </div>
      </template>
    </section>
  </div>
</template>
