<script setup lang="ts">
import DeliveryConfirmModal from '~/apps/driver/components/DeliveryConfirmModal.vue'
import AvailabilityToggle from '~/apps/driver/components/AvailabilityToggle.vue'

// Driver job list: every action calls the backend state machine; nothing is simulated locally.
//   GET  /api/drivers/me/deliveries          active jobs (with tracking history)
//   GET  /api/deliveries/me/optimized-route  ordered pickup / drop-off stops
//   POST /api/deliveries/{id}/pickup         scan or type the package tracking code
//   POST /api/deliveries/{id}/in-transit     out for delivery / re-attempt
//   POST /api/deliveries/{id}/fail           failed attempt with reason
//   POST /api/deliveries/{id}/not-collected  pickup impossible
//   POST /api/deliveries/{id}/complete       recipient OTP + proof-of-delivery photo (multipart)
//   PUT  /api/drivers/me/location            GPS position (used for route ordering and live tracking)
const { get, post, put } = useDriverApi()
const { token } = useAuth()
const config = useRuntimeConfig()

const tab = ref<'jobs' | 'route'>('jobs')
const jobs = ref<any[]>([])
const stops = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const busyId = ref<string | null>(null)

const label = (s?: string) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')
const vehicleLabel = (c?: string) => ({ BICYCLE: 'Bicycle', K_TRUCK: 'Suzuki Carry', VAN: 'Toyota HiAce' } as Record<string, string>)[c ?? ''] ?? '—'
const errText = (e: any, what: string) => e?.data?.message ?? `${what} (${e?.status ?? 'network error'})`

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [j, r] = await Promise.all([
      get<any[]>('/api/drivers/me/deliveries'),
      get<any[]>('/api/deliveries/me/optimized-route'),
    ])
    jobs.value = j ?? []
    stops.value = r ?? []
  } catch (e: any) {
    error.value = errText(e, 'Could not load your jobs')
  } finally {
    loading.value = false
  }
}

// --- GPS ---
const position = ref<{ latitude: string; longitude: string } | null>(null)
let gpsTimer: ReturnType<typeof setInterval> | undefined
function readGps(): Promise<{ latitude: string; longitude: string } | null> {
  return new Promise(resolve => {
    if (!process.client || !navigator.geolocation) return resolve(null)
    navigator.geolocation.getCurrentPosition(
      p => resolve({ latitude: p.coords.latitude.toFixed(6), longitude: p.coords.longitude.toFixed(6) }),
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 },
    )
  })
}
async function pushGps() {
  const p = await readGps()
  if (!p) return
  position.value = p
  try { await put('/api/drivers/me/location', p) } catch { /* next heartbeat retries */ }
}

async function act(id: string, fn: () => Promise<any>, ok: string) {
  busyId.value = id
  error.value = ''
  notice.value = ''
  try {
    await fn()
    notice.value = ok
    await load()
  } catch (e: any) {
    error.value = errText(e, 'Action failed')
  } finally {
    busyId.value = null
  }
}

// --- pickup scan ---
const scanFor = ref<any | null>(null)
const scanCode = ref('')
const scanning = ref(false)
const video = ref<HTMLVideoElement | null>(null)
let stream: MediaStream | null = null
const canCameraScan = computed(() => process.client && 'BarcodeDetector' in window && !!navigator.mediaDevices)

function openScan(job: any) {
  scanFor.value = job
  scanCode.value = ''
}
async function startCamera() {
  if (!canCameraScan.value) return
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    scanning.value = true
    await nextTick()
    if (!video.value) return
    video.value.srcObject = stream
    await video.value.play()
    const detector = new (window as any).BarcodeDetector({ formats: ['qr_code', 'code_128'] })
    const loop = async () => {
      if (!scanning.value || !video.value) return
      try {
        const codes = await detector.detect(video.value)
        if (codes?.length) {
          scanCode.value = codes[0].rawValue
          stopCamera()
          return
        }
      } catch { /* keep trying */ }
      requestAnimationFrame(loop)
    }
    loop()
  } catch {
    error.value = 'Camera not available. Type the tracking number instead.'
    stopCamera()
  }
}
function stopCamera() {
  scanning.value = false
  stream?.getTracks().forEach(t => t.stop())
  stream = null
}
function closeScan() {
  stopCamera()
  scanFor.value = null
}
async function confirmPickup() {
  const job = scanFor.value
  if (!job || !scanCode.value.trim()) return
  const p = await readGps()
  await act(job.deliveryId, () => post(`/api/deliveries/${job.deliveryId}/pickup`, { trackingCode: scanCode.value.trim(), ...(p ?? {}) }),
    `Package ${job.trackingNumber} collected.`)
  if (!error.value) closeScan()
}

