<script setup lang="ts">
const emit = defineEmits<{ close: []; saved: [] }>()

const { $t } = useNuxtApp()
const { get, put } = useApi()

type ApiUser = {
  id: string
  firstName?: string
  lastName?: string
  email: string
}

const users = ref<ApiUser[]>([])
const query = ref('')
const selectedUserId = ref('')
const selectedRole = ref('')
const loadingUsers = ref(true)
const saving = ref(false)
const error = ref('')
const okMsg = ref('')

const roles = ['Client', 'Driver', 'Vendor']
const roleLabel = (r: string) => $t('role.' + r.toLowerCase())

// UI label -> backend role string used by PUT /api/admin/users/{id}/role
const roleMap: Record<string, string> = {
  Client: 'ROLE_BUYER',
  Driver: 'ROLE_DELIVERY',
  Vendor: 'ROLE_SELLER',
}

onMounted(async () => {
  try {
    const res = await get<ApiUser[]>('/api/admin/users')
    users.value = Array.isArray(res) ? res : []
  } catch {
    error.value = 'Failed to load users.'
  } finally {
    loadingUsers.value = false
  }
})

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return users.value.slice(0, 8)
  return users.value
    .filter(u =>
      `${u.firstName ?? ''} ${u.lastName ?? ''} ${u.email}`.toLowerCase().includes(q)
    )
    .slice(0, 8)
})

function pickUser(u: ApiUser) {
  selectedUserId.value = u.id
  query.value = `${u.firstName ?? ''} ${u.lastName ?? ''} (${u.email})`.trim()
}

async function confirm() {
  error.value = ''
  okMsg.value = ''
  if (!selectedUserId.value || !selectedRole.value) {
    error.value = 'Select a user and a role.'
    return
  }
  saving.value = true
  try {
    // Backend expects newRoleName as a QUERY PARAM, not a JSON body.
    const roleName = roleMap[selectedRole.value]
    await put(
      `/api/admin/users/${selectedUserId.value}/role?newRoleName=${encodeURIComponent(roleName)}`,
      {} // body unused, but useApi.put requires a second arg
    )
    okMsg.value = 'Role updated.'
    emit('saved')
    setTimeout(() => emit('close'), 1000)
  } catch (e: any) {
    error.value = e?.data?.message || 'Failed to update role.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">

        <div class="modal-header">
          <h2 class="modal-title">{{ $t('users.assignRole') }}</h2>
          <button class="modal-close" aria-label="Close" @click="$emit('close')">×</button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">{{ $t('users.user') }}</label>
            <input v-model="query" type="text" class="form-input" :placeholder="$t('users.searchUser')" />
            <div v-if="loadingUsers" class="hint">Loading users…</div>
            <ul v-else-if="filtered.length" class="user-list">
              <li
                v-for="u in filtered" :key="u.id"
                class="user-item"
                :class="{ 'user-item--active': selectedUserId === u.id }"
                @click="pickUser(u)"
              >
                {{ u.firstName }} {{ u.lastName }} <span class="user-email">{{ u.email }}</span>
              </li>
            </ul>
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('users.role') }}</label>
            <div class="role-options">
              <button
                v-for="role in roles" :key="role"
                class="role-option"
                :class="{ 'role-option--active': selectedRole === role }"
                @click="selectedRole = role"
              >{{ roleLabel(role) }}</button>
            </div>
          </div>

          <p v-if="error" class="msg msg--error">{{ error }}</p>
          <p v-if="okMsg" class="msg msg--ok">{{ okMsg }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" :disabled="saving" @click="$emit('close')">{{ $t('common.cancel') }}</button>
          <button class="btn-primary" :disabled="saving" @click="confirm">
            {{ saving ? 'Saving…' : $t('common.confirm') }}
          </button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card    { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); max-height: 90vh; overflow-y: auto; }
.modal-header  { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem 1.25rem 0; }
.modal-title   { font-size: 1rem; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.modal-close   { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); padding: 0.25rem; border-radius: 4px; line-height: 1; }
.modal-close:hover { background: var(--color-page-bg); }
.modal-body    { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
.form-group    { display: flex; flex-direction: column; gap: 0.375rem; }
.form-label    { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-primary); }
.form-input    { border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.625rem 0.75rem; font-size: 0.875rem; color: var(--color-text-primary); outline: none; }
.form-input:focus { border-color: var(--color-primary); }
.hint { font-size: 0.75rem; color: var(--color-text-muted); }
.user-list { list-style: none; margin: 0.25rem 0 0; padding: 0; border: 1px solid var(--color-border); border-radius: 0.5rem; max-height: 180px; overflow-y: auto; }
.user-item { padding: 0.5rem 0.75rem; font-size: 0.8125rem; cursor: pointer; }
.user-item:hover { background: var(--color-page-bg); }
.user-item--active { background: #FEE2E2; }
.user-email { color: var(--color-text-muted); font-size: 0.75rem; }
.role-options  { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.role-option   { padding: 0.375rem 1rem; border-radius: 999px; border: 1px solid var(--color-border); background: transparent; font-size: 0.8125rem; font-weight: 500; color: var(--color-text-secondary); cursor: pointer; transition: all 0.15s; }
.role-option--active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.msg { font-size: 0.8125rem; margin: 0; }
.msg--error { color: #DC2626; }
.msg--ok    { color: #16A34A; }
.modal-footer  { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }
.btn-primary   { background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem; padding: 0.625rem 1.25rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-secondary { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.625rem 1.25rem; font-size: 0.875rem; cursor: pointer; }
.btn-secondary:hover { background: var(--color-page-bg); }
</style>
