<script setup lang="ts">
const emit = defineEmits<{ close: []; saved: [] }>()

const { $t } = useNuxtApp()
const { post } = useApi()

const form = reactive({
  lastName:  '',
  firstName: '',
  userName:  '',
  role:      '',
  email:     '',
  phone:     '',
  password:  '',
  // Driver-only fields (required by the backend for driver onboarding)
  vehicleType: '',
  plateNumber: '',
})

const saving = ref(false)
const error = ref('')
const okMsg = ref('')

// UI role label -> backend ROLE_* string
const roleMap: Record<string, string> = {
  Client: 'ROLE_BUYER',
  Driver: 'ROLE_DELIVERY',
  Vendor: 'ROLE_SELLER',
  MarketAdmin: 'ROLE_MARKET_ADMIN',
  XenderAdmin: 'ROLE_XENDER_ADMIN',
}

// Client/Vendor self-verify by email after creation, so a password is
// collected up front. Driver/Admin roles are invite-only: the backend
// generates the account disabled and emails an OTP + setup link, so no
// password field is needed for those.
const isInviteRole = computed(() => form.role === 'Driver' || form.role === 'MarketAdmin' || form.role === 'XenderAdmin')
const isDriver = computed(() => form.role === 'Driver')

function reset() {
  Object.keys(form).forEach(k => ((form as any)[k] = ''))
  error.value = ''
  okMsg.value = ''
}

async function save() {
  error.value = ''
  okMsg.value = ''

  if (!form.firstName || !form.lastName || !form.email || !form.role) {
    error.value = 'Please fill first name, last name, email and role.'
    return
  }
  if (!isInviteRole.value && !form.password) {
    error.value = 'Please set a password for this account.'
    return
  }
  if (isDriver.value && (!form.vehicleType || !form.plateNumber)) {
    error.value = 'Vehicle category and plate number are required for drivers.'
    return
  }

  saving.value = true
  try {
    if (isDriver.value) {
      // POST /api/admin/onboard-driver (DriverOnboardingRequest) — driver
      // self-registration is blocked server-side, so this is the only path.
      await post('/api/admin/onboard-driver', {
        email: form.email.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim(),
        vehicleType: form.vehicleType.trim(),
        plateNumber: form.plateNumber.trim(),
      })
      okMsg.value = 'Driver invited. They will receive a setup email with a code to activate their account.'
    } else if (form.role === 'MarketAdmin' || form.role === 'XenderAdmin') {
      // POST /api/admin/onboard-staff?roleName=... (StaffOnboardingRequest)
      await post(`/api/admin/onboard-staff?roleName=${roleMap[form.role]}`, {
        email: form.email.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim(),
      })
      okMsg.value = 'Admin invited. They will receive a setup email with a code to activate their account.'
    } else {
      // Client / Vendor: normal self-verifying registration.
      await post('/api/auth/register', {
        email: form.email.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        userName: (form.userName || form.email.split('@')[0]).trim(),
        password: form.password,
        phone: form.phone.trim(),
        role: roleMap[form.role] ?? 'ROLE_BUYER',
      })
      okMsg.value = 'User created. They must verify their email before logging in.'
    }

    emit('saved')
    setTimeout(() => emit('close'), 1200)
  } catch (e: any) {
    error.value = e?.data?.message || 'Failed to create user.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">

        <p class="modal-label">{{ $t('users.userUpper') }}</p>

        <div class="modal-body">
          <div class="form-grid">

            <div class="form-group">
              <label class="form-label">{{ $t('users.firstName') }}</label>
              <input v-model="form.firstName" type="text" class="form-input" :placeholder="$t('users.phFirstName')" />
            </div>

            <div class="form-group">
              <label class="form-label">{{ $t('users.lastName') }}</label>
              <input v-model="form.lastName" type="text" class="form-input" :placeholder="$t('users.phLastName')" />
            </div>

            <div class="form-group">
              <label class="form-label">{{ $t('users.email') }}</label>
              <input v-model="form.email" type="email" class="form-input" :placeholder="$t('users.phEmail')" />
            </div>

            <div class="form-group">
              <label class="form-label">{{ $t('users.phone') }}</label>
              <input v-model="form.phone" type="tel" class="form-input" :placeholder="$t('users.phPhone')" />
            </div>

            <div class="form-group">
              <label class="form-label">Username</label>
              <input v-model="form.userName" type="text" class="form-input" placeholder="auto from email" />
            </div>

            <div class="form-group" v-if="!isInviteRole">
              <label class="form-label">Password</label>
              <input v-model="form.password" type="password" class="form-input" placeholder="••••••••" />
            </div>

            <div class="form-group form-group--full" v-if="isInviteRole">
              <p class="msg-info">
                {{ isDriver ? 'Drivers' : 'Admins' }} can't set their own password here — they'll get an
                email with a setup code to activate the account and choose their own password.
              </p>
            </div>

            <template v-if="isDriver">
              <div class="form-group">
                <label class="form-label">Vehicle category</label>
                <select v-model="form.vehicleType" class="form-input">
                  <option value="">— Select —</option>
                  <option value="BICYCLE">Bicycle / motorbike (small parcels)</option>
                  <option value="K_TRUCK">Suzuki Carry / K-Truck</option>
                  <option value="VAN">Toyota HiAce / van</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Plate number</label>
                <input v-model="form.plateNumber" type="text" class="form-input" placeholder="e.g. CE-123-AB" />
                <p class="text-xs text-gray-500 mt-1">The vehicle is registered in Reports → Fleet and assigned to this driver.</p>
              </div>
            </template>

            <div class="form-group form-group--full">
              <label class="form-label">{{ $t('users.role') }}</label>
              <select v-model="form.role" class="form-select">
                <option value="" disabled>{{ $t('users.selectRole') }}</option>
                <option value="Client">{{ $t('role.client') }}</option>
                <option value="Driver">{{ $t('role.driver') }}</option>
                <option value="Vendor">{{ $t('role.vendor') }}</option>
                <option value="MarketAdmin">Market Admin</option>
                <option value="XenderAdmin">Xender Admin</option>
              </select>
            </div>

          </div>

          <p v-if="error" class="msg msg--error">{{ error }}</p>
          <p v-if="okMsg" class="msg msg--ok">{{ okMsg }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'Saving…' : $t('common.save') }}
          </button>
          <button class="btn-secondary" :disabled="saving" @click="reset">{{ $t('common.reset') }}</button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card    { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 600px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); max-height: 90vh; overflow-y: auto; }
