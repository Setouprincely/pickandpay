<script setup lang="ts">
// Platform settings persisted in the backend (pricing_settings table).
// Read: MANAGE_SYSTEM / MANAGE_XENDER. Write: MANAGE_SYSTEM (super admin).
const { get, put } = useApi()
const { errorMessage } = useOps()

const form = reactive({
  minPrice: 0,
  peakSurchargePercent: 0,
  peakSurchargeEnabled: false,
  bicycleMaxWeightKg: 5,
  bicycleMaxDimensionCm: 60,
  kTruckMaxWeightKg: 350,
  productModerationRequired: true,
})
let original = { ...form }

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const msg = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await get<any>('/api/settings/pricing')
    Object.keys(form).forEach(k => { if (res?.[k] !== undefined && res[k] !== null) (form as any)[k] = res[k] })
    original = { ...form }
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not load settings')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  msg.value = ''
  try {
    const res = await put<any>('/api/settings/pricing', { ...form })
    Object.keys(form).forEach(k => { if (res?.[k] !== undefined) (form as any)[k] = res[k] })
    original = { ...form }
    msg.value = 'Settings saved. They apply to the next quote, order and dispatch decision.'
  } catch (e: any) {
    error.value = errorMessage(e, 'Could not save settings')
  } finally {
    saving.value = false
  }
}

function reset() {
  Object.assign(form, original)
  msg.value = ''
}

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-5 max-w-4xl">
    <h1 class="text-2xl font-bold text-gray-900">Settings</h1>
    <div v-if="loading" class="text-gray-500">Loading settings…</div>
    <template v-else>
      <div v-if="error" class="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
      <div v-if="msg" class="rounded-lg bg-green-50 text-green-700 p-3 text-sm">{{ msg }}</div>

      <section class="bg-white rounded-xl border p-5 space-y-3">
        <h2 class="font-semibold">Delivery pricing</h2>
        <p class="text-xs text-gray-500">Delivery fees follow the distance tiers (single parcel: ≤5 km 500, ≤10 km 700, ≤15 km 1 000, ≤20 km 1 200, &gt;20 km 1 500 XAF; full truck: 7 000 / 8 500 / 10 000 XAF), then the surcharge and the minimum below are applied.</p>
        <label class="block text-sm">Minimum delivery price (XAF)
          <input v-model.number="form.minPrice" type="number" min="0" step="50" class="border rounded px-3 py-2 w-full mt-1" />
        </label>
        <label class="flex items-center gap-2 text-sm">
          <input v-model="form.peakSurchargeEnabled" type="checkbox" /> Apply peak-hour surcharge
        </label>
        <label class="block text-sm">Peak surcharge (%)
          <input v-model.number="form.peakSurchargePercent" type="number" min="0" max="300" class="border rounded px-3 py-2 w-full mt-1" :disabled="!form.peakSurchargeEnabled" />
        </label>
      </section>

      <section class="bg-white rounded-xl border p-5 space-y-3">
        <h2 class="font-semibold">Vehicle decision matrix</h2>
        <p class="text-xs text-gray-500">Each package gets a required vehicle category from its total weight and largest dimension. Dispatchers can only assign a vehicle of that category or larger. Packages with unknown weight never go by bicycle.</p>
        <div class="grid sm:grid-cols-3 gap-3">
          <label class="block text-sm">Bicycle: max weight (kg)
            <input v-model.number="form.bicycleMaxWeightKg" type="number" min="0.1" step="0.5" class="border rounded px-3 py-2 w-full mt-1" />
          </label>
          <label class="block text-sm">Bicycle: max dimension (cm)
            <input v-model.number="form.bicycleMaxDimensionCm" type="number" min="1" class="border rounded px-3 py-2 w-full mt-1" />
          </label>
          <label class="block text-sm">Suzuki Carry / K-Truck: max weight (kg)
            <input v-model.number="form.kTruckMaxWeightKg" type="number" min="1" class="border rounded px-3 py-2 w-full mt-1" />
          </label>
        </div>
        <p class="text-xs text-gray-500">Heavier than the K-Truck limit → Toyota HiAce. Full-truck bookings always need at least a K-Truck.</p>
      </section>

      <section class="bg-white rounded-xl border p-5 space-y-2">
        <h2 class="font-semibold">Marketplace publication rules</h2>
        <label class="flex items-center gap-2 text-sm">
          <input v-model="form.productModerationRequired" type="checkbox" /> New seller listings require administrator approval before they are published
        </label>
      </section>

      <div class="flex gap-2">
        <UButton color="error" icon="i-lucide-save" :loading="saving" @click="save">Save changes</UButton>
        <UButton variant="outline" @click="reset">Discard</UButton>
      </div>
    </template>
  </div>
</template>
