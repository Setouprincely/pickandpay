<script setup lang="ts">
// Product moderation: listings submitted by sellers are published only after approval.
const { get, put } = useApi()
const { money, dt, label, statusClass, errorMessage } = useOps()

const status = ref('PENDING_REVIEW')
const q = ref('')
const page = ref(0)
const rows = ref<any[]>([])
const totalPages = ref(0)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const msg = ref('')
const busyId = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: String(page.value), size: '20' })
    if (status.value) params.set('status', status.value)
    if (q.value.trim()) params.set('q', q.value.trim())
    const res = await get<any>(`/api/product/admin?${params}`)
    rows.value = res.content ?? []
    total.value = res.totalElements ?? res.page?.totalElements ?? 0
    totalPages.value = res.totalPages ?? res.page?.totalPages ?? 0
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not load products')
  } finally {
    loading.value = false
  }
}

async function approve(p: any) {
  busyId.value = p.id
  msg.value = ''
  try {
    await put(`/api/product/${p.id}/approve`, {})
    msg.value = `"${p.name}" approved and published.`
    await load()
  } catch (e: any) {
    error.value = errorMessage(e, 'Approval failed')
  } finally {
    busyId.value = null
  }
}

async function reject(p: any) {
  const reason = window.prompt(`Reason for rejecting "${p.name}" (sent to the seller):`)
  if (!reason || !reason.trim()) return
  busyId.value = p.id
  msg.value = ''
  try {
    await put(`/api/product/${p.id}/reject`, { reason: reason.trim() })
    msg.value = `"${p.name}" rejected. The seller has been notified.`
    await load()
  } catch (e: any) {
    error.value = errorMessage(e, 'Rejection failed')
  } finally {
    busyId.value = null
  }
}

watch(status, () => { page.value = 0; load() })
onMounted(load)
</script>

<template>
  <div class="p-6 space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Product moderation</h1>
        <p class="text-sm text-gray-500">{{ total }} product(s). Customers only see published, active products of verified sellers.</p>
      </div>
      <form class="flex gap-2" @submit.prevent="page = 0; load()">
        <select v-model="status" class="border rounded-lg px-3 py-2 text-sm">
          <option value="PENDING_REVIEW">Pending review</option>
          <option value="PUBLISHED">Published</option>
          <option value="REJECTED">Rejected</option>
          <option value="">All</option>
        </select>
        <input v-model="q" class="border rounded-lg px-3 py-2 text-sm w-64" placeholder="Product, seller or ID" />
        <UButton type="submit" color="error" icon="i-lucide-search" :loading="loading">Search</UButton>
      </form>
    </div>
    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
    <div v-if="msg" class="rounded-lg bg-green-50 text-green-700 p-3 text-sm">{{ msg }}</div>

    <div class="bg-white rounded-xl border overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-left text-gray-600">
          <tr><th class="p-3">Product</th><th class="p-3">Seller</th><th class="p-3">Price</th><th class="p-3">Stock</th><th class="p-3">Weight</th><th class="p-3">Status</th><th class="p-3">Submitted</th><th class="p-3"></th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="8" class="p-6 text-center text-gray-500">Loading…</td></tr>
          <tr v-else-if="!rows.length"><td colspan="8" class="p-6 text-center text-gray-500">Nothing to review.</td></tr>
          <tr v-for="p in rows" v-else :key="p.id" class="border-t align-top">
            <td class="p-3">
              <div class="flex gap-3">
                <img v-if="p.images?.[0]" :src="p.images[0]" class="h-14 w-14 object-cover rounded border" :alt="p.name">
                <div>
                  <div class="font-semibold">{{ p.name }}</div>
                  <div class="text-xs text-gray-500 max-w-xs">{{ p.description }}</div>
                  <div class="text-xs text-gray-500">{{ p.categories?.map((c: any) => c.name).join(', ') }} · SKU {{ p.SKU || '—' }}</div>
                </div>
              </div>
            </td>
            <td class="p-3">{{ p.sellerName }}<div class="text-xs" :class="p.sellerVerified ? 'text-green-700' : 'text-amber-700'">{{ p.sellerVerified ? 'Verified seller' : 'Seller not verified' }}</div></td>
            <td class="p-3 whitespace-nowrap">{{ money(p.price) }}</td>
            <td class="p-3">{{ p.stockQuantity }}</td>
            <td class="p-3 whitespace-nowrap">{{ p.weightKg ?? '?' }} kg</td>
            <td class="p-3">
              <span class="px-2 py-0.5 rounded text-xs" :class="statusClass(p.publicationStatus)">{{ label(p.publicationStatus) }}</span>
              <div v-if="!p.active" class="text-xs text-gray-500">Deactivated by seller</div>
              <div v-if="p.rejectionReason" class="text-xs text-red-700">{{ p.rejectionReason }}</div>
            </td>
            <td class="p-3 text-xs text-gray-500 whitespace-nowrap">{{ dt(p.createdAt) }}</td>
            <td class="p-3 whitespace-nowrap">
              <UButton v-if="p.publicationStatus !== 'PUBLISHED'" size="xs" color="success" icon="i-lucide-check" :loading="busyId === p.id" @click="approve(p)">Approve</UButton>
              <UButton v-if="p.publicationStatus !== 'REJECTED'" size="xs" color="error" variant="outline" icon="i-lucide-x" class="ml-1" :loading="busyId === p.id" @click="reject(p)">{{ p.publicationStatus === 'PUBLISHED' ? 'Unpublish' : 'Reject' }}</UButton>
            </td>
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
  </div>
</template>
