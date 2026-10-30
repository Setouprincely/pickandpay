<script setup lang="ts">
// Seller onboarding review: administrators approve or reject seller applications.
// Only verified sellers' (approved) products are visible to customers.
const { get, put } = useApi()
const { dt, errorMessage } = useOps()

const sellers = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const msg = ref('')
const busyId = ref<string | null>(null)
const tab = ref<'pending' | 'verified' | 'all'>('pending')

async function load() {
  loading.value = true
  error.value = ''
  try {
    sellers.value = (await get<any[]>('/api/sellers/all')) ?? []
  } catch (e: any) {
    error.value = errorMessage(e, 'Failed to load sellers')
  } finally {
    loading.value = false
  }
}

async function setVerified(s: any, verified: boolean) {
  let note = ''
  if (!verified) {
    const r = window.prompt(`Reason for ${s.isVerified ? 'suspending' : 'rejecting'} "${s.storeName}" (sent to the seller):`)
    if (!r || !r.trim()) return
    note = r.trim()
  }
  busyId.value = s.id
  msg.value = ''
  error.value = ''
  try {
    const params = new URLSearchParams({ isVerified: String(verified) })
    if (note) params.set('note', note)
    const updated = await put<any>(`/api/sellers/verify/${s.id}?${params}`, {})
    Object.assign(s, updated)
    msg.value = verified ? `${s.storeName} approved.` : `${s.storeName} ${note ? 'rejected' : 'updated'}.`
  } catch (e: any) {
    error.value = errorMessage(e, 'Update failed')
  } finally {
    busyId.value = null
  }
}

const pending = computed(() => sellers.value.filter(s => !s.isVerified))
const verified = computed(() => sellers.value.filter(s => s.isVerified))
const shown = computed(() => tab.value === 'pending' ? pending.value : tab.value === 'verified' ? verified.value : sellers.value)

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-4">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Seller approvals</h1>
      <p class="text-sm text-gray-500">{{ verified.length }} verified · {{ pending.length }} awaiting review or rejected</p>
    </div>
    <div class="flex gap-2">
      <button v-for="t in (['pending','verified','all'] as const)" :key="t" class="px-3 py-1.5 rounded-full text-sm border"
              :class="tab === t ? 'bg-red-600 text-white border-red-600' : 'bg-white'" @click="tab = t">
        {{ t === 'pending' ? `Pending (${pending.length})` : t === 'verified' ? `Verified (${verified.length})` : `All (${sellers.length})` }}
      </button>
      <UButton variant="ghost" icon="i-lucide-refresh-cw" @click="load">Refresh</UButton>
    </div>
    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
    <div v-if="msg" class="rounded-lg bg-green-50 text-green-700 p-3 text-sm">{{ msg }}</div>

    <div class="bg-white rounded-xl border overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-left text-gray-600">
          <tr><th class="p-3">Store</th><th class="p-3">Owner</th><th class="p-3">Pickup address</th><th class="p-3">Status</th><th class="p-3">Registered</th><th class="p-3"></th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="p-6 text-center text-gray-500">Loading…</td></tr>
          <tr v-else-if="!shown.length"><td colspan="6" class="p-6 text-center text-gray-500">No sellers in this list.</td></tr>
          <tr v-for="s in shown" v-else :key="s.id" class="border-t align-top">
            <td class="p-3 font-semibold">{{ s.storeName }}<div class="text-xs text-gray-500">{{ s.contactNumber }}</div></td>
            <td class="p-3">{{ s.ownerName }}<div class="text-xs text-gray-500">{{ s.ownerEmail }}</div></td>
            <td class="p-3 text-xs">{{ s.address }}<br>{{ s.city }}</td>
            <td class="p-3">
              <span class="px-2 py-0.5 rounded text-xs" :class="s.isVerified ? 'bg-green-100 text-green-800' : s.verificationNote ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'">
                {{ s.isVerified ? 'Verified' : s.verificationNote ? 'Rejected' : 'Pending review' }}
              </span>
              <div v-if="s.isVerified" class="text-xs text-gray-500">{{ dt(s.verifiedAt) }}</div>
              <div v-if="s.verificationNote" class="text-xs text-red-700">{{ s.verificationNote }}</div>
            </td>
            <td class="p-3 text-xs text-gray-500 whitespace-nowrap">{{ dt(s.createdAt) }}</td>
            <td class="p-3 whitespace-nowrap">
              <UButton v-if="!s.isVerified" size="xs" color="success" icon="i-lucide-check" :loading="busyId === s.id" @click="setVerified(s, true)">Approve</UButton>
              <UButton size="xs" color="error" variant="outline" icon="i-lucide-x" class="ml-1" :loading="busyId === s.id" @click="setVerified(s, false)">{{ s.isVerified ? 'Suspend' : 'Reject' }}</UButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
