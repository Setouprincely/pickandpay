<script setup lang="ts">
defineEmits<{ 'row-click': [any] }>()

const { $t } = useNuxtApp()
const { get, post, patch, del } = useApi()

// VehicleResponseDTO (backend): id, name, vehicleType, category (BICYCLE | K_TRUCK | VAN), categoryLabel,
// plateNumber, maxLoadKg, status (ACTIVE | IN_MAINTENANCE | INACTIVE | RETIRED),
// assignedDriverProfileId, assignedDriverName, assignedDriverPhone, last/nextMaintenanceDate, notes
type Vehicle = {
  id: string
  name: string
  vehicleType: string
  category?: string
  categoryLabel?: string
  plateNumber: string
  maxLoadKg?: number
  status: string
  assignedDriverProfileId?: string
  assignedDriverName?: string
  nextMaintenanceDate?: string
}
// DriverProfileDTO (backend /api/drivers)
type Driver = { driverId: string; profileId?: string; fullName: string; vehicleName?: string }

const vehicles = ref<Vehicle[]>([])
const drivers = ref<Driver[]>([])
const loading = ref(true)
const error = ref('')
const showAddModal = ref(false)
const saving = ref(false)

const emptyVehicle = () => ({ name: '', category: 'BICYCLE', vehicleType: 'Bicycle', plateNumber: '', maxLoadKg: 5 as number | null, nextMaintenanceDate: '', notes: '' })
const newVehicle = reactive(emptyVehicle())
const CATEGORY_DEFAULTS: Record<string, { type: string; load: number }> = {
  BICYCLE: { type: 'Bicycle', load: 5 },
  K_TRUCK: { type: 'Suzuki Carry', load: 350 },
  VAN: { type: 'Toyota HiAce', load: 1000 },
}
watch(() => newVehicle.category, c => {
  newVehicle.vehicleType = CATEGORY_DEFAULTS[c]?.type ?? ''
  newVehicle.maxLoadKg = CATEGORY_DEFAULTS[c]?.load ?? null
})

const errText = (e: any, fallback: string) => e?.data?.message ?? `${fallback} (${e?.status ?? 'network error'})`

async function refreshVehicles() {
  const res = await get<Vehicle[]>('/api/fleet')
  vehicles.value = Array.isArray(res) ? res : []
}

onMounted(async () => {
  try {
    const [, dRes] = await Promise.all([refreshVehicles(), get<Driver[]>('/api/drivers')])
    drivers.value = Array.isArray(dRes) ? dRes.filter(d => d.profileId) : []
  } catch (e: any) {
    error.value = errText(e, 'Failed to load fleet')
  } finally {
    loading.value = false
  }
})

async function addVehicle() {
  if (!newVehicle.name.trim() || !newVehicle.plateNumber.trim()) {
    alert('Name and plate number are required.')
    return
  }
  saving.value = true
  try {
    const res = await post<Vehicle>('/api/fleet', {
      ...newVehicle,
      nextMaintenanceDate: newVehicle.nextMaintenanceDate || null,
    })
    vehicles.value = [res, ...vehicles.value]
    showAddModal.value = false
    Object.assign(newVehicle, emptyVehicle())
  } catch (e: any) {
    alert(errText(e, 'Could not add vehicle'))
  } finally {
    saving.value = false
  }
}

async function updateStatus(id: string, status: string) {
  try {
    const updated = await patch<Vehicle>(`/api/fleet/${id}/status?status=${encodeURIComponent(status)}`)
    vehicles.value = vehicles.value.map(v => v.id === id ? updated : v)
  } catch (e: any) {
    alert(errText(e, 'Could not update status'))
    await refreshVehicles()
  }
}

async function assignDriver(vehicleId: string, profileId: string) {
  if (!profileId) return
  try {
    await patch(`/api/fleet/${vehicleId}/assign?driverProfileId=${encodeURIComponent(profileId)}`)
    await refreshVehicles()
  } catch (e: any) {
    alert(errText(e, 'Could not assign driver'))
    await refreshVehicles()
  }
}

