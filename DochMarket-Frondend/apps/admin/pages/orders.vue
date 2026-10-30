<script setup lang="ts">
// Order management: every marketplace order from the shared database, searchable by
// order ID, reference (DO-...), package tracking number (DCH-...) or customer.
const { get, post } = useApi()
const { money, dt, label, statusClass, errorMessage, vehicleLabel } = useOps()
const route = useRoute()

const STATUSES = ['', 'PENDING', 'PAYMENT_FAILED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED', 'COMPLETED']

const q = ref(String(route.query.q ?? ''))
const status = ref('')
const startDate = ref('')
const endDate = ref('')
const page = ref(0)
const size = 20

const rows = ref<any[]>([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

const selected = ref<any | null>(null)
const detailLoading = ref(false)
const detailError = ref('')
const actionMsg = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: String(page.value), size: String(size) })
    if (q.value.trim()) params.set('q', q.value.trim())
    if (status.value) params.set('status', status.value)
    if (startDate.value) params.set('startDate', startDate.value)
    if (endDate.value) params.set('endDate', endDate.value)
    const res = await get<any>(`/api/orders/all?${params}`)
    rows.value = res.content ?? []
    total.value = res.totalElements ?? res.page?.totalElements ?? 0
    totalPages.value = res.totalPages ?? res.page?.totalPages ?? 0
    if (rows.value.length === 1 && q.value.trim()) open(rows.value[0].orderId)
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not load orders')
    rows.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}

async function open(orderId: string) {
  detailLoading.value = true
  detailError.value = ''
  actionMsg.value = ''
  try {
    selected.value = await get<any>(`/api/orders/${orderId}`)
  } catch (e: any) {
    detailError.value = errorMessage(e, 'Could not load order')
  } finally {
    detailLoading.value = false
  }
}

const cancelling = ref(false)
async function cancelOrder() {
  if (!selected.value) return
  const reason = window.prompt(`Cancel order ${selected.value.reference}? Enter a reason (required):`)
  if (!reason || !reason.trim()) return
  cancelling.value = true
  try {
    selected.value = await post<any>(`/api/orders/${selected.value.orderId}/cancel`, { reason: reason.trim() })
    actionMsg.value = 'Order cancelled. Stock released and refund flagged when applicable.'
    await load()
  } catch (e: any) {
    detailError.value = errorMessage(e, 'Cancellation failed')
  } finally {
    cancelling.value = false
  }
}

