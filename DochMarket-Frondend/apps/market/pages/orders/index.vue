<script setup lang="ts">
useHead({ title: 'My orders — Doch Market' })

const { history, errText } = useOrder()
const { format } = useCurrency()

const orders = ref<any[]>([])
const loading = ref(true)
const error = ref('')

const label = (s?: string) => (s ? s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase()) : '—')
const badge = (s?: string) =>
  s === 'DELIVERED' || s === 'COMPLETED' ? 'bg-green-100 text-green-800'
    : s === 'PAYMENT_FAILED' || s === 'CANCELLED' || s === 'RETURNED' ? 'bg-red-100 text-red-800'
      : s === 'PENDING' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'

onMounted(async () => {
  try {
    orders.value = (await history()) ?? []
  } catch (e: any) {
    error.value = errText(e, 'Could not load your orders')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: 'My orders' }]" />
    <section class="max-w-5xl mx-auto px-4 py-6 space-y-4">
      <h1 class="text-2xl font-extrabold text-gray-900">My orders</h1>
      <div v-if="error" class="rounded bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
      <div v-if="loading" class="text-gray-500">Loading…</div>
      <div v-else-if="!orders.length" class="rounded-lg border bg-white p-10 text-center">
        <p class="text-gray-600 mb-4">You have not placed any order yet.</p>
        <NuxtLink to="/shop" class="inline-block bg-red-600 text-white px-5 py-2 rounded font-semibold">Start shopping</NuxtLink>
      </div>
      <NuxtLink v-for="o in orders" v-else :key="o.orderId" :to="`/orders/${o.orderId}`"
                class="block rounded-lg border bg-white p-4 hover:border-red-400">
        <div class="flex flex-wrap justify-between gap-2">
          <div>
            <div class="font-mono font-bold">{{ o.reference }}</div>
            <div class="text-xs text-gray-500">{{ new Date(o.createdAt).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) }}</div>
          </div>
          <div class="text-right">
            <span class="px-2 py-0.5 rounded text-xs" :class="badge(o.status)">{{ label(o.status) }}</span>
            <div class="font-bold mt-1">{{ format(o.totalAmount) }}</div>
          </div>
        </div>
        <div class="text-sm text-gray-600 mt-2">{{ o.items.map((i: any) => `${i.quantity} × ${i.productName}`).join(', ') }}</div>
        <div class="flex flex-wrap gap-3 mt-2 text-xs">
          <span v-for="l in o.deliveryLegs" :key="l.deliveryId" class="text-gray-500">
            Package <span class="font-mono">{{ l.trackingNumber }}</span> · {{ label(l.status) }}
          </span>
          <span v-if="o.canPay" class="text-red-700 font-semibold">Payment required</span>
          <span v-if="o.canReview" class="text-amber-700 font-semibold">Rate this order</span>
        </div>
      </NuxtLink>
    </section>
  </div>
</template>