.modal-label   { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--color-text-secondary); margin: 0; padding: 1.25rem 1.5rem 0; }
.modal-body    { padding: 1.25rem 1.5rem; }
.form-grid     { display: grid; grid-template-columns: 1fr 1fr; gap: 0.875rem; min-width: 0; }
.form-group    { display: flex; flex-direction: column; gap: 0.3rem; }
.form-group--full { grid-column: 1 / -1; }
.form-label  { font-size: 0.8125rem; font-weight: 500; color: var(--color-text-primary); }
.form-input  { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.5rem 0.75rem; font-size: 0.875rem; color: var(--color-text-primary); outline: none; background: #fff; width: 100%; box-sizing: border-box; }
.form-input:focus { border-color: var(--color-primary); }
.form-input::placeholder { color: var(--color-text-muted); }
.form-select { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.5rem 0.75rem; font-size: 0.875rem; color: var(--color-text-primary); outline: none; background: #fff; width: 100%; appearance: none; cursor: pointer; }
.form-select:focus { border-color: var(--color-primary); }
.msg { font-size: 0.8125rem; margin: 0.875rem 0 0; }
.msg-info { font-size: 0.8125rem; color: var(--color-text-secondary); background: var(--color-page-bg); border-radius: 0.375rem; padding: 0.625rem 0.75rem; margin: 0; }
.msg--error { color: #DC2626; }
.msg--ok    { color: #16A34A; }
.modal-footer { display: flex; gap: 0.75rem; padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); }
.btn-primary  { background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem; padding: 0.625rem 1.5rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-secondary { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.625rem 1.5rem; font-size: 0.875rem; cursor: pointer; }
.btn-secondary:hover { background: var(--color-page-bg); }
@media (max-width: 540px) {
  .form-grid        { grid-template-columns: 1fr; }
  .form-group--full { grid-column: 1; }
}
</style>