async function reconcilePayment() {
  const pid = selected.value?.paymentDetails?.paymentId
  if (!pid) return
  try {
    await post(`/api/payments/${pid}/reconcile`, {})
    await open(selected.value.orderId)
    actionMsg.value = 'Payment status refreshed from Fapshi.'
    await load()
  } catch (e: any) {
    detailError.value = errorMessage(e, 'Reconciliation failed')
  }
}

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Orders</h1>
        <p class="text-sm text-gray-500">{{ total }} order(s) in the shared database</p>
      </div>
      <form class="flex flex-wrap items-end gap-2" @submit.prevent="search">
        <input v-model="q" class="border rounded-lg px-3 py-2 text-sm w-72" placeholder="Order ID, DO-…, DCH-…, customer email or name" />
        <select v-model="status" class="border rounded-lg px-3 py-2 text-sm" @change="search">
          <option v-for="s in STATUSES" :key="s" :value="s">{{ s ? label(s) : 'All statuses' }}</option>
        </select>
        <input v-model="startDate" type="date" class="border rounded-lg px-2 py-2 text-sm" title="From" />
        <input v-model="endDate" type="date" class="border rounded-lg px-2 py-2 text-sm" title="To" />
        <UButton type="submit" icon="i-lucide-search" color="error" :loading="loading">Search</UButton>
        <UButton variant="ghost" icon="i-lucide-refresh-cw" @click="load">Refresh</UButton>
      </form>
    </div>

    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>

    <div class="grid gap-4" :class="selected ? 'lg:grid-cols-[1fr_520px]' : ''">
      <div class="bg-white rounded-xl border overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-600">
            <tr>
              <th class="p-3">Reference</th><th class="p-3">Customer</th><th class="p-3">Total</th>
              <th class="p-3">Payment</th><th class="p-3">Order</th><th class="p-3">Packages</th><th class="p-3">Created</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="p-6 text-center text-gray-500">Loading…</td></tr>
            <tr v-else-if="!rows.length"><td colspan="7" class="p-6 text-center text-gray-500">No orders match these filters.</td></tr>
            <tr v-for="o in rows" v-else :key="o.orderId"
                class="border-t hover:bg-gray-50 cursor-pointer" :class="selected?.orderId === o.orderId ? 'bg-red-50' : ''"
                @click="open(o.orderId)">
              <td class="p-3 font-mono font-semibold">{{ o.reference }}</td>
              <td class="p-3">{{ o.customerName }}<div class="text-xs text-gray-500">{{ o.customerEmail }}</div></td>
              <td class="p-3 whitespace-nowrap">{{ money(o.totalAmount) }}</td>
              <td class="p-3"><span class="px-2 py-0.5 rounded text-xs" :class="statusClass(o.paymentDetails?.status)">{{ label(o.paymentDetails?.status) }}</span></td>
              <td class="p-3"><span class="px-2 py-0.5 rounded text-xs" :class="statusClass(o.status)">{{ label(o.status) }}</span></td>
              <td class="p-3">
                <div v-for="l in o.deliveryLegs" :key="l.deliveryId" class="text-xs whitespace-nowrap">
                  <span class="font-mono">{{ l.trackingNumber }}</span> · {{ label(l.status) }}
                </div>
              </td>
              <td class="p-3 whitespace-nowrap text-gray-500">{{ dt(o.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="flex items-center justify-between p-3 border-t text-sm">
          <span>Page {{ totalPages ? page + 1 : 0 }} / {{ totalPages }}</span>
          <div class="flex gap-2">
            <UButton size="sm" variant="outline" :disabled="page === 0 || loading" @click="page--; load()">Previous</UButton>
            <UButton size="sm" variant="outline" :disabled="page + 1 >= totalPages || loading" @click="page++; load()">Next</UButton>
          </div>
        </div>
      </div>

      <aside v-if="selected || detailLoading" class="bg-white rounded-xl border p-4 space-y-4 self-start">
        <div v-if="detailLoading" class="text-gray-500">Loading order…</div>
        <template v-else-if="selected">
          <div class="flex items-start justify-between">
            <div>
              <h2 class="text-lg font-bold font-mono">{{ selected.reference }}</h2>
              <p class="text-xs text-gray-500 font-mono">{{ selected.orderId }}</p>
            </div>
            <UButton size="xs" variant="ghost" icon="i-lucide-x" @click="selected = null" />
          </div>
          <div v-if="detailError" class="rounded bg-red-50 text-red-700 p-2 text-sm">{{ detailError }}</div>
          <div v-if="actionMsg" class="rounded bg-green-50 text-green-700 p-2 text-sm">{{ actionMsg }}</div>

          <div class="grid grid-cols-2 gap-2 text-sm">
            <div><span class="text-gray-500">Status</span><br><span class="px-2 py-0.5 rounded text-xs" :class="statusClass(selected.status)">{{ label(selected.status) }}</span></div>
            <div><span class="text-gray-500">Placed</span><br>{{ dt(selected.createdAt) }}</div>
            <div><span class="text-gray-500">Customer</span><br>{{ selected.customerName }}<br><span class="text-xs">{{ selected.customerEmail }} · {{ selected.contactPhone }}</span></div>
            <div><span class="text-gray-500">Deliver to</span><br>{{ selected.deliveryAddress }}</div>
          </div>

          <div>
            <h3 class="font-semibold mb-1">Items</h3>
            <div v-for="i in selected.items" :key="i.productId" class="flex justify-between text-sm border-b py-1">
              <span>{{ i.quantity }} × {{ i.productName }} <span class="text-xs text-gray-500">({{ i.sellerName }})</span></span>
              <span>{{ money(i.total_price) }}</span>
            </div>
            <div class="flex justify-between text-sm pt-1"><span>Products</span><span>{{ money(selected.productTotal) }}</span></div>
            <div class="flex justify-between text-sm"><span>Delivery</span><span>{{ money(selected.deliveryFee) }}</span></div>
            <div class="flex justify-between font-semibold"><span>Total</span><span>{{ money(selected.totalAmount) }}</span></div>
          </div>

          <div v-if="selected.paymentDetails" class="text-sm rounded-lg bg-gray-50 p-3">
            <h3 class="font-semibold mb-1">Payment</h3>
            <div>Status: <span class="px-2 py-0.5 rounded text-xs" :class="statusClass(selected.paymentDetails.status)">{{ label(selected.paymentDetails.status) }}</span>
              <span class="text-xs text-gray-500"> (provider: {{ selected.paymentDetails.providerStatus ?? '—' }})</span></div>
            <div>Amount: {{ money(selected.paymentDetails.amount) }} · {{ selected.paymentDetails.paymentMethod }} · {{ selected.paymentDetails.paymentFlow }}</div>
            <div class="font-mono text-xs">Fapshi ref: {{ selected.paymentDetails.providerReference ?? '—' }}</div>
            <div v-if="selected.paymentDetails.paidAt">Paid: {{ dt(selected.paymentDetails.paidAt) }}</div>
            <div v-if="selected.paymentDetails.failureReason" class="text-red-700">{{ selected.paymentDetails.failureReason }}</div>
            <UButton v-if="selected.paymentDetails.status === 'PENDING'" size="xs" class="mt-2" variant="outline" icon="i-lucide-refresh-cw" @click="reconcilePayment">Check with Fapshi</UButton>
          </div>

          <div>
            <h3 class="font-semibold mb-1">Packages</h3>
            <div v-for="l in selected.deliveryLegs" :key="l.deliveryId" class="border rounded-lg p-3 mb-2 text-sm">
              <div class="flex justify-between">
                <span class="font-mono font-semibold">{{ l.trackingNumber }}</span>
                <span class="px-2 py-0.5 rounded text-xs" :class="statusClass(l.status)">{{ label(l.status) }}</span>
              </div>
              <div class="text-xs text-gray-600">Seller: {{ l.sellerName }} · {{ l.packageWeightKg ?? '?' }} kg · needs {{ vehicleLabel(l.requiredVehicleCategory) }}</div>
              <div v-if="l.driverName" class="text-xs">Driver: {{ l.driverName }} ({{ l.driverPhone }}) · {{ l.vehicleName }} {{ l.vehiclePlate }}</div>
              <div v-if="l.failureReason" class="text-xs text-red-700">Reason: {{ l.failureReason }}</div>
              <ol v-if="l.events?.length" class="mt-2 border-l-2 border-gray-200 pl-3 space-y-1">
                <li v-for="(e, idx) in l.events" :key="idx" class="text-xs">
                  <span class="text-gray-500">{{ dt(e.occurredAt) }}</span> —
                  <strong>{{ e.toStatus ? label(e.toStatus) : label(e.eventType) }}</strong>
                  <span v-if="e.message"> · {{ e.message }}</span>
                  <span v-if="e.internalNote" class="text-gray-500"> · {{ e.internalNote }}</span>
                  <span v-if="e.actorName" class="text-gray-400"> ({{ e.actorName }})</span>
                </li>
              </ol>
              <NuxtLink :to="`/dispatch?q=${l.trackingNumber}`" class="text-xs text-red-700 underline">Open in dispatch</NuxtLink>
            </div>
          </div>

          <div v-if="selected.review" class="text-sm rounded-lg bg-amber-50 p-3">
            <h3 class="font-semibold">Customer review</h3>
            <div>Order {{ selected.review.rating }}/5<span v-if="selected.review.deliveryRating"> · delivery {{ selected.review.deliveryRating }}/5</span></div>
            <div v-if="selected.review.comment" class="italic">“{{ selected.review.comment }}”</div>
          </div>

          <div class="flex gap-2">
            <UButton v-if="selected.canCancel" color="error" variant="outline" icon="i-lucide-ban" :loading="cancelling" @click="cancelOrder">Cancel order</UButton>
            <NuxtLink :to="`/audit?entityType=ORDER&entityId=${selected.orderId}`"><UButton variant="ghost" icon="i-lucide-scroll-text">Audit trail</UButton></NuxtLink>
          </div>
        </template>
      </aside>
    </div>
  </div>
</template>
