<script setup lang="ts">
import type { PickedLocation } from '../composables/useLocations'

useHead({ title: 'Checkout — Doch Market' })

const { items, subtotal, clear } = useCart()
const { format } = useCurrency()
const { place, preview } = useOrder()
const { notify } = useNotify()
const { $t } = useNuxtApp()
const { user } = useAuth()

const form = reactive({
  name: '', email: '', phone: '',
  address: '', city: '', region: '',
})
onMounted(() => {
  const u: any = user.value
  if (u) {
    form.name ||= [u.firstName, u.lastName].filter(Boolean).join(' ')
    form.email ||= u.email ?? ''
    form.phone ||= u.phone ?? u.tel ?? ''
  }
})

// Delivery location comes from the cascading picker (Region → City → Subdivision → landmark).
const location = ref<PickedLocation | null>(null)
watch(location, (l) => {
  form.region = l?.regionName ?? ''
  form.city = l?.cityName ?? ''
  form.address = l ? l.label : ''
})
const deliveryAddress = computed(() =>
  [location.value?.landmark, location.value?.subdivisionName, form.city, form.region, 'Cameroon'].filter(Boolean).join(', '))

// Payment: DOCH collects Mobile Money through Fapshi (MTN MoMo and Orange Money only).
const payments = computed(() => [
  { id: 'MTN_MOMO', label: 'MTN Mobile Money', desc: $t('market.checkout.momoDesc'), icon: 'i-heroicons-device-phone-mobile' },
  { id: 'ORANGE_MONEY', label: 'Orange Money', desc: $t('market.checkout.momoDesc'), icon: 'i-heroicons-device-phone-mobile' },
])
const payment = ref<'MTN_MOMO' | 'ORANGE_MONEY'>('MTN_MOMO')
/** 'prompt' sends a payment request to the phone; 'page' opens the Fapshi payment page. */
const payMode = ref<'prompt' | 'page'>('prompt')
const momoNumber = ref('')
watch(() => form.phone, p => { if (!momoNumber.value) momoNumber.value = p.replace(/\D/g, '').replace(/^237/, '') })
const momoValid = computed(() => /^6\d{8}$/.test(momoNumber.value.replace(/\D/g, '')))

// Real delivery quote from the backend (per seller, by road distance).
const quote = ref<any | null>(null)
const quoting = ref(false)
const quoteError = ref('')
async function refreshQuote() {
  quote.value = null
  quoteError.value = ''
  if (!location.value || !items.value.length) return
  quoting.value = true
  try {
    quote.value = await preview([...items.value], deliveryAddress.value)
  } catch (e: any) {
    quoteError.value = e?.data?.message ?? `Could not price delivery (${e?.status ?? 'network error'})`
  } finally {
    quoting.value = false
  }
}
watch([location, () => items.value.map(i => `${i.product.id}:${i.qty}`).join()], refreshQuote)

const required = ['name', 'email', 'phone'] as const
const submitted = ref(false)
const isValid = computed(() => required.every(k => form[k].trim() !== '') && !!location.value
  && (payMode.value === 'page' || momoValid.value))
const placing = ref(false)

async function placeOrder() {
  submitted.value = true
  if (!items.value.length) return
  if (!isValid.value) {
    notify({ title: $t('market.checkout.missingTitle'), message: $t('market.checkout.missingText'), variant: 'error' })
    return
  }
  if (!quote.value) {
    notify({ title: 'Delivery not priced', message: quoteError.value || 'Choose a delivery location first.', variant: 'error' })
    return
  }

  placing.value = true
  const res = await place({
    items: [...items.value],
    deliveryAddress: deliveryAddress.value,
    contactPhone: form.phone.trim(),
    paymentMethod: payment.value,
    paymentPhone: payMode.value === 'prompt' ? momoNumber.value.replace(/\D/g, '') : undefined,
    customer: {
      name: form.name, email: form.email, phone: form.phone,
      address: form.address, city: form.city, region: form.region,
    },
  })
  placing.value = false

  if (!res.ok || !res.order) {
    notify({ title: 'Order failed', message: res.error || 'Could not place the order.', variant: 'error' })
    return
  }
  clear()
  const order = res.order
  if (process.client && order.paymentId) {
    sessionStorage.setItem('doch_pending_payment_id', order.paymentId)
    sessionStorage.setItem('doch_pending_order_id', order.orderId)
  }
  if (order.paymentStatus === 'FAILED') {
    notify({ title: `Order ${order.reference} created`, message: 'The payment could not be started. You can retry from your order.', variant: 'error' })
    await navigateTo(`/orders/${order.orderId}`)
    return
  }
  if (order.paymentUrl && payMode.value === 'page') {
    notify({ title: `Order ${order.reference} placed`, message: 'Redirecting to payment…', variant: 'success' })
    window.location.href = order.paymentUrl
    return
  }
  // Direct prompt: the customer approves on the phone; the status page polls the provider.
  await navigateTo(`/payment-status?paymentId=${order.paymentId}`)
}

