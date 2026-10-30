<script setup lang="ts">
useHead({ title: 'Send a Parcel — DOCH Xender' })

import type { PickedLocation } from '../composables/useLocations'
import type { TransportType } from '../composables/usePricing'

const { $t } = useNuxtApp()
const { user } = useAuth()
const { notify } = useNotify()
const { previewQuote } = usePricing()
const { create } = useShipments()
const { formatXAF, formatDuration } = useFormat()

const STEPS = ['Item', 'Weight', 'Route', 'Receiver', 'Review', 'Pay'] as const
const stepLabels = computed(() => [
  $t('xender.steps.item'),
  $t('xender.steps.weight'),
  $t('xender.steps.route'),
  $t('xender.steps.receiver'),
  $t('xender.steps.review'),
  $t('xender.steps.pay'),
])
const step = ref(0) // 0-based index into STEPS
const submitted = ref(false)

// ---- form state ----
const photo = ref<string | null>(null)
const photoFile = ref<File | null>(null)
const photoLoading = ref(false)
const cameraInputEl = ref<HTMLInputElement | null>(null)
const galleryInputEl = ref<HTMLInputElement | null>(null)
const description = ref('')
const weight = ref<number | null>(null)
const transportType = ref<TransportType>('SINGLE_PARCEL')
const pickup = ref<PickedLocation | null>(null)
const destination = ref<PickedLocation | null>(null)
const receiver = reactive({ name: '', phone: '' })
const payMethod = ref<'mtn' | 'orange'>('mtn')
const payPhone = ref(user.value?.phone || '')
const paying = ref(false)

const weightChips = [0.5, 1, 2, 5, 10]

// ---- derived ----
// Pickup and destination can be in different cities/towns (cross-city deliveries
// are allowed) — the only thing we reject is an identical pickup and drop-off.
const sameSpot = computed(() =>
  !!pickup.value && !!destination.value &&
  pickup.value.subdivisionId === destination.value.subdivisionId &&
  pickup.value.landmark.trim().toLowerCase() === destination.value.landmark.trim().toLowerCase())

// Price, road distance and duration come from the backend preview (the amount actually charged).
const priced = ref<{ zone: string; km: number; total: number; etaMin?: number }>({ zone: '', km: 0, total: 0 })
const quoting = ref(false)
const quoteError = ref('')
const addr = (l: PickedLocation) => [l.label, l.cityName, l.regionName, 'Cameroon'].filter(Boolean).join(', ')
async function refreshQuote() {
  priced.value = { zone: '', km: 0, total: 0 }
  quoteError.value = ''
  if (!pickup.value || !destination.value || sameSpot.value) return
  quoting.value = true
  try {
    priced.value = await previewQuote(addr(pickup.value), addr(destination.value), transportType.value)
  } catch (e: any) {
    quoteError.value = e?.data?.message ?? 'Could not price this route. Add more detail to the addresses.'
  } finally {
    quoting.value = false
  }
}
watch([pickup, destination, transportType], refreshQuote)
const km = computed(() => priced.value.km)
const eta = computed(() => priced.value.etaMin ?? 0)

// Cameroon mobile numbers are exactly 9 digits (after the +237 prefix).
const receiverPhoneDigits = computed(() => receiver.phone.replace(/\D/g, ''))
const receiverPhoneValid = computed(() => receiverPhoneDigits.value.length === 9)
const payPhoneDigits = computed(() => payPhone.value.replace(/\D/g, ''))
const payPhoneValid = computed(() => payPhoneDigits.value.length === 9)

// ---- per-step validation ----
const stepValid = computed(() => {
  switch (step.value) {
    case 0: return !!photo.value
    case 1: return !!weight.value && weight.value > 0 && weight.value <= 50
    case 2: return !!pickup.value && !!destination.value && !sameSpot.value && priced.value.total > 0
    case 3: return receiver.name.trim().length >= 2 && receiverPhoneValid.value
    case 4: return true
    case 5: return payPhoneValid.value
    default: return false
  }
})

