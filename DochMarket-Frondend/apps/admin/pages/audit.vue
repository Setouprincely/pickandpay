<script setup lang="ts">
// Read-only audit trail of business and security operations.
const { get } = useApi()
const { dt, errorMessage } = useOps()
const route = useRoute()

const entityType = ref(String(route.query.entityType ?? ''))
const entityId = ref(String(route.query.entityId ?? ''))
const page = ref(0)
const rows = ref<any[]>([])
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: String(page.value), size: '50' })
    if (entityType.value) params.set('entityType', entityType.value)
    if (entityId.value.trim()) params.set('entityId', entityId.value.trim())
    const res = await get<any>(`/api/admin/audit?${params}`)
    rows.value = res.content ?? []
    totalPages.value = res.totalPages ?? res.page?.totalPages ?? 0
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not load the audit trail')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="p-6 space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <h1 class="text-2xl font-bold text-gray-900">Audit trail</h1>
      <form class="flex gap-2" @submit.prevent="page = 0; load()">
        <select v-model="entityType" class="border rounded-lg px-3 py-2 text-sm">
          <option value="">All entities</option>
          <option v-for="t in ['ORDER','PAYMENT','DELIVERY','PRODUCT','SELLER','VEHICLE','SETTINGS']" :key="t" :value="t">{{ t }}</option>
        </select>
        <input v-model="entityId" class="border rounded-lg px-3 py-2 text-sm w-80" placeholder="Entity ID (UUID)" />
        <UButton type="submit" color="error" icon="i-lucide-search" :loading="loading">Filter</UButton>
      </form>
    </div>
    <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
    <div class="bg-white rounded-xl border overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-left text-gray-600">
          <tr><th class="p-3">When</th><th class="p-3">Actor</th><th class="p-3">Action</th><th class="p-3">Entity</th><th class="p-3">Details</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="p-6 text-center text-gray-500">Loading…</td></tr>
          <tr v-else-if="!rows.length"><td colspan="5" class="p-6 text-center text-gray-500">No audit entries.</td></tr>
          <tr v-for="a in rows" v-else :key="a.id" class="border-t align-top">
            <td class="p-3 whitespace-nowrap text-gray-500">{{ dt(a.occurredAt) }}</td>
            <td class="p-3">{{ a.actorEmail }}<div class="text-xs text-gray-500">{{ a.actorRole }}</div></td>
            <td class="p-3 font-semibold">{{ a.action }}</td>
            <td class="p-3 text-xs">{{ a.entityType }}<div class="font-mono">{{ a.entityId }}</div></td>
            <td class="p-3 text-xs max-w-xl">{{ a.details }}</td>
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
