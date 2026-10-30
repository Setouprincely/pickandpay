<script setup lang="ts">
import AvailabilityToggle from '~/apps/driver/components/AvailabilityToggle.vue'

// Driver profile from the backend. Personal details are editable (PUT /api/users/me);
// the vehicle is assigned by DOCH fleet management and is read-only here.
const { $t } = useNuxtApp()
const { get, put } = useDriverApi()

const profile = ref<any | null>(null)
const form = reactive({ firstName: '', lastName: '', phone: '' })
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const saved = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [me, p] = await Promise.all([get<any>('/api/users/me'), get<any>('/api/drivers/me/profile')])
    form.firstName = me.firstName ?? ''
    form.lastName = me.lastName ?? ''
    form.phone = me.phone ?? ''
    profile.value = { ...p, email: me.email }
  } catch (e: any) {
    error.value = e?.data?.message ?? `Could not load your profile (${e?.status ?? 'network error'})`
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.firstName.trim() || !form.lastName.trim()) {
    error.value = 'First and last name are required.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await put('/api/users/me', { firstName: form.firstName.trim(), lastName: form.lastName.trim(), phone: form.phone.trim() })
    saved.value = 'Profile saved.'
  } catch (e: any) {
    error.value = e?.data?.message ?? `Could not save (${e?.status ?? 'network error'})`
  } finally {
    saving.value = false
  }
}

const categoryLabel = (c?: string) => ({ BICYCLE: 'Bicycle', K_TRUCK: 'Suzuki Carry / K-Truck', VAN: 'Toyota HiAce' } as Record<string, string>)[c ?? ''] ?? '—'

onMounted(load)
</script>

<template>
  <div class="p-4 space-y-4 max-w-2xl">
    <h1 class="text-xl font-bold">{{ $t('driver.settings.title') ?? 'Settings' }}</h1>
    <div v-if="loading" class="text-gray-500">Loading…</div>
    <template v-else>
      <div v-if="error" class="rounded bg-red-50 text-red-700 p-3 text-sm">{{ error }}</div>
      <div v-if="saved" class="rounded bg-green-50 text-green-700 p-3 text-sm">{{ saved }}</div>

      <section class="rounded-xl border bg-white p-4 space-y-2">
        <h2 class="font-semibold">Availability</h2>
        <AvailabilityToggle />
        <p class="text-xs text-gray-500">Dispatch can only assign jobs to drivers who are online.</p>
      </section>

      <section class="rounded-xl border bg-white p-4 space-y-3">
        <h2 class="font-semibold">My account</h2>
        <div class="grid sm:grid-cols-2 gap-3">
          <label class="text-sm">First name<input v-model="form.firstName" class="w-full border rounded px-3 py-2 mt-1" /></label>
          <label class="text-sm">Last name<input v-model="form.lastName" class="w-full border rounded px-3 py-2 mt-1" /></label>
          <label class="text-sm">Phone<input v-model="form.phone" type="tel" class="w-full border rounded px-3 py-2 mt-1" /></label>
          <label class="text-sm">Email<input :value="profile?.email" class="w-full border rounded px-3 py-2 mt-1 bg-gray-50" disabled /></label>
        </div>
        <button class="bg-red-600 text-white rounded px-4 py-2 text-sm font-semibold disabled:opacity-50" :disabled="saving" @click="onSave">
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </section>

      <section class="rounded-xl border bg-white p-4 space-y-1 text-sm">
        <h2 class="font-semibold mb-2">Assigned vehicle</h2>
        <template v-if="profile?.vehicleId">
          <div>{{ profile.vehicleName }} — {{ categoryLabel(profile.vehicleCategory) }}</div>
          <div class="text-gray-600">Plate {{ profile.plateNumber }} · {{ profile.vehicleType }}</div>
        </template>
        <p v-else class="text-gray-600">No fleet vehicle assigned yet. Dispatch assigns one with your first job.</p>
      </section>

      <section class="rounded-xl border bg-white p-4 text-sm grid grid-cols-3 gap-3 text-center">
        <div><div class="text-2xl font-bold">{{ profile?.totalDeliveries ?? 0 }}</div><div class="text-gray-500">Deliveries</div></div>
        <div><div class="text-2xl font-bold">{{ profile?.activeDeliveries ?? 0 }}</div><div class="text-gray-500">Active jobs</div></div>
        <div><div class="text-2xl font-bold">{{ profile?.rating ?? '—' }}</div><div class="text-gray-500">Customer rating</div></div>
      </section>
    </template>
  </div>
</template>