const stepHint = computed(() => {
  switch (step.value) {
    case 0: return $t('xender.send.hintItem')
    case 1: return $t('xender.send.hintWeight')
    case 2: return $t('xender.send.hintRoute')
    case 3: return $t('xender.send.hintReceiver')
    case 4: return $t('xender.send.hintReview')
    case 5: return $t('xender.send.hintPay')
    default: return ''
  }
})

// ---- photo handling: keep the real File for upload, downscale a copy for preview ----
async function onPhoto(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  photoLoading.value = true
  try {
    photo.value = await resizeImage(file, 900, 0.7)
    photoFile.value = file
  } catch {
    notify({ title: 'Couldn’t load image', message: 'Please try another photo.', variant: 'error' })
  } finally {
    photoLoading.value = false
    input.value = '' // allow re-selecting the same file later
  }
}

function resizeImage(file: File, maxDim: number, quality: number): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const img = new Image()
      img.onload = () => {
        const scale = Math.min(1, maxDim / Math.max(img.width, img.height))
        const w = Math.round(img.width * scale)
        const h = Math.round(img.height * scale)
        const canvas = document.createElement('canvas')
        canvas.width = w; canvas.height = h
        const ctx = canvas.getContext('2d')
        if (!ctx) return reject(new Error('no ctx'))
        ctx.drawImage(img, 0, 0, w, h)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.onerror = reject
      img.src = reader.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// ---- navigation ----
function next() {
  submitted.value = true
  if (!stepValid.value) {
    notify({ title: 'Almost there', message: 'Please complete this step to continue.', variant: 'error' })
    return
  }
  submitted.value = false
  if (step.value < STEPS.length - 1) step.value++
}
function back() {
  submitted.value = false
  if (step.value > 0) step.value--
  else navigateTo('/')
}

async function pay() {
  submitted.value = true
  if (!stepValid.value) {
    notify({ title: 'Add a payment number', message: 'Enter the Mobile Money number to charge.', variant: 'error' })
    return
  }
  paying.value = true

  // 1) Create the delivery (real multipart: request JSON part + image file).
  const res = await create({
    file: photoFile.value,
    description: description.value.trim(),
    weightKg: weight.value || 0,
    transportType: transportType.value,
    pickup: endpointFrom(pickup.value!),
    destination: endpointFrom(destination.value!),
    distanceKm: Math.round(km.value * 10) / 10,
    etaMin: Math.round(eta.value),
    receiver: { name: receiver.name.trim(), phone: receiver.phone.trim() },
    price: priced.value,
    payment: { method: payMethod.value, phone: payPhone.value.trim() },
  })

  if (!res.ok || !res.shipment) {
    paying.value = false
    notify({ title: 'Could not create delivery', message: res.error || 'Please try again.', variant: 'error' })
    return
  }
  paying.value = false

  // The backend started the payment with the request: a Mobile Money prompt was sent to payPhone.
  const s = res.shipment
  if (s.paymentStatus === 'FAILED' || !s.paymentId) {
    notify({ title: `Request ${s.ref} created`, message: s.failureReason || 'The payment could not start. Retry from the shipment page.', variant: 'error' })
    navigateTo(`/shipments/${s.id}?new=1`)
    return
  }
  if (typeof window !== 'undefined') {
    sessionStorage.setItem('doch_pending_payment_id', s.paymentId)
    sessionStorage.setItem('doch_pending_payment_app', 'xender')
    sessionStorage.setItem('doch_pending_shipment_id', s.id)
  }
  notify({ title: 'Approve the payment on your phone', message: `A Mobile Money request was sent to ${payPhone.value.trim()}.`, variant: 'success' })
  navigateTo(`/payment-status?paymentId=${s.paymentId}`)
}

// Flatten a PickedLocation into the shipment's stored endpoint shape.
function endpointFrom(l: PickedLocation) {
  return {
    id: l.subdivisionId,
    name: l.label,        // subdivision (+ landmark) — reads well in compact lists
    area: l.cityName,
    region: l.regionName,
    city: l.cityName,
    subdivision: l.subdivisionName,
    landmark: l.landmark,
    lat: l.lat,
    lng: l.lng,
  }
}

function fieldError(bad: boolean) {
  return { 'field--error': submitted.value && bad }
}
</script>

<template>
  <div class="snd">
    <!-- Stepper -->
    <div class="snd-steps">
      <div
        v-for="(s, i) in stepLabels"
        :key="i"
        class="snd-step"
        :class="{ 'snd-step--active': i === step, 'snd-step--done': i < step }"
      >
        <span class="snd-step__dot">
          <UIcon v-if="i < step" name="i-heroicons-check" />
          <template v-else>{{ i + 1 }}</template>
        </span>
        <span class="snd-step__label">{{ s }}</span>
      </div>
    </div>

    <div class="snd-card">
      <header class="snd-head">
        <h1 class="snd-head__title">{{ step === 5 ? $t('xender.send.confirmPay') : stepLabels[step] }}</h1>
        <p class="snd-head__hint">{{ stepHint }}</p>
      </header>

      <!-- STEP 1: Item photo + description -->
      <div v-if="step === 0" class="snd-body">
         <div class="snd-photo" :class="{ 'snd-photo--has': photo, 'field--error': submitted && !photo }">
            <input ref="cameraInputEl" type="file" accept="image/*" capture="environment" class="snd-photo__input" @change="onPhoto" />
            <input ref="galleryInputEl" type="file" accept="image/*" class="snd-photo__input" @change="onPhoto" />

            <template v-if="photoLoading">
              <UIcon name="i-heroicons-arrow-path" class="snd-photo__spin" />
              <span>{{ $t('xender.send.processing') }}</span>
            </template>

            <template v-else-if="photo">
              <img :src="photo" alt="Item" class="snd-photo__img" />
              <button type="button" class="snd-photo__change" @click="cameraInputEl?.click()">
                <UIcon name="i-heroicons-camera" /> {{ $t('xender.send.photoChange') }}
              </button>
            </template>

            <template v-else>
              <button type="button" class="snd-photo__tap" @click="cameraInputEl?.click()">
                <UIcon name="i-heroicons-camera" class="snd-photo__icon" />
                <span class="snd-photo__cta">{{ $t('xender.send.photoCta') }}</span>
                <span class="snd-photo__sub">{{ $t('xender.send.photoSub') }}</span>
              </button>
              <button type="button" class="snd-photo__gallery-link" @click="galleryInputEl?.click()">
                {{ $t('xender.send.photoUploadLink') }}
              </button>
            </template>
          </div>

        <label class="field">
          <span class="field__label">{{ $t('xender.send.whatsInside') }} <span class="field__opt">{{ $t('xender.send.optional') }}</span></span>
          <input v-model="description" type="text" class="field__input" :placeholder="$t('xender.send.descPh')" />
        </label>

        <div class="snd-chips">
          <button type="button" class="snd-chip" :class="{ 'snd-chip--on': transportType === 'SINGLE_PARCEL' }" @click="transportType = 'SINGLE_PARCEL'">
            {{ $t('xender.send.singleParcel') }}
          </button>
          <button type="button" class="snd-chip" :class="{ 'snd-chip--on': transportType === 'FULL_TRUCK' }" @click="transportType = 'FULL_TRUCK'">
            {{ $t('xender.send.fullTruck') }}
          </button>
        </div>
      </div>

      <!-- STEP 2: Weight -->
      <div v-else-if="step === 1" class="snd-body">
        <label class="field" :class="fieldError(!stepValid)">
          <span class="field__label">{{ $t('xender.send.weightLabel') }}</span>
          <div class="field__control">
            <UIcon name="i-heroicons-scale" class="field__lead" />
            <input v-model.number="weight" type="number" min="0.1" max="50" step="0.1" class="field__input field__input--icon" placeholder="0.0" />
          </div>
        </label>
        <div class="snd-chips">
          <button v-for="c in weightChips" :key="c" type="button" class="snd-chip" :class="{ 'snd-chip--on': weight === c }" @click="weight = c">
            {{ c }} kg
          </button>
        </div>
        <p class="snd-note"><UIcon name="i-heroicons-information-circle" /> {{ $t('xender.send.weightNote') }}</p>
      </div>

      <!-- STEP 3: Route -->
      <div v-else-if="step === 2" class="snd-body">
        <XenderLocationPicker
          v-model="pickup"
          :label="$t('xender.send.from')"
          icon="i-heroicons-map-pin"
          :invalid="submitted && !pickup"
        />
        <XenderLocationPicker
          v-model="destination"
          :label="$t('xender.send.to')"
          icon="i-heroicons-flag"
          :invalid="submitted && !destination"
        />

        <p v-if="submitted && sameSpot" class="field__err">
          <UIcon name="i-heroicons-exclamation-triangle" /> {{ $t('xender.send.sameSpot') }}
        </p>
        <p v-if="quoting" class="field__hint">Calculating road distance and price…</p>
        <p v-else-if="quoteError" class="field__err">
          <UIcon name="i-heroicons-exclamation-triangle" /> {{ quoteError }}
        </p>
        <p v-else-if="priced.total" class="field__hint">Price: <strong>{{ formatXAF(priced.total) }}</strong></p>

        <div v-if="km" class="snd-route-meta">
          <div class="snd-route-meta__item">
            <UIcon name="i-heroicons-arrows-right-left" />
            <div><strong>{{ (Math.round(km * 10) / 10).toFixed(1) }} km</strong><span>{{ $t('xender.send.distance') }}</span></div>
          </div>
          <div class="snd-route-meta__item">
            <UIcon name="i-heroicons-clock" />
            <div><strong>{{ formatDuration(eta) }}</strong><span>{{ $t('xender.send.estDelivery') }}</span></div>
          </div>
        </div>
      </div>

      <!-- STEP 4: Receiver -->
      <div v-else-if="step === 3" class="snd-body">
        <label class="field" :class="fieldError(receiver.name.trim().length < 2)">
          <span class="field__label">{{ $t('xender.send.receiverName') }}</span>
          <div class="field__control">
            <UIcon name="i-heroicons-user" class="field__lead" />
            <input v-model="receiver.name" type="text" class="field__input field__input--icon" :placeholder="$t('xender.send.receiverNamePh')" />
          </div>
        </label>
        <label class="field" :class="fieldError(!receiverPhoneValid)">
          <span class="field__label">{{ $t('xender.send.receiverPhone') }}</span>
          <div class="field__control">
            <span class="field__prefix">+237</span>
            <input v-model="receiver.phone" type="tel" inputmode="numeric" class="field__input field__input--prefix" placeholder="6XX XXX XXX" />
          </div>
          <p v-if="submitted && !receiverPhoneValid" class="field__err">
            <UIcon name="i-heroicons-exclamation-triangle" /> {{ $t('xender.send.phoneError', { n: receiverPhoneDigits.length }) }}
          </p>
          <p v-else class="field__hint">{{ $t('xender.send.receiverHint') }}</p>
        </label>
      </div>

      <!-- STEP 5: Review -->
      <div v-else-if="step === 4" class="snd-body">
        <div class="rev">
          <div class="rev__item">
            <div class="rev__thumb"><img v-if="photo" :src="photo" alt="" /><UIcon v-else name="i-heroicons-cube" /></div>
            <div>
              <p class="rev__name">{{ description || $t('xender.send.parcel') }}</p>
              <p class="rev__sub">{{ weight }} kg</p>
            </div>
            <button class="rev__edit" @click="step = 0">{{ $t('common.edit') }}</button>
          </div>
          
          <div class="rev__row"><UIcon name="i-heroicons-truck" /><span>{{ $t('xender.send.transportTypeRow') }}</span><strong>{{ transportType === 'FULL_TRUCK' ? $t('xender.send.fullTruck') : $t('xender.send.singleParcel') }}</strong><button class="rev__edit" @click="step = 0">{{ $t('common.edit') }}</button></div>

          <div class="rev__row"><UIcon name="i-heroicons-map-pin" /><span>{{ $t('xender.send.from') }}</span><strong>{{ pickup?.label }} · {{ pickup?.cityName }}</strong><button class="rev__edit" @click="step = 2">{{ $t('common.edit') }}</button></div>
          <div class="rev__row"><UIcon name="i-heroicons-flag" /><span>{{ $t('xender.send.to') }}</span><strong>{{ destination?.label }} · {{ destination?.cityName }}</strong></div>
          <div class="rev__row"><UIcon name="i-heroicons-user" /><span>{{ $t('xender.send.receiverRow') }}</span><strong>{{ receiver.name }} (+237 {{ receiver.phone }})</strong><button class="rev__edit" @click="step = 3">{{ $t('common.edit') }}</button></div>
          <div class="rev__row"><UIcon name="i-heroicons-clock" /><span>{{ $t('xender.send.eta') }}</span><strong>{{ formatDuration(eta) }} · {{ (Math.round(km * 10) / 10).toFixed(1) }} km</strong></div>

          <div class="rev__price">
            <div class="rev__price-line"><span>{{ $t('xender.send.zoneRow') }} · {{ priced.zone }}</span><span>{{ formatXAF(priced.total) }}</span></div>
            <div class="rev__price-total"><span>{{ $t('xender.send.totalToPay') }}</span><span>{{ formatXAF(priced.total) }}</span></div></div>
        </div>
      </div>

      <!-- STEP 6: Payment -->
      <div v-else-if="step === 5" class="snd-body">
        <div class="snd-pay-total">
          <span>{{ $t('xender.send.amountDue') }}</span>
          <strong>{{ formatXAF(priced.total) }}</strong>
        </div>

        <p class="field__label" style="margin-bottom:.5rem">{{ $t('xender.send.payWith') }}</p>
        <div class="snd-pay-methods">
          <button type="button" class="snd-pay-method" :class="{ 'snd-pay-method--on': payMethod === 'mtn' }" @click="payMethod = 'mtn'">
            <span class="snd-pay-method__badge snd-pay-method__badge--mtn">MTN</span> MTN MoMo
            <UIcon v-if="payMethod === 'mtn'" name="i-heroicons-check-circle" class="snd-pay-method__check" />
          </button>
          <button type="button" class="snd-pay-method" :class="{ 'snd-pay-method--on': payMethod === 'orange' }" @click="payMethod = 'orange'">
            <span class="snd-pay-method__badge snd-pay-method__badge--orange">OM</span> Orange Money
            <UIcon v-if="payMethod === 'orange'" name="i-heroicons-check-circle" class="snd-pay-method__check" />
          </button>
        </div>

        <label class="field" :class="fieldError(!payPhoneValid)" style="margin-top:1.1rem">
          <span class="field__label">{{ $t('xender.send.momoNumber') }}</span>
          <div class="field__control">
            <span class="field__prefix">+237</span>
            <input v-model="payPhone" type="tel" inputmode="numeric" class="field__input field__input--prefix" placeholder="6XX XXX XXX" />
          </div>
          <p v-if="submitted && !payPhoneValid" class="field__err">
            <UIcon name="i-heroicons-exclamation-triangle" /> {{ $t('xender.send.phoneError', { n: payPhoneDigits.length }) }}
          </p>
        </label>
        <p class="snd-note"><UIcon name="i-heroicons-lock-closed" /> {{ $t('xender.send.payNote') }}</p>
      </div>

      <!-- Footer nav -->
      <div class="snd-foot">
        <button type="button" class="snd-btn snd-btn--ghost" @click="back">
          <UIcon name="i-heroicons-arrow-left" /> {{ step === 0 ? $t('xender.send.cancel') : $t('xender.send.back') }}
        </button>
        <button v-if="step < STEPS.length - 1" type="button" class="snd-btn snd-btn--primary" @click="next">
          {{ $t('xender.send.continue') }} <UIcon name="i-heroicons-arrow-right" />
        </button>
        <button v-else type="button" class="snd-btn snd-btn--primary" :disabled="paying" @click="pay">
          <UIcon v-if="paying" name="i-heroicons-arrow-path" class="snd-spin" />
          {{ paying ? $t('xender.send.processing') : $t('xender.send.pay', { amount: formatXAF(priced.total) }) }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.snd { max-width: 560px; margin: 0 auto; }

/* Stepper */
.snd-steps { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.snd-step { display: flex; flex-direction: column; align-items: center; gap: 0.35rem; flex: 1; position: relative; }
.snd-step:not(:last-child)::after { content: ''; position: absolute; top: 0.85rem; left: 50%; width: 100%; height: 2px; background: #E5E7EB; z-index: 0; }
.snd-step--done:not(:last-child)::after { background: #16A34A; }
.snd-step__dot { position: relative; z-index: 1; width: 1.75rem; height: 1.75rem; border-radius: 50%; background: #fff; border: 2px solid #E5E7EB; color: #9CA3AF; font-size: 0.72rem; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.snd-step--active .snd-step__dot { border-color: #E8202A; color: #E8202A; }
.snd-step--done .snd-step__dot { background: #16A34A; border-color: #16A34A; color: #fff; }
.snd-step__dot :deep(svg) { width: 0.9rem; height: 0.9rem; }
.snd-step__label { font-size: 0.66rem; font-weight: 600; color: #9CA3AF; }
.snd-step--active .snd-step__label { color: #111827; }
.snd-step--done .snd-step__label { color: #16A34A; }

.snd-card { background: #fff; border: 1px solid #E5E7EB; border-radius: 1rem; padding: 1.75rem; }
.snd-head__title { font-size: 1.25rem; font-weight: 800; color: #111827; margin: 0 0 0.3rem; }
.snd-head__hint { font-size: 0.82rem; color: #6B7280; margin: 0 0 1.5rem; }
.snd-body { display: flex; flex-direction: column; gap: 1.1rem; }

/* Photo */
/* Photo */
.snd-photo { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.4rem; min-height: 12rem; border: 2px dashed #E5E7EB; border-radius: 0.85rem; color: #6B7280; overflow: hidden; transition: border-color 0.15s, background 0.15s; }
.snd-photo:hover { border-color: #E8202A; background: #FEF2F2; }
.snd-photo--has { border-style: solid; padding: 0; }
.snd-photo__input { position: absolute; inset: 0; opacity: 0; width: 0; height: 0; pointer-events: none; }
.snd-photo__tap { all: unset; display: flex; flex-direction: column; align-items: center; gap: 0.4rem; cursor: pointer; padding: 1.5rem 1rem; width: 100%; box-sizing: border-box; }
.snd-photo__gallery-link { all: unset; cursor: pointer; font-size: 0.72rem; font-weight: 600; color: #E8202A; padding: 0.4rem 0 0.9rem; }
.snd-photo__gallery-link:hover { text-decoration: underline; }
.snd-photo__icon { width: 2.5rem; height: 2.5rem; color: #E8202A; }
.snd-photo__cta { font-size: 0.85rem; font-weight: 700; color: #111827; }
.snd-photo__sub { font-size: 0.72rem; color: #9CA3AF; }
.snd-photo__img { width: 100%; height: 14rem; object-fit: cover; display: block; }
.snd-photo__change { all: unset; position: absolute; bottom: 0.6rem; right: 0.6rem; display: inline-flex; align-items: center; gap: 0.3rem; background: rgba(0,0,0,0.6); color: #fff; font-size: 0.72rem; font-weight: 600; padding: 0.35rem 0.6rem; border-radius: 999px; cursor: pointer; }
.snd-photo__change :deep(svg) { width: 0.9rem; height: 0.9rem; }
.snd-photo__spin { width: 1.6rem; height: 1.6rem; animation: snd-rot 0.8s linear infinite; }
.snd-photo.field--error { border-color: #E8202A; }

/* fields */
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__opt { color: #9CA3AF; font-weight: 500; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1.05rem; height: 1.05rem; color: #9CA3AF; pointer-events: none; }
.field__prefix { position: absolute; left: 0.75rem; font-size: 0.875rem; font-weight: 600; color: #6B7280; pointer-events: none; }
.field__input { width: 100%; border: 1px solid #E5E7EB; border-radius: 0.5rem; padding: 0.7rem 0.75rem; font-size: 0.9rem; color: #111827; outline: none; background: #fff; transition: border-color 0.15s, box-shadow 0.15s; }
.field__input--icon { padding-left: 2.35rem; }
.field__input--prefix { padding-left: 3.1rem; }
.field__select { appearance: none; cursor: pointer; }
.field__input:focus { border-color: #E8202A; box-shadow: 0 0 0 3px rgba(232,32,42,0.1); }
.field--error .field__input { border-color: #E8202A; background: #FEF2F2; }
.field__hint { font-size: 0.72rem; color: #9CA3AF; margin: 0.1rem 0 0; }
.field__err { display: flex; align-items: center; gap: 0.35rem; font-size: 0.75rem; color: #E8202A; font-weight: 600; margin: 0.1rem 0 0; }
.field__err :deep(svg) { width: 0.95rem; height: 0.95rem; flex-shrink: 0; }

/* weight chips */
.snd-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.snd-chip { border: 1px solid #E5E7EB; background: #fff; border-radius: 999px; padding: 0.4rem 0.9rem; font-size: 0.8rem; font-weight: 600; color: #4B5563; cursor: pointer; }
.snd-chip:hover { border-color: #E8202A; color: #E8202A; }
.snd-chip--on { background: #E8202A; border-color: #E8202A; color: #fff; }

.snd-note { display: flex; align-items: flex-start; gap: 0.4rem; font-size: 0.75rem; color: #6B7280; background: #F9FAFB; border-radius: 0.5rem; padding: 0.7rem; margin: 0; }
.snd-note :deep(svg) { flex-shrink: 0; width: 1rem; height: 1rem; margin-top: 0.05rem; color: #9CA3AF; }

/* route meta */
.snd-route-meta { display: flex; gap: 0.75rem; }
.snd-route-meta__item { flex: 1; display: flex; align-items: center; gap: 0.6rem; background: #FEF2F2; border-radius: 0.6rem; padding: 0.8rem; }
.snd-route-meta__item :deep(svg) { width: 1.3rem; height: 1.3rem; color: #E8202A; }
.snd-route-meta__item strong { display: block; font-size: 0.95rem; font-weight: 800; color: #111827; }
.snd-route-meta__item span { font-size: 0.7rem; color: #6B7280; }

/* review */
.rev { display: flex; flex-direction: column; gap: 0.1rem; }
.rev__item { display: flex; align-items: center; gap: 0.85rem; padding: 0.5rem 0 1rem; border-bottom: 1px solid #F3F4F6; }
.rev__thumb { flex-shrink: 0; width: 3.5rem; height: 3.5rem; border-radius: 0.6rem; background: #F3F4F6; display: flex; align-items: center; justify-content: center; overflow: hidden; color: #9CA3AF; }
.rev__thumb img { width: 100%; height: 100%; object-fit: cover; }
.rev__thumb :deep(svg) { width: 1.6rem; height: 1.6rem; }
.rev__name { font-size: 0.9rem; font-weight: 700; color: #111827; margin: 0; }
.rev__sub { font-size: 0.78rem; color: #6B7280; margin: 0.15rem 0 0; }
.rev__row { display: flex; align-items: center; gap: 0.6rem; padding: 0.7rem 0; border-bottom: 1px solid #F3F4F6; font-size: 0.82rem; }
.rev__row :deep(svg) { width: 1.05rem; height: 1.05rem; color: #9CA3AF; flex-shrink: 0; }
.rev__row > span { color: #9CA3AF; min-width: 4.5rem; }
.rev__row > strong { flex: 1; color: #111827; font-weight: 600; }
.rev__edit { background: none; border: none; color: #E8202A; font-size: 0.75rem; font-weight: 700; cursor: pointer; }
.rev__edit:hover { text-decoration: underline; }
.rev__price { margin-top: 1rem; background: #F9FAFB; border-radius: 0.6rem; padding: 1rem; }
.rev__price-line { display: flex; justify-content: space-between; font-size: 0.8rem; color: #4B5563; margin-bottom: 0.5rem; }
.rev__price-total { display: flex; justify-content: space-between; font-size: 0.95rem; font-weight: 800; color: #111827; padding-top: 0.6rem; border-top: 1px dashed #D1D5DB; }
.rev__price-total span:last-child { color: #E8202A; }

/* payment */
.snd-pay-total { display: flex; align-items: center; justify-content: space-between; background: #FEF2F2; border-radius: 0.7rem; padding: 1rem 1.2rem; }
.snd-pay-total span { font-size: 0.82rem; font-weight: 600; color: #6B7280; }
.snd-pay-total strong { font-size: 1.35rem; font-weight: 800; color: #E8202A; }
.snd-pay-methods { display: flex; flex-direction: column; gap: 0.6rem; }
.snd-pay-method { position: relative; display: flex; align-items: center; gap: 0.7rem; border: 1px solid #E5E7EB; background: #fff; border-radius: 0.6rem; padding: 0.85rem 1rem; font-size: 0.85rem; font-weight: 600; color: #111827; cursor: pointer; text-align: left; }
.snd-pay-method--on { border-color: #E8202A; background: #FEF2F2; }
.snd-pay-method__badge { font-size: 0.68rem; font-weight: 800; color: #fff; padding: 0.2rem 0.4rem; border-radius: 0.3rem; }
.snd-pay-method__badge--mtn { background: #FFCC00; color: #111827; }
.snd-pay-method__badge--orange { background: #FF6600; }
.snd-pay-method__check { position: absolute; right: 1rem; width: 1.2rem; height: 1.2rem; color: #E8202A; }

/* footer */
.snd-foot { display: flex; gap: 0.75rem; margin-top: 1.75rem; }
.snd-btn { flex: 1; display: inline-flex; align-items: center; justify-content: center; gap: 0.4rem; border-radius: 0.5rem; padding: 0.8rem 1rem; font-size: 0.82rem; font-weight: 700; cursor: pointer; border: none; transition: all 0.15s; }
.snd-btn--ghost { flex: 0 0 auto; background: #fff; border: 1px solid #E5E7EB; color: #4B5563; }
.snd-btn--ghost:hover { border-color: #9CA3AF; }
.snd-btn--primary { background: #E8202A; color: #fff; }
.snd-btn--primary:hover:not(:disabled) { background: #C8181F; }
.snd-btn--primary:disabled { opacity: 0.7; cursor: not-allowed; }
.snd-btn :deep(svg) { width: 1.05rem; height: 1.05rem; }
.snd-spin { animation: snd-rot 0.8s linear infinite; }
@keyframes snd-rot { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .snd-steps { margin-bottom: 1.1rem; }
  .snd-step__dot { width: 1.5rem; height: 1.5rem; font-size: 0.66rem; }
  .snd-step:not(:last-child)::after { top: 0.72rem; }
  .snd-step__label { font-size: 0.58rem; }
  .snd-card { padding: 1.1rem; border-radius: 0.85rem; }
  .snd-head__title { font-size: 1.1rem; }
  .snd-head__hint { font-size: 0.78rem; margin-bottom: 1.1rem; }
  .snd-body { gap: 0.9rem; }
  .snd-photo { min-height: 9.5rem; }
  .snd-foot { margin-top: 1.25rem; }
  .snd-btn { padding: 0.7rem 0.85rem; font-size: 0.8rem; }
}
</style>
