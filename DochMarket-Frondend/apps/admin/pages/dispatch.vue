<script setup lang="ts">
// Dispatch & hub operations: converts paid, sealed packages into driver jobs and handles exceptions.
const { get, post } = useApi()
const { money, dt, label, statusClass, errorMessage, vehicleLabel } = useOps()
const route = useRoute()

const TABS = [
  { key: 'ready', label: 'Ready to assign', statuses: 'PENDING_ASSIGNMENT,AT_HUB' },
  { key: 'prep', label: 'Seller preparing', statuses: 'PENDING_PREPARATION' },
  { key: 'assigned', label: 'Assigned', statuses: 'DRIVER_ASSIGNED' },
  { key: 'road', label: 'On the road', statuses: 'PICKED_UP,IN_TRANSIT' },
  { key: 'exceptions', label: 'Exceptions', statuses: 'FAILED' },
  { key: 'closed', label: 'Closed', statuses: 'DELIVERED,RETURNED,CANCELLED' },
  { key: 'all', label: 'All', statuses: '' },
]
const RANK: Record<string, number> = { BICYCLE: 1, K_TRUCK: 2, VAN: 3 }

const tab = ref(route.query.q ? 'all' : 'ready')
const q = ref(String(route.query.q ?? ''))
const page = ref(0)
const rows = ref<any[]>([])
const totalPages = ref(0)
const summary = ref<Record<string, number>>({})
const loading = ref(false)
const error = ref('')

const selected = ref<any | null>(null)
const busy = ref(false)
const msg = ref('')
const actionError = ref('')

const drivers = ref<any[]>([])
const offlineDrivers = ref<any[]>([])
const vehicles = ref<any[]>([])
const assign = reactive({ driverId: '', vehicleId: '', scheduledFor: '', note: '' })
const hub = reactive({ trackingCode: '', hubName: 'Yaounde Central Hub', note: '' })

const tabCount = (statuses: string) =>
  statuses ? statuses.split(',').reduce((n, s) => n + (summary.value[s] ?? 0), 0) : Object.values(summary.value).reduce((a, b) => a + b, 0)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const t = TABS.find(x => x.key === tab.value)!
    const params = new URLSearchParams({ page: String(page.value), size: '20' })
    if (t.statuses) params.set('status', t.statuses)
    if (q.value.trim()) params.set('q', q.value.trim())
    const [res, sum] = await Promise.all([get<any>(`/api/deliveries/all?${params}`), get<any>('/api/deliveries/summary')])
    rows.value = res.content ?? []
    totalPages.value = res.totalPages ?? res.page?.totalPages ?? 0
    summary.value = sum ?? {}
    if (rows.value.length === 1 && q.value.trim()) select(rows.value[0])
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not load the dispatch queue')
  } finally {
    loading.value = false
  }
}

async function loadResources() {
  try {
    const [d, v, all] = await Promise.all([
      get<any[]>('/api/drivers/available'),
      get<any[]>('/api/fleet?status=ACTIVE'),
      get<any[]>('/api/drivers').catch(() => []),
    ])
    drivers.value = d ?? []
    vehicles.value = v ?? []
    const online = new Set(drivers.value.map(x => x.driverId))
    offlineDrivers.value = (all ?? []).filter(x => !online.has(x.driverId))
  } catch (e: any) {
    actionError.value = errorMessage(e, 'Could not load drivers / vehicles')
  }
}

async function select(row: any) {
  msg.value = ''
  actionError.value = ''
  try {
    selected.value = await get<any>(`/api/deliveries/${row.deliveryId}`)
    hub.trackingCode = ''
    assign.driverId = ''
    assign.vehicleId = ''
    assign.note = ''
    assign.scheduledFor = ''
    await loadResources()
  } catch (e: any) {
    actionError.value = errorMessage(e, 'Could not load package')
  }
}

// Only vehicles that satisfy the package's required category (decision matrix).
const suitableVehicles = computed(() => {
  const need = RANK[selected.value?.requiredVehicleCategory ?? ''] ?? 0
  return vehicles.value.filter(v => (RANK[v.category] ?? 0) >= need)
})

// Pre-select the vehicle already held by the chosen driver when it is suitable.
watch(() => assign.driverId, id => {
  const d = drivers.value.find(x => x.driverId === id)
  if (d?.vehicleId && suitableVehicles.value.some(v => v.id === d.vehicleId)) assign.vehicleId = d.vehicleId
})

async function act(fn: () => Promise<any>, success: string) {
  busy.value = true
  actionError.value = ''
  msg.value = ''
  try {
    selected.value = await fn()
    msg.value = success
    await load()
  } catch (e: any) {
    actionError.value = errorMessage(e, 'Action failed')
  } finally {
    busy.value = false
  }
}