function fieldClass(key: typeof required[number]) {
  return { 'field--error': submitted.value && form[key].trim() === '' }
}
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.cart.title'), to: '/cart' }, { label: $t('market.checkout.title') }]" />

    <section class="co">
      <div class="co__inner">
        <h1 class="co__title">{{ $t('market.checkout.title') }}</h1>

        <!-- Empty cart guard -->
        <div v-if="!items.length" class="co-empty">
          <UIcon name="i-heroicons-shopping-cart" class="co-empty__icon" />
          <p class="co-empty__title">{{ $t('market.checkout.emptyTitle') }}</p>
          <NuxtLink to="/shop" class="co-empty__btn">{{ $t('market.checkout.continueShopping') }}</NuxtLink>
        </div>

        <form v-else class="co-grid" @submit.prevent="placeOrder">
          <!-- Details -->
          <div class="co-main">
            <div class="co-card">
              <h2 class="co-card__title">{{ $t('market.checkout.shippingDetails') }}</h2>
              <div class="co-fields">
                <label class="field" :class="fieldClass('name')">
                  <span class="field__label">{{ $t('market.checkout.fullName') }}</span>
                  <input v-model="form.name" type="text" class="field__input" placeholder="John Doe" />
                </label>
                <label class="field" :class="fieldClass('email')">
                  <span class="field__label">{{ $t('market.checkout.email') }}</span>
                  <input v-model="form.email" type="email" class="field__input" placeholder="you@example.com" />
                </label>
                <label class="field" :class="fieldClass('phone')">
                  <span class="field__label">{{ $t('market.checkout.phone') }}</span>
                  <input v-model="form.phone" type="tel" class="field__input" placeholder="+237 6XX XXX XXX" />
                </label>
                <MarketLocationPicker v-model="location" :invalid="submitted && !location" />
              </div>
            </div>

            <div class="co-card">
              <h2 class="co-card__title">{{ $t('market.checkout.paymentMethod') }}</h2>
              <div class="co-pays">
                <label
                  v-for="p in payments" :key="p.id"
                  class="pay" :class="{ 'pay--on': payment === p.id }"
                >
                  <input v-model="payment" type="radio" :value="p.id" class="pay__radio" />
                  <UIcon :name="p.icon" class="pay__icon" />
                  <span class="pay__text">
                    <span class="pay__label">{{ p.label }}</span>
                    <span class="pay__desc">{{ p.desc }}</span>
                  </span>
                </label>
              </div>
              <div class="co-fields" style="margin-top:0.75rem">
                <label class="pay" :class="{ 'pay--on': payMode === 'prompt' }">
                  <input v-model="payMode" type="radio" value="prompt" class="pay__radio" />
                  <span class="pay__text"><span class="pay__label">Send a payment request to my phone</span>
                    <span class="pay__desc">Approve it with your Mobile Money PIN.</span></span>
                </label>
                <label v-if="payMode === 'prompt'" class="field" :class="{ 'field--error': submitted && !momoValid }">
                  <span class="field__label">Mobile Money number (9 digits)</span>
                  <input v-model="momoNumber" type="tel" class="field__input" placeholder="6XXXXXXXX" />
                </label>
                <label class="pay" :class="{ 'pay--on': payMode === 'page' }">
                  <input v-model="payMode" type="radio" value="page" class="pay__radio" />
                  <span class="pay__text"><span class="pay__label">Pay on the secure Fapshi payment page</span>
                    <span class="pay__desc">You will be redirected and brought back here.</span></span>
                </label>
              </div>
            </div>
          </div>

          <!-- Summary -->
          <aside class="co-summary">
            <h2 class="co-summary__title">{{ $t('market.checkout.yourOrder') }}</h2>
            <div class="co-summary__items">
              <div v-for="item in items" :key="item.product.id" class="co-line">
                <span class="co-line__thumb">
                  <img v-if="item.product.image" :src="item.product.image" :alt="item.product.title" />
                  <span class="co-line__qty">{{ item.qty }}</span>
                </span>
                <span class="co-line__name">{{ item.product.title }}</span>
                <span class="co-line__price">{{ format(item.product.price * item.qty) }}</span>
              </div>
            </div>

            <div class="co-summary__row"><span>{{ $t('market.checkout.subTotal') }}</span><span>{{ format(subtotal) }}</span></div>
            <template v-if="quote">
              <div v-for="b in quote.sellerBreakdowns" :key="b.sellerId" class="co-summary__row">
                <span>Delivery from {{ b.sellerName }} ({{ b.distanceKm?.toFixed?.(1) ?? b.distanceKm }} km)</span><span>{{ format(b.deliveryFee) }}</span>
              </div>
              <div class="co-summary__total"><span>{{ $t('market.checkout.total') }}</span><span>{{ format(quote.totalAmount) }}</span></div>
            </template>
            <div v-else class="co-summary__row">
              <span>{{ $t('market.checkout.shipping') }}</span>
              <span>{{ quoting ? 'Calculating…' : quoteError || 'Choose a delivery location' }}</span>
            </div>

            <button type="submit" class="co-place" :disabled="placing || quoting || !quote">
              <UIcon name="i-heroicons-lock-closed" class="co-place__icon" />
              {{ placing ? $t('market.checkout.placing') : $t('market.checkout.placeOrder') }}
            </button>
            <p class="co-secure"><UIcon name="i-heroicons-shield-check" /> {{ $t('market.checkout.secure') }}</p>
          </aside>
        </form>
      </div>
    </section>
  </div>