// --- exceptions ---
const FAIL_REASONS = [
  { value: 'RECIPIENT_UNAVAILABLE', label: 'Recipient unavailable' },
  { value: 'ADDRESS_NOT_FOUND', label: 'Address not found' },
  { value: 'REFUSED', label: 'Recipient refused the package' },
  { value: 'DAMAGED', label: 'Package damaged' },
  { value: 'DETAILS_MISMATCH', label: 'Package details do not match' },
]
const PICKUP_REASONS = [
  { value: 'PACKAGE_NOT_READY', label: 'Package not ready' },
  { value: 'SENDER_UNAVAILABLE', label: 'Sender / seller unavailable' },
  { value: 'DETAILS_MISMATCH', label: 'Package details do not match' },
  { value: 'VEHICLE_ISSUE', label: 'Vehicle issue' },
  { value: 'DRIVER_UNAVAILABLE', label: 'I cannot do this job' },
]
const issueFor = ref<any | null>(null)
const issueKind = ref<'fail' | 'not-collected'>('fail')
const issueReason = ref('')
const issueNote = ref('')
function openIssue(job: any, kind: 'fail' | 'not-collected') {
  issueFor.value = job
  issueKind.value = kind
  issueReason.value = ''
  issueNote.value = ''
}
async function submitIssue() {
  const job = issueFor.value
  if (!job || !issueReason.value) return
  const p = await readGps()
  await act(job.deliveryId, () => post(`/api/deliveries/${job.deliveryId}/${issueKind.value}`,
    { reason: issueReason.value, note: issueNote.value.trim() || undefined, ...(p ?? {}) }),
    issueKind.value === 'fail' ? 'Failed attempt recorded. Dispatch has been notified.' : 'Reported. The package is back with dispatch.')
  if (!error.value) issueFor.value = null
}

// --- delivery confirmation (OTP + photo) ---
const confirmFor = ref<any | null>(null)
async function completeDelivery(payload: { otp: string; image: File }) {
  const job = confirmFor.value
  if (!job) return
  busyId.value = job.deliveryId
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('otp', payload.otp)
    fd.append('image', payload.image)
    const p = await readGps()
    if (p) { fd.append('latitude', p.latitude); fd.append('longitude', p.longitude) }
    const t = token.value || (process.client ? localStorage.getItem('driver-token') : null)
    await $fetch(`${config.public.apiBase}/api/deliveries/${job.deliveryId}/complete`, {
      method: 'POST',
      headers: t ? { Authorization: `Bearer ${t}` } : {},
      body: fd,
    })
    confirmFor.value = null
    notice.value = `Package ${job.trackingNumber} delivered.`
    await load()
  } catch (e: any) {
    error.value = errText(e, 'Delivery could not be confirmed')
    await load()
    if (confirmFor.value) confirmFor.value = jobs.value.find(j => j.deliveryId === job.deliveryId) ?? null
  } finally {
    busyId.value = null
  }
}