const id = () => selected.value.deliveryId
const doAssign = () => act(() => post(`/api/deliveries/${id()}/assign`, {
  driverId: assign.driverId, vehicleId: assign.vehicleId, note: assign.note || undefined,
  scheduledFor: assign.scheduledFor ? new Date(assign.scheduledFor).toISOString().slice(0, 19) : undefined,
}), 'Driver and vehicle assigned. The driver has been notified.')

function withReason(prompt: string, required = true) {
  const reason = window.prompt(prompt)
  if (required && (!reason || !reason.trim())) return null
  return reason?.trim() ?? ''
}
const doUnassign = () => { const r = withReason('Reason for removing the driver (e.g. driver unavailable):'); if (r !== null) act(() => post(`/api/deliveries/${id()}/unassign`, { reason: 'DRIVER_UNAVAILABLE', note: r }), 'Driver removed; package is back in the queue.') }
const doHub = () => act(() => post(`/api/deliveries/${id()}/hub-scan`, { ...hub }), 'Package scanned in at the hub.')
const doReturn = () => { const r = withReason('Reason for returning the package to the sender:'); if (r !== null) act(() => post(`/api/deliveries/${id()}/return`, { note: r }), 'Package marked as returned to sender.') }
const doCancel = () => { const r = withReason('Reason for cancelling this package:'); if (r !== null) act(() => post(`/api/deliveries/${id()}/cancel`, { note: r }), 'Package cancelled.') }
const doResetOtp = () => { const r = withReason('Confirm you verified the recipient identity (note):'); if (r !== null) act(() => post(`/api/deliveries/${id()}/reset-otp`, { note: r }), 'A new delivery code was sent to the customer.') }

const can = computed(() => {
  const s = selected.value?.status
  return {
    assign: ['PENDING_ASSIGNMENT', 'AT_HUB', 'DRIVER_ASSIGNED'].includes(s),
    unassign: s === 'DRIVER_ASSIGNED',
    hub: ['PICKED_UP', 'IN_TRANSIT', 'FAILED'].includes(s),
    ret: ['AT_HUB', 'FAILED'].includes(s),
    cancel: ['PENDING_PAYMENT', 'PENDING_PREPARATION', 'PENDING_ASSIGNMENT', 'DRIVER_ASSIGNED'].includes(s),
    resetOtp: selected.value?.otpLocked || (selected.value?.otpFailedAttempts ?? 0) > 0,
  }
})

watch(tab, () => { page.value = 0; load() })
onMounted(load)
</script>