async function unassignDriver(vehicleId: string) {
  try {
    await patch(`/api/fleet/${vehicleId}/unassign`)
    await refreshVehicles()
  } catch (e: any) {
    alert(errText(e, 'Could not unassign driver'))
  }
}

async function deleteVehicle(v: Vehicle) {
  if (!confirm(`Delete vehicle ${v.name} (${v.plateNumber})?`)) return
  try {
    await del(`/api/fleet/${v.id}`)
    vehicles.value = vehicles.value.filter(x => x.id !== v.id)
  } catch (e: any) {
    alert(errText(e, 'Could not delete vehicle'))
  }
}

const freeDrivers = computed(() => {
  const busy = new Set(vehicles.value.map(v => v.assignedDriverProfileId).filter(Boolean))
  return drivers.value.filter(d => !busy.has(d.profileId))
})

const page = ref(1)
const perPage = 8
const totalPages = computed(() => Math.max(1, Math.ceil(vehicles.value.length / perPage)))
const paged = computed(() => vehicles.value.slice((page.value - 1) * perPage, page.value * perPage))
</script>

<template>
  <div class="ft-card">
    <div class="ft-header">
      <h2 class="ft-title">{{ $t('reports.fleet.title') }}</h2>
      <button class="ft-add-btn" @click="showAddModal = true">+ Add Vehicle</button>
    </div>

    <div v-if="loading" class="ft-state">Loading fleet…</div>
    <div v-else-if="error" class="ft-state ft-state--error">{{ error }}</div>
    <div v-else-if="vehicles.length === 0" class="ft-state">No vehicles in the fleet yet. Add the bicycles, Suzuki Carry / K-Trucks and Toyota HiAce vans used for dispatch.</div>

    <template v-else>
      <div class="ft-scroll">
        <table class="ft-table">
          <thead>
            <tr>
              <th>Vehicle</th><th>Plate</th><th>Category</th><th>Max load</th><th>Driver</th><th>Status</th><th>Next maintenance</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in paged" :key="row.id" class="ft-row">
              <td>{{ row.name }}<div style="font-size:0.65rem;color:#6B7280">{{ row.vehicleType }}</div></td>
              <td>{{ row.plateNumber }}</td>
              <td>{{ row.categoryLabel ?? '⚠ not set' }}</td>
              <td>{{ row.maxLoadKg != null ? row.maxLoadKg + ' kg' : '—' }}</td>
              <td>
                <span v-if="row.assignedDriverName">{{ row.assignedDriverName }}</span>
                <select v-else class="ft-select" @change="e => assignDriver(row.id, (e.target as HTMLSelectElement).value)">
                  <option value="">Assign driver…</option>
                  <option v-for="d in freeDrivers" :key="d.profileId" :value="d.profileId">{{ d.fullName }}</option>
                </select>
              </td>
              <td>
                <select class="ft-select" :value="row.status" @change="e => updateStatus(row.id, (e.target as HTMLSelectElement).value)">
                  <option value="ACTIVE">Active</option>
                  <option value="IN_MAINTENANCE">In maintenance</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="RETIRED">Retired</option>
                </select>
              </td>
              <td>{{ row.nextMaintenanceDate ?? '—' }}</td>
              <td class="ft-actions">
                <button v-if="row.assignedDriverName" class="ft-btn ft-btn--sm" @click="unassignDriver(row.id)">Unassign</button>
                <button class="ft-btn ft-btn--danger ft-btn--sm" @click="deleteVehicle(row)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="ft-pagination">
        <button class="pg-arrow" @click="page = Math.max(1, page - 1)">←</button>
        <button v-for="p in totalPages" :key="p" class="pg-btn" :class="{ 'pg-btn--active': page === p }" @click="page = p">
          {{ String(p).padStart(2,'0') }}
        </button>
        <button class="pg-arrow" @click="page = Math.min(totalPages, page + 1)">→</button>
      </div>
    </template>

    <!-- Add Vehicle Modal -->
    <Teleport v-if="showAddModal" to="body">
      <div class="modal-overlay" @click.self="showAddModal = false">
        <div class="modal-card">
          <h3 class="modal-title">Add Vehicle</h3>
          <div class="modal-body">
            <div class="form-group">
              <label>Name *</label>
              <input v-model="newVehicle.name" type="text" class="form-input" placeholder="Carry 01" />
            </div>
            <div class="form-group">
              <label>Plate Number *</label>
              <input v-model="newVehicle.plateNumber" type="text" class="form-input" placeholder="LT-1234-A" />
            </div>
            <div class="form-group">
              <label>Fleet category * (dispatch decision matrix)</label>
              <select v-model="newVehicle.category" class="form-input">
                <option value="BICYCLE">Bicycle (light parcels)</option>
                <option value="K_TRUCK">Suzuki Carry / K-Truck (medium loads)</option>
                <option value="VAN">Toyota HiAce (large / heavy loads)</option>
              </select>
            </div>
            <div class="form-group">
              <label>Model</label>
              <input v-model="newVehicle.vehicleType" type="text" class="form-input" />
            </div>
            <div class="form-group">
              <label>Max load (kg)</label>
              <input v-model.number="newVehicle.maxLoadKg" type="number" min="1" class="form-input" />
            </div>
            <div class="form-group">
              <label>Next maintenance</label>
              <input v-model="newVehicle.nextMaintenanceDate" type="date" class="form-input" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="showAddModal = false">Cancel</button>
            <button class="btn-primary" :disabled="saving" @click="addVehicle">{{ saving ? 'Saving…' : 'Add Vehicle' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
<style scoped>
.ft-card   { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; min-width: 0; }
.ft-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.ft-title  { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--color-text-primary); margin: 0; }
.ft-add-btn { background: var(--color-primary); color: #fff; border: none; border-radius: 0.375rem; padding: 0.5rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer; }
.ft-add-btn:hover { background: var(--color-primary-hover); }
.ft-state  { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.ft-state--error { color: #DC2626; }
.ft-scroll { overflow-x: auto; }
.ft-table  { width: 100%; border-collapse: collapse; min-width: 700px; }
.ft-table th { font-size: 0.625rem; font-weight: 700; color: var(--color-text-muted); text-align: left; padding: 0.5rem 0.5rem; border-bottom: 1px solid var(--color-border); }
.ft-table td { font-size: 0.75rem; color: var(--color-text-primary); padding: 0.625rem 0.5rem; border-bottom: 1px solid var(--color-border); vertical-align: middle; }
.ft-row:hover { background: #F9FAFB; }
.ft-select { border: 1px solid var(--color-border); border-radius: 0.25rem; padding: 0.25rem 0.375rem; font-size: 0.75rem; background: #fff; cursor: pointer; }
.ft-actions { display: flex; gap: 0.375rem; }
.ft-btn { border: none; border-radius: 0.25rem; cursor: pointer; font-size: 0.6875rem; font-weight: 600; padding: 0.25rem 0.5rem; }
.ft-btn--sm { padding: 0.2rem 0.5rem; }
.ft-btn--danger { background: #FEE2E2; color: #DC2626; }
.ft-btn--danger:hover { background: #FCA5A5; }
.ft-pagination { display: flex; align-items: center; justify-content: center; gap: 0.375rem; margin-top: 1rem; }
.pg-btn { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: none; font-size: 0.6875rem; background: transparent; color: var(--color-page-idle-text); cursor: pointer; }
.pg-btn--active { background: var(--color-page-active-bg); color: #fff; font-weight: 700; }
.pg-arrow { width: 1.5rem; height: 1.5rem; border-radius: 50%; border: 1px solid var(--color-border); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: 0.75rem; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.modal-title { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0; padding: 1.25rem 1.25rem 0; }
.modal-body { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.375rem; }
.form-group label { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }
.form-input { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.5rem 0.75rem; font-size: 0.875rem; outline: none; }
.form-input:focus { border-color: var(--color-primary); }
.modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }
.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem; padding: 0.625rem 1.25rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
.btn-primary:hover { background: var(--color-primary-hover); }
.btn-secondary { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.625rem 1.25rem; font-size: 0.875rem; cursor: pointer; }
</style>