const mapsLink = (lat?: string, lng?: string, address?: string) =>
  lat && lng ? `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`
    : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address ?? '')}`

const counts = computed(() => ({
  pickup: jobs.value.filter(j => j.status === 'DRIVER_ASSIGNED').length,
  carrying: jobs.value.filter(j => ['PICKED_UP', 'IN_TRANSIT', 'FAILED'].includes(j.status)).length,
}))

onMounted(async () => {
  await load()
  pushGps()
  gpsTimer = setInterval(pushGps, 60000)
})
onBeforeUnmount(() => {
  clearInterval(gpsTimer)
  stopCamera()
})
</script>

<template>
  <div class="p-4 space-y-3 max-w-3xl">
    <AvailabilityToggle />
    <div class="flex gap-2">
      <button class="px-3 py-1.5 rounded-full text-sm border" :class="tab === 'jobs' ? 'bg-red-600 text-white border-red-600' : 'bg-white'" @click="tab = 'jobs'">
        Jobs ({{ jobs.length }})
      </button>
      <button class="px-3 py-1.5 rounded-full text-sm border" :class="tab === 'route' ? 'bg-red-600 text-white border-red-600' : 'bg-white'" @click="tab = 'route'">
        Trip stops ({{ stops.length }})
      </button>
      <button class="ml-auto text-sm text-gray-600" @click="load">Refresh</button>
    </div>
    <p class="text-xs text-gray-500">{{ counts.pickup }} to collect · {{ counts.carrying }} on board
      <span v-if="position"> · GPS {{ position.latitude }}, {{ position.longitude }}</span></p>

    <div v-if="error" class="rounded bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
    <div v-if="notice" class="rounded bg-green-50 text-green-700 p-3 text-sm">{{ notice }}</div>
    <div v-if="loading" class="text-gray-500">Loading…</div>
    <div v-else-if="!jobs.length" class="rounded-xl border bg-white p-8 text-center text-gray-500">
      No jobs assigned. Make sure you are online so dispatch can assign you.
    </div>

    <!-- Jobs -->
    <template v-else-if="tab === 'jobs'">
      <div v-for="j in jobs" :key="j.deliveryId" class="rounded-xl border bg-white p-4 space-y-2">
        <div class="flex justify-between gap-2">
          <div>
            <div class="font-mono font-bold">{{ j.trackingNumber }}</div>
            <div class="text-xs text-gray-500">{{ j.source === 'MARKET' ? `Order ${j.orderReference}` : 'DochXender parcel' }}
              · {{ j.packageWeightKg ?? '?' }} kg · {{ j.vehicle ? `${j.vehicle.name} (${j.vehicle.plateNumber})` : vehicleLabel(j.requiredVehicleCategory) }}</div>
          </div>
          <span class="px-2 py-0.5 rounded text-xs h-fit" :class="j.status === 'FAILED' ? 'bg-red-100 text-red-800' : j.status === 'DRIVER_ASSIGNED' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'">{{ label(j.status) }}</span>
        </div>

        <div class="text-sm grid sm:grid-cols-2 gap-2">
          <div :class="j.status === 'DRIVER_ASSIGNED' ? 'font-semibold' : 'text-gray-500'">
            <div class="text-xs uppercase text-gray-400">Pickup</div>
            {{ j.pickupLocationAddress }}
            <div class="text-xs">{{ j.sellerName }} {{ j.sellerPhone }}</div>
            <a :href="mapsLink(j.pickupLocationCoords?.latitude, j.pickupLocationCoords?.longitude, j.pickupLocationAddress)" target="_blank" class="text-xs text-red-700 underline">Navigate</a>
          </div>
          <div :class="j.status !== 'DRIVER_ASSIGNED' ? 'font-semibold' : 'text-gray-500'">
            <div class="text-xs uppercase text-gray-400">Deliver to</div>
            {{ j.receiverAddress }}
            <div class="text-xs">{{ j.receiverName }} <a v-if="j.receiverPhone" :href="`tel:${j.receiverPhone}`" class="underline">{{ j.receiverPhone }}</a></div>
            <a :href="mapsLink(j.receiverLocationCoords?.latitude, j.receiverLocationCoords?.longitude, j.receiverAddress)" target="_blank" class="text-xs text-red-700 underline">Navigate</a>
          </div>
        </div>
        <div v-if="j.deliveryNote" class="text-xs text-gray-600">Note: {{ j.deliveryNote }}</div>
        <div v-if="j.failureReason" class="text-xs text-red-700">Last issue: {{ j.failureReason }}</div>
        <div v-if="j.otpLocked" class="text-xs text-red-700 font-semibold">Delivery code locked after wrong attempts — call dispatch.</div>
        <div v-if="j.parcelImageUrls?.length" class="flex gap-2">
          <img v-for="u in j.parcelImageUrls" :key="u" :src="u" class="h-14 w-14 rounded object-cover border" alt="Sealed package">
        </div>

        <div class="flex flex-wrap gap-2 pt-1">
          <template v-if="j.status === 'DRIVER_ASSIGNED'">
            <button class="bg-red-600 text-white rounded px-3 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busyId === j.deliveryId" @click="openScan(j)">Scan &amp; confirm pickup</button>
            <button class="border rounded px-3 py-2 text-sm" :disabled="busyId === j.deliveryId" @click="openIssue(j, 'not-collected')">Cannot collect</button>
          </template>
          <template v-else-if="j.status === 'PICKED_UP' || j.status === 'FAILED'">
            <button class="bg-red-600 text-white rounded px-3 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busyId === j.deliveryId"
                    @click="act(j.deliveryId, () => post(`/api/deliveries/${j.deliveryId}/in-transit`, {}), 'On the way to the recipient.')">
              {{ j.status === 'FAILED' ? 'Re-attempt delivery' : 'Start delivery run' }}
            </button>
            <button v-if="j.status === 'PICKED_UP'" class="border rounded px-3 py-2 text-sm" :disabled="busyId === j.deliveryId || j.otpLocked" @click="confirmFor = j">Deliver now</button>
          </template>
          <template v-else-if="j.status === 'IN_TRANSIT'">
            <button class="bg-green-600 text-white rounded px-3 py-2 text-sm font-semibold disabled:opacity-50" :disabled="busyId === j.deliveryId || j.otpLocked" @click="confirmFor = j">Enter code &amp; deliver</button>
            <button class="border rounded px-3 py-2 text-sm" :disabled="busyId === j.deliveryId" @click="openIssue(j, 'fail')">Delivery failed</button>
          </template>
        </div>
      </div>
    </template>

    <!-- Trip stops -->
    <template v-else>
      <ol class="space-y-2">
        <li v-for="s in stops" :key="`${s.deliveryId}-${s.stopType}`" class="rounded-xl border bg-white p-3 flex gap-3">
          <div class="h-8 w-8 rounded-full flex items-center justify-center font-bold text-white shrink-0" :class="s.stopType === 'PICKUP' ? 'bg-amber-500' : 'bg-green-600'">{{ s.sequence }}</div>
          <div class="text-sm flex-1">
            <div class="font-semibold">{{ s.stopType === 'PICKUP' ? 'Pickup' : 'Drop-off' }} · <span class="font-mono">{{ s.trackingNumber }}</span></div>
            <div>{{ s.address }}</div>
            <div class="text-xs text-gray-500">{{ s.contactName }} <a v-if="s.contactPhone" :href="`tel:${s.contactPhone}`" class="underline">{{ s.contactPhone }}</a>
              <span v-if="s.legDistanceKm != null"> · ~{{ s.legDistanceKm }} km from previous stop</span></div>
          </div>
          <a :href="mapsLink(s.latitude, s.longitude, s.address)" target="_blank" class="text-sm text-red-700 underline self-center">Go</a>
        </li>
      </ol>
      <p class="text-xs text-gray-500">Order is computed from your last GPS position; each pickup always comes before its drop-off.</p>
    </template>

    <!-- Pickup scan dialog -->
    <div v-if="scanFor" class="fixed inset-0 bg-black/50 flex items-end sm:items-center justify-center z-50" @click.self="closeScan">
      <div class="bg-white rounded-t-2xl sm:rounded-2xl p-5 w-full max-w-md space-y-3">
        <h3 class="font-bold">Confirm pickup of {{ scanFor.trackingNumber }}</h3>
        <p class="text-sm text-gray-600">Scan the QR code / label on the package, or type the tracking number written on it.</p>
        <video v-show="scanning" ref="video" class="w-full rounded" playsinline muted />
        <button v-if="canCameraScan && !scanning" class="w-full border rounded py-2 text-sm" @click="startCamera">Scan with camera</button>
        <input v-model="scanCode" class="w-full border rounded px-3 py-2 font-mono uppercase" placeholder="DCH-XXXXXXXXXX" />
        <div class="flex gap-2">
          <button class="flex-1 border rounded py-2" @click="closeScan">Cancel</button>
          <button class="flex-1 bg-red-600 text-white rounded py-2 font-semibold disabled:opacity-50" :disabled="!scanCode.trim() || busyId === scanFor.deliveryId" @click="confirmPickup">Confirm pickup</button>
        </div>
      </div>
    </div>

    <!-- Exception dialog -->
    <div v-if="issueFor" class="fixed inset-0 bg-black/50 flex items-end sm:items-center justify-center z-50" @click.self="issueFor = null">
      <div class="bg-white rounded-t-2xl sm:rounded-2xl p-5 w-full max-w-md space-y-3">
        <h3 class="font-bold">{{ issueKind === 'fail' ? 'Delivery attempt failed' : 'Package could not be collected' }} — {{ issueFor.trackingNumber }}</h3>
        <select v-model="issueReason" class="w-full border rounded px-3 py-2">
          <option value="">Select a reason…</option>
          <option v-for="r in (issueKind === 'fail' ? FAIL_REASONS : PICKUP_REASONS)" :key="r.value" :value="r.value">{{ r.label }}</option>
        </select>
        <textarea v-model="issueNote" rows="3" maxlength="500" class="w-full border rounded p-2 text-sm" placeholder="Details for dispatch (optional)" />
        <div class="flex gap-2">
          <button class="flex-1 border rounded py-2" @click="issueFor = null">Cancel</button>
          <button class="flex-1 bg-red-600 text-white rounded py-2 font-semibold disabled:opacity-50" :disabled="!issueReason || busyId === issueFor.deliveryId" @click="submitIssue">Report</button>
        </div>
      </div>
    </div>

    <DeliveryConfirmModal v-if="confirmFor" @close="confirmFor = null" @confirm="completeDelivery" />
  </div>
</template>