<template>
  <div class="p-6 space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Dispatch</h1>
        <p class="text-sm text-gray-500">Assign drivers and vehicles, process hub scans and handle delivery exceptions.</p>
      </div>
      <form class="flex gap-2" @submit.prevent="page = 0; load()">
        <input v-model="q" class="border rounded-lg px-3 py-2 text-sm w-72" placeholder="Tracking DCH-…, order DO-…, seller" />
        <UButton type="submit" color="error" icon="i-lucide-search" :loading="loading">Search</UButton>
      </form>
    </div>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in TABS" :key="t.key" class="px-3 py-1.5 rounded-full text-sm border"
              :class="tab === t.key ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700'" @click="tab = t.key">
        {{ t.label }} <span class="ml-1 opacity-75">{{ tabCount(t.statuses) }}</span>
      </button>
    </div>

    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>

    <div class="grid gap-4" :class="selected ? 'xl:grid-cols-[1fr_560px]' : ''">
      <div class="bg-white rounded-xl border overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-600">
            <tr><th class="p-3">Tracking</th><th class="p-3">Order / type</th><th class="p-3">Pickup → Drop-off</th><th class="p-3">Load</th><th class="p-3">Driver</th><th class="p-3">Status</th></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="p-6 text-center text-gray-500">Loading…</td></tr>
            <tr v-else-if="!rows.length"><td colspan="6" class="p-6 text-center text-gray-500">No packages in this queue.</td></tr>
            <tr v-for="d in rows" v-else :key="d.deliveryId" class="border-t hover:bg-gray-50 cursor-pointer"
                :class="selected?.deliveryId === d.deliveryId ? 'bg-red-50' : ''" @click="select(d)">
              <td class="p-3 font-mono font-semibold whitespace-nowrap">{{ d.trackingNumber }}</td>
              <td class="p-3">{{ d.orderReference ?? 'Xender' }}<div class="text-xs text-gray-500">{{ d.sellerName ?? d.receiverName }}</div></td>
              <td class="p-3 text-xs max-w-xs">{{ d.pickupLocationAddress }}<br>→ {{ d.receiverAddress }}</td>
              <td class="p-3 text-xs whitespace-nowrap">{{ d.packageWeightKg ?? '?' }} kg<br>{{ vehicleLabel(d.requiredVehicleCategory) }}</td>
              <td class="p-3 text-xs">{{ d.driverName ?? '—' }}<div v-if="d.vehicle">{{ d.vehicle.name }}</div></td>
              <td class="p-3"><span class="px-2 py-0.5 rounded text-xs whitespace-nowrap" :class="statusClass(d.status)">{{ label(d.status) }}</span></td>
            </tr>
          </tbody>
        </table>
        <div class="flex items-center justify-between p-3 border-t text-sm">
          <span>Page {{ totalPages ? page + 1 : 0 }} / {{ totalPages }}</span>
          <div class="flex gap-2">
            <UButton size="sm" variant="outline" :disabled="page === 0" @click="page--; load()">Previous</UButton>
            <UButton size="sm" variant="outline" :disabled="page + 1 >= totalPages" @click="page++; load()">Next</UButton>
          </div>
        </div>
      </div>

      <aside v-if="selected" class="bg-white rounded-xl border p-4 space-y-4 self-start">
        <div class="flex items-start justify-between">
          <div>
            <h2 class="text-lg font-bold font-mono">{{ selected.trackingNumber }}</h2>
            <p class="text-xs text-gray-500">{{ selected.source }} · {{ selected.orderReference ?? '' }}
              <NuxtLink v-if="selected.orderReference" :to="`/orders?q=${selected.orderReference}`" class="text-red-700 underline ml-1">open order</NuxtLink></p>
          </div>
          <div class="flex items-center gap-2">
            <span class="px-2 py-0.5 rounded text-xs" :class="statusClass(selected.status)">{{ label(selected.status) }}</span>
            <UButton size="xs" variant="ghost" icon="i-lucide-x" @click="selected = null" />
          </div>
        </div>
        <div v-if="actionError" class="rounded bg-red-50 text-red-700 p-2 text-sm">{{ actionError }}</div>
        <div v-if="msg" class="rounded bg-green-50 text-green-700 p-2 text-sm">{{ msg }}</div>

        <div class="grid grid-cols-2 gap-3 text-sm">
          <div><span class="text-gray-500">Pickup</span><br>{{ selected.pickupLocationAddress }}<div class="text-xs">{{ selected.sellerName }} {{ selected.sellerPhone }}</div></div>
          <div><span class="text-gray-500">Deliver to</span><br>{{ selected.receiverAddress }}<div class="text-xs">{{ selected.receiverName }} {{ selected.receiverPhone }}</div></div>
          <div><span class="text-gray-500">Package</span><br>{{ selected.packageWeightKg ?? 'unknown' }} kg<span v-if="selected.packageMaxDimensionCm">, max {{ selected.packageMaxDimensionCm }} cm</span></div>
          <div><span class="text-gray-500">Required vehicle</span><br><strong>{{ vehicleLabel(selected.requiredVehicleCategory) }}</strong></div>
          <div v-if="selected.driverName"><span class="text-gray-500">Driver</span><br>{{ selected.driverName }} · {{ selected.driverPhone }}</div>
          <div v-if="selected.vehicle"><span class="text-gray-500">Vehicle</span><br>{{ selected.vehicle.name }} ({{ selected.vehicle.plateNumber }})</div>
          <div v-if="selected.currentHub"><span class="text-gray-500">Hub</span><br>{{ selected.currentHub }}</div>
          <div v-if="selected.failureReason" class="col-span-2 text-red-700"><span class="text-gray-500">Reason</span><br>{{ selected.failureReason }}</div>
          <div v-if="selected.otpFailedAttempts" class="col-span-2" :class="selected.otpLocked ? 'text-red-700 font-semibold' : 'text-amber-700'">
            Wrong delivery codes entered: {{ selected.otpFailedAttempts }}<span v-if="selected.otpLocked"> — LOCKED</span>
          </div>
        </div>

        <div v-if="selected.parcelImageUrls?.length" class="flex gap-2">
          <a v-for="u in selected.parcelImageUrls" :key="u" :href="u" target="_blank"><img :src="u" class="h-16 w-16 object-cover rounded border" alt="Parcel"></a>
          <a v-if="selected.proofOfDeliveryUrl" :href="selected.proofOfDeliveryUrl" target="_blank"><img :src="selected.proofOfDeliveryUrl" class="h-16 w-16 object-cover rounded border-2 border-green-500" alt="Proof of delivery"></a>
        </div>

        <!-- Assign / reassign -->
        <div v-if="can.assign" class="rounded-lg border p-3 space-y-2">
          <h3 class="font-semibold text-sm">{{ selected.status === 'DRIVER_ASSIGNED' ? 'Reassign driver / vehicle' : selected.status === 'AT_HUB' ? 'Assign onward trip' : 'Assign driver and vehicle' }}</h3>
          <select v-model="assign.driverId" class="border rounded px-2 py-1.5 text-sm w-full">
            <option value="">— Online driver —</option>
            <option v-for="d in drivers" :key="d.driverId" :value="d.driverId">
              {{ d.fullName }} · {{ d.activeDeliveries }} active job(s){{ d.vehicleName ? ' · has ' + d.vehicleName : '' }}
            </option>
            <optgroup v-if="offlineDrivers.length" label="Offline (cannot be assigned)">
              <option v-for="d in offlineDrivers" :key="d.driverId" :value="d.driverId" disabled>
                {{ d.fullName || d.email }} · offline
              </option>
            </optgroup>
          </select>
          <p v-if="!drivers.length" class="text-xs text-amber-700">
            No driver is online{{ offlineDrivers.length ? ` (${offlineDrivers.length} registered driver(s) are offline)` : '' }}.
            A driver goes online with the Online switch on the Dashboard or Deliveries page of the driver app; signing in alone is not enough.
            <button class="underline ml-1" @click="loadResources">Refresh</button>
          </p>
          <select v-model="assign.vehicleId" class="border rounded px-2 py-1.5 text-sm w-full">
            <option value="">— Vehicle ({{ vehicleLabel(selected.requiredVehicleCategory) }} or larger) —</option>
            <option v-for="v in suitableVehicles" :key="v.id" :value="v.id">
              {{ v.name }} · {{ v.plateNumber }} · {{ v.categoryLabel }}{{ v.assignedDriverName ? ' (held by ' + v.assignedDriverName + ')' : '' }}
            </option>
          </select>
          <p v-if="!suitableVehicles.length" class="text-xs text-amber-700">No active vehicle of a suitable category. Add one under Reports → Fleet.</p>
          <div class="flex gap-2">
            <input v-model="assign.scheduledFor" type="datetime-local" class="border rounded px-2 py-1.5 text-sm" title="Scheduled for (optional)" />
            <input v-model="assign.note" class="border rounded px-2 py-1.5 text-sm flex-1" placeholder="Note for the driver (optional)" />
          </div>
          <UButton color="error" icon="i-lucide-user-check" :disabled="!assign.driverId || !assign.vehicleId" :loading="busy" @click="doAssign">Assign</UButton>
        </div>

        <!-- Hub scan -->
        <div v-if="can.hub" class="rounded-lg border p-3 space-y-2">
          <h3 class="font-semibold text-sm">Hub check-in (scan, inspect, sort)</h3>
          <input v-model="hub.trackingCode" class="border rounded px-2 py-1.5 text-sm w-full font-mono" placeholder="Scan or type tracking number" />
          <input v-model="hub.hubName" class="border rounded px-2 py-1.5 text-sm w-full" placeholder="Hub name" />
          <input v-model="hub.note" class="border rounded px-2 py-1.5 text-sm w-full" placeholder="Inspection notes" />
          <UButton icon="i-lucide-scan-line" :disabled="!hub.trackingCode || !hub.hubName" :loading="busy" @click="doHub">Record hub scan</UButton>
        </div>

        <div class="flex flex-wrap gap-2">
          <UButton v-if="can.unassign" variant="outline" icon="i-lucide-user-x" :loading="busy" @click="doUnassign">Remove driver</UButton>
          <UButton v-if="can.resetOtp" variant="outline" icon="i-lucide-key-round" :loading="busy" @click="doResetOtp">Reset delivery code</UButton>
          <UButton v-if="can.ret" variant="outline" color="error" icon="i-lucide-undo-2" :loading="busy" @click="doReturn">Return to sender</UButton>
          <UButton v-if="can.cancel" variant="outline" color="error" icon="i-lucide-ban" :loading="busy" @click="doCancel">Cancel package</UButton>
        </div>

        <div>
          <h3 class="font-semibold text-sm mb-1">Tracking history</h3>
          <ol class="border-l-2 border-gray-200 pl-3 space-y-1">
            <li v-for="(e, i) in selected.events" :key="i" class="text-xs">
              <span class="text-gray-500">{{ dt(e.occurredAt) }}</span> —
              <strong>{{ e.fromStatus !== e.toStatus && e.toStatus ? label(e.toStatus) : label(e.eventType) }}</strong>
              <span v-if="e.message"> · {{ e.message }}</span>
              <span v-if="e.internalNote" class="text-gray-500"> · {{ e.internalNote }}</span>
              <span v-if="e.hubName"> · {{ e.hubName }}</span>
              <span v-if="e.actorName" class="text-gray-400"> ({{ e.actorName }})</span>
            </li>
          </ol>
        </div>
        <p class="text-xs text-gray-400">Price: {{ money(selected.price) }} · created {{ dt(selected.createdAt) }}</p>
      </aside>
    </div>
  </div>
</template>
