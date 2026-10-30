<script setup lang="ts">
import FleetTable from '~/components/reports/FleetTable.vue'
import DriverRatings from '~/components/reports/DriverRatings.vue'
import DriverDetailsModal from '~/components/reports/DriverDetailsModal.vue'

export type DriverDTO = {
  driverId: string
  fullName: string
  vehicleType: string | null
  plateNumber: string | null
  rating: number | null
  totalDeliveries: number | null
  available?: boolean
  isAvailable?: boolean
}

const { get } = useApi()

const drivers = ref<DriverDTO[]>([])
const loading = ref(true)
const error = ref('')

// The driver that was clicked — passed to the modal (was previously a generic order)
const selectedDriver = ref<DriverDTO | null>(null)
const modalOpen = ref(false)

onMounted(async () => {
  try {
    // GET /api/drivers — List<DriverProfileDTO>
    drivers.value = await get<DriverDTO[]>('/api/drivers')
  } catch (e: any) {
    error.value = `Failed to load reports (${e?.status ?? 'network error'})`
    console.error('Reports page error:', e)
  } finally {
    loading.value = false
  }
})

// Open the modal for the SPECIFIC driver clicked.
function openDriverModal(driver: DriverDTO) {
  selectedDriver.value = driver
  modalOpen.value = true
}
</script>

<template>
  <div class="reports-content">

    <div v-if="loading" class="rep-state">
      <span class="rep-spinner" /> Loading reports…
    </div>

    <div v-else-if="error" class="rep-state rep-state--error">
      ⚠️ {{ error }}
    </div>

    <template v-else>
      <!-- Fleet: localStorage CRUD, real drivers available for assignment -->
      <FleetTable :drivers="drivers" />

      <!-- Driver ratings: real data; clicking a row opens THAT driver's details -->
      <DriverRatings
        :drivers="drivers"
        @row-click="openDriverModal"
      />

      <DriverDetailsModal
        v-if="modalOpen"
        :driver="selectedDriver"
        @close="modalOpen = false"
      />
    </template>

  </div>
</template>

<style scoped>
.reports-content {
  display: flex; flex-direction: column;
  gap: 1.25rem; padding: 1rem;
  overflow-x: hidden; min-width: 0;
}
.rep-state {
  display: flex; align-items: center; justify-content: center; gap: 0.75rem;
  padding: 3rem 1rem; background: var(--color-surface);
  border: 1px solid var(--color-border); border-radius: 0.75rem;
  color: var(--color-text-secondary); font-size: 0.875rem;
}
.rep-state--error { border-color: #FCA5A5; background: #FFF1F2; color: #DC2626; }
.rep-spinner {
  width: 1.25rem; height: 1.25rem;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: rep-spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes rep-spin { to { transform: rotate(360deg); } }
@media (min-width: 1024px) { .reports-content { padding: 1.5rem; } }
</style>
