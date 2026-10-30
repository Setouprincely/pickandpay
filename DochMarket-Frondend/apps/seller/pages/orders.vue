<script setup lang="ts">
// Seller fulfilment: paid orders containing this seller's products.
// The seller seals the package and uploads a photo, which releases it to DOCH dispatch.
const { get, post } = useApi()
const { format } = useMoney()
const { notify } = useNotify()

const orders = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const filter = ref<'todo' | 'active' | 'done' | 'all'>('todo')
const uploading = ref<string | null>(null)

const TODO = ['PENDING_PREPARATION']
const ACTIVE = ['PENDING_ASSIGNMENT', 'DRIVER_ASSIGNED', 'PICKED_UP', 'AT_HUB', 'IN_TRANSIT', 'FAILED']
const DONE = ['DELIVERED', 'RETURNED', 'CANCELLED']

const label = (s?: string) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')
const dt = (v?: string) => (v ? new Date(v).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '—')
const badge = (s?: string) =>
  s === 'PENDING_PREPARATION' ? 'bg-amber-100 text-amber-800'
    : s === 'DELIVERED' ? 'bg-green-100 text-green-800'
      : DONE.includes(s ?? '') || s === 'FAILED' ? 'bg-red-100 text-red-800' : 'bg-blue-100 text-blue-800'

async function load() {
  loading.value = true
  error.value = ''
  try {
    orders.value = (await get<any[]>('/api/orders/seller/me')) ?? []
  } catch (e: any) {
    error.value = e?.data?.message ?? `Could not load orders (${e?.status ?? 'network error'})`
  } finally {
    loading.value = false
  }
}

const shown = computed(() => orders.value.filter(o => {
  const s = o.sellerDeliveryStatus
  if (filter.value === 'todo') return TODO.includes(s)
  if (filter.value === 'active') return ACTIVE.includes(s)
  if (filter.value === 'done') return DONE.includes(s)
  return true
}))
const counts = computed(() => ({
  todo: orders.value.filter(o => TODO.includes(o.sellerDeliveryStatus)).length,
  active: orders.value.filter(o => ACTIVE.includes(o.sellerDeliveryStatus)).length,
  done: orders.value.filter(o => DONE.includes(o.sellerDeliveryStatus)).length,
}))

async function markReady(order: any, ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    notify({ title: 'Use a JPEG, PNG or WEBP photo of 5 MB or less.', variant: 'error' })
    return
  }
  uploading.value = order.sellerDeliveryId
  try {
    const form = new FormData()
    form.append('image', file, file.name)
    await post(`/api/deliveries/market/${order.sellerDeliveryId}/ready`, form)
    notify({ title: `Package ${order.sellerTrackingNumber} is ready. DOCH dispatch has been notified.` })
    await load()
  } catch (e: any) {
    notify({ title: e?.data?.message ?? `Upload failed (${e?.status ?? 'network error'})`, variant: 'error' })
  } finally {
    uploading.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="p-4 md:p-6 space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Orders to fulfil</h1>
        <p class="text-sm text-gray-500">Paid orders that include your products. Seal each package, write its tracking number on it and upload a photo to release it to DOCH dispatch.</p>
      </div>
      <UButton variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="load">Refresh</UButton>
    </div>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in (['todo','active','done','all'] as const)" :key="t" class="px-3 py-1.5 rounded-full text-sm border"
              :class="filter === t ? 'bg-red-600 text-white border-red-600' : 'bg-white'" @click="filter = t">
        {{ t === 'todo' ? `To prepare (${counts.todo})` : t === 'active' ? `With DOCH (${counts.active})` : t === 'done' ? `Closed (${counts.done})` : `All (${orders.length})` }}
      </button>
    </div>

    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
    <div v-if="loading" class="text-gray-500">Loading…</div>
    <div v-else-if="!shown.length" class="rounded-xl border bg-white p-8 text-center text-gray-500">No orders in this list.</div>

    <div v-for="o in shown" v-else :key="o.orderId" class="rounded-xl border bg-white p-4 space-y-3">
      <div class="flex flex-wrap items-start justify-between gap-2">
        <div>
          <div class="font-mono font-bold">{{ o.reference }}</div>
          <div class="text-xs text-gray-500">Placed {{ dt(o.createdAt) }} · paid {{ dt(o.paidAt) }}</div>
        </div>
        <span class="px-2 py-0.5 rounded text-xs" :class="badge(o.sellerDeliveryStatus)">{{ label(o.sellerDeliveryStatus) }}</span>
      </div>
      <div class="text-sm">
        <div v-for="i in o.items" :key="i.productId" class="flex justify-between border-b py-1">
          <span>{{ i.quantity }} × {{ i.productName }}</span><span>{{ format(i.total_price) }}</span>
        </div>
        <div class="flex justify-between font-semibold pt-1"><span>Your sale</span><span>{{ format(o.productTotal) }}</span></div>
      </div>
      <div class="text-sm rounded-lg bg-gray-50 p-3">
        Package <strong class="font-mono">{{ o.sellerTrackingNumber }}</strong>
        <template v-if="o.deliveryLegs?.[0]?.driverName"> · driver {{ o.deliveryLegs[0].driverName }} ({{ o.deliveryLegs[0].driverPhone }})</template>
        <div v-if="o.deliveryLegs?.[0]?.failureReason" class="text-red-700 text-xs">{{ o.deliveryLegs[0].failureReason }}</div>
        <ol v-if="o.deliveryLegs?.[0]?.events?.length" class="mt-2 border-l-2 pl-3 space-y-0.5">
          <li v-for="(e, idx) in o.deliveryLegs[0].events.filter((x: any) => x.message)" :key="idx" class="text-xs">
            <span class="text-gray-500">{{ dt(e.occurredAt) }}</span> — {{ e.message }}
          </li>
        </ol>
      </div>
      <div v-if="o.sellerDeliveryStatus === 'PENDING_PREPARATION'">
        <label class="inline-flex items-center gap-2 cursor-pointer rounded-lg bg-red-600 text-white px-4 py-2 text-sm font-semibold"
               :class="uploading === o.sellerDeliveryId ? 'opacity-60 pointer-events-none' : ''">
          <UIcon name="i-lucide-camera" />
          {{ uploading === o.sellerDeliveryId ? 'Uploading…' : 'Upload sealed-package photo & mark ready' }}
          <input type="file" accept="image/jpeg,image/png,image/webp" capture="environment" class="hidden" @change="markReady(o, $event)">
        </label>
      </div>
    </div>
  </div>
</template>