</template>

<style scoped>
.co__inner { max-width: 1280px; margin: 0 auto; padding: 1.75rem 1.5rem 3rem; }
.co__title { font-size: 1.375rem; font-weight: 800; color: #111827; margin: 0 0 1.5rem; }

.co-empty { text-align: center; padding: 4rem 1rem; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; }
.co-empty__icon { width: 3rem; height: 3rem; color: #D1D5DB; }
.co-empty__title { font-size: 1.125rem; font-weight: 700; color: #374151; margin: 1rem 0 1.25rem; }
.co-empty__btn { background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.65rem 1.75rem; font-size: 0.8125rem; font-weight: 700; }

.co-grid { display: grid; grid-template-columns: 1fr 360px; gap: 1.5rem; align-items: start; }
.co-main { display: flex; flex-direction: column; gap: 1.25rem; }
.co-card { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem 1.5rem; }
.co-card__title { font-size: 1rem; font-weight: 700; color: #111827; margin: 0 0 1.25rem; }

.co-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field--wide { grid-column: 1 / -1; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__input { border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.6rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; transition: border-color 0.15s; }
.field__input:focus { border-color: var(--color-primary, #E8202A); }
.field__input--area { resize: vertical; font-family: inherit; }
.field--error .field__input { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }

.co-pays { display: grid; gap: 0.75rem; }
.pay { display: flex; align-items: center; gap: 0.875rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 0.875rem 1rem; cursor: pointer; }
.pay--on { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }
.pay__radio { accent-color: var(--color-primary, #E8202A); width: 1rem; height: 1rem; }
.pay__icon { width: 1.5rem; height: 1.5rem; color: #374151; }
.pay--on .pay__icon { color: var(--color-primary, #E8202A); }
.pay__text { display: flex; flex-direction: column; }
.pay__label { font-size: 0.875rem; font-weight: 600; color: #111827; }
.pay__desc { font-size: 0.75rem; color: #9CA3AF; }

/* Summary */
.co-summary { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; position: sticky; top: 1rem; }
.co-summary__title { font-size: 1rem; font-weight: 700; color: #111827; margin: 0 0 1rem; }
.co-summary__items { display: flex; flex-direction: column; gap: 0.75rem; max-height: 17rem; overflow-y: auto; padding-bottom: 0.75rem; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.co-line { display: flex; align-items: center; gap: 0.625rem; }
.co-line__thumb { position: relative; width: 2.75rem; height: 2.75rem; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; overflow: hidden; flex-shrink: 0; background: #F3F4F6; }
.co-line__thumb img { width: 100%; height: 100%; object-fit: cover; }
.co-line__qty { position: absolute; top: -0.45rem; right: -0.45rem; min-width: 1.15rem; height: 1.15rem; padding: 0 0.25rem; background: #111827; color: #fff; font-size: 0.625rem; font-weight: 700; border-radius: 999px; display: flex; align-items: center; justify-content: center; }
.co-line__name { flex: 1; min-width: 0; font-size: 0.75rem; color: #374151; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.co-line__price { font-size: 0.8125rem; font-weight: 700; color: #111827; white-space: nowrap; }
.co-summary__row { display: flex; justify-content: space-between; font-size: 0.875rem; color: #6B7280; padding: 0.55rem 0; }
.co-summary__total { display: flex; justify-content: space-between; font-size: 1rem; font-weight: 800; color: #111827; padding: 0.75rem 0; border-top: 2px solid #111827; margin: 0.25rem 0 0.875rem; }
.co-place { width: 100%; display: flex; align-items: center; justify-content: center; gap: 0.5rem; background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem; padding: 0.8rem 1rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.co-place:hover:not(:disabled) { background: var(--color-primary-hover, #C8181F); }
.co-place:disabled { opacity: 0.6; cursor: not-allowed; }
.co-place__icon { width: 1rem; height: 1rem; }
.co-secure { display: flex; align-items: center; justify-content: center; gap: 0.35rem; font-size: 0.6875rem; color: #9CA3AF; margin: 0.75rem 0 0; }
.co-secure :deep(svg) { width: 0.875rem; height: 0.875rem; color: #16A34A; }

@media (max-width: 900px) {
  .co-grid { grid-template-columns: 1fr; }
  .co-summary { position: static; }
}
@media (max-width: 520px) { .co-fields { grid-template-columns: 1fr; } }
</style>