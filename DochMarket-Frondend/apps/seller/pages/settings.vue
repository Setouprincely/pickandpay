<script setup lang="ts">
const { $t, $locale, $setLocale } = useNuxtApp()
const { notify } = useNotify()
const { get, put } = useApi()

// Shop profile is stored on the seller record: GET / PUT /api/users/me.
// The address is the pickup point used by DOCH drivers and for delivery pricing.
const firstName = ref('')
const lastName = ref('')
const phone = ref('')
const contactEmail = ref('')
const shopName = ref('')
const city = ref('')
const address = ref('')
const contactNumber = ref('')
const verified = ref<boolean | null>(null)
const verificationNote = ref('')
const loading = ref(true)
const saving = ref(false)

onMounted(async () => {
  try {
    const me = await get<any>('/api/users/me')
    firstName.value = me.firstName ?? ''
    lastName.value = me.lastName ?? ''
    phone.value = me.phone ?? ''
    contactEmail.value = me.email ?? ''
    const s = me.sellerProfile ?? {}
    shopName.value = s.storeName ?? ''
    city.value = s.city ?? ''
    address.value = s.address ?? ''
    contactNumber.value = s.contactNumber ?? ''
    verified.value = s.isVerified ?? null
    verificationNote.value = s.verificationNote ?? ''
  } catch (e: any) {
    notify({ title: e?.data?.message ?? 'Could not load your profile', variant: 'error' })
  } finally {
    loading.value = false
  }
})

async function save() {
  if (!shopName.value.trim() || !address.value.trim()) {
    notify({ title: 'Shop name and pickup address are required.', variant: 'error' })
    return
  }
  saving.value = true
  try {
    await put('/api/users/me', {
      firstName: firstName.value.trim(),
      lastName: lastName.value.trim(),
      phone: phone.value.trim(),
      storeName: shopName.value.trim(),
      storeCity: city.value.trim(),
      storeAddress: address.value.trim(),
      storeContactNumber: contactNumber.value.trim(),
    })
    notify({ title: $t('seller.settings.saved') })
  } catch (e: any) {
    notify({ title: e?.data?.message ?? `Could not save (${e?.status ?? 'network error'})`, variant: 'error' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="settings">
    <h1 class="settings__title">{{ $t('seller.settings.title') }}</h1>
    <div v-if="loading">Loading…</div>
    <template v-else>
    <section class="card">
      <h3 class="card__title">Account status</h3>
      <p v-if="verified">Your seller account is <strong>verified</strong>. Approved products are visible to customers.</p>
      <p v-else-if="verificationNote">Your application was not approved: <strong>{{ verificationNote }}</strong>. Update your details and contact DOCH support.</p>
      <p v-else>Your seller account is <strong>awaiting DOCH approval</strong>. You can already add products; they go live after approval.</p>
    </section>

    <!-- Shop profile -->
    <section class="card">
      <h3 class="card__title">{{ $t('seller.settings.shopProfile') }}</h3>
      <div class="field">
        <label>{{ $t('seller.settings.shopName') }} *</label>
        <UInput v-model="shopName" size="md" />
      </div>
      <div class="field-row">
        <div class="field">
          <label>{{ $t('seller.settings.location') }} (city)</label>
          <UInput v-model="city" size="md" />
        </div>
        <div class="field">
          <label>Shop phone</label>
          <UInput v-model="contactNumber" size="md" />
        </div>
      </div>
      <div class="field">
        <label>Pickup address * (where DOCH drivers collect your packages)</label>
        <UInput v-model="address" size="md" placeholder="Street, district, city" />
      </div>
    </section>

    <section class="card">
      <h3 class="card__title">Owner</h3>
      <div class="field-row">
        <div class="field"><label>First name</label><UInput v-model="firstName" size="md" /></div>
        <div class="field"><label>Last name</label><UInput v-model="lastName" size="md" /></div>
      </div>
      <div class="field-row">
        <div class="field"><label>{{ $t('seller.settings.contactEmail') }}</label><UInput v-model="contactEmail" type="email" size="md" disabled /></div>
        <div class="field"><label>Phone</label><UInput v-model="phone" size="md" /></div>
      </div>
    </section>

    <!-- Language -->
    <section class="card">
      <h3 class="card__title">{{ $t('seller.settings.language') }}</h3>
      <div class="lang-row">
        <UButton
          :variant="$locale === 'en' ? 'solid' : 'outline'"
          :color="$locale === 'en' ? 'error' : 'neutral'"
          @click="$setLocale('en')"
        >
          {{ $t('lang.en') }}
        </UButton>
        <UButton
          :variant="$locale === 'fr' ? 'solid' : 'outline'"
          :color="$locale === 'fr' ? 'error' : 'neutral'"
          @click="$setLocale('fr')"
        >
          {{ $t('lang.fr') }}
        </UButton>
      </div>
    </section>

    <div class="settings__actions">
      <UButton class="save-btn" icon="i-lucide-check" :loading="saving" @click="save">
        {{ $t('seller.settings.save') }}
      </UButton>
    </div>
    </template>
  </div>
</template>

<style scoped>
.settings { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; min-width: 0; max-width: 46rem; }
.settings__title { font-size: 1.25rem; font-weight: 800; color: var(--color-text-primary); margin: 0; }

.card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 0.75rem; padding: 1.25rem; }
.card__title { font-size: 0.85rem; font-weight: 700; color: var(--color-text-primary); margin: 0 0 1rem; }

.field { display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.9rem; }
.field:last-child { margin-bottom: 0; }
.field label { font-size: 0.75rem; font-weight: 500; color: var(--color-text-secondary); }
.field-row { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.field :deep(input) { color: var(--color-text-primary) !important; }

.toggle-row { display: flex; align-items: center; justify-content: space-between; padding: 0.6rem 0; border-bottom: 1px solid var(--color-border); font-size: 0.85rem; color: var(--color-text-primary); }
.toggle-row:last-child { border-bottom: none; }

.lang-row { display: flex; gap: 0.5rem; }

.settings__actions { display: flex; justify-content: flex-end; }
.save-btn { background: var(--color-button-red) !important; }

@media (min-width: 640px) {
  .field-row { grid-template-columns: 1fr 1fr; }
}
@media (min-width: 1024px) { .settings { padding: 1.5rem; gap: 1.25rem; } }
</style>
