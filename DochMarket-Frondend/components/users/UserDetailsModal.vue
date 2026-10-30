<script setup lang="ts">
/*
 * UserDetailsModal — opens in VIEW or EDIT mode, pre-filled with one specific
 * user's real data fetched fresh from GET /api/admin/users/{userId}.
 *
 * Save behaviour:
 *  - Role changes go to the REAL endpoint: PUT /api/admin/users/{id}/role?newRoleName=...
 *  - Individual roles: DELETE /api/admin/users/{id}/roles/{role}; whole account: DELETE /api/admin/users/{id}
 *  - There is NO backend endpoint to edit name/email/phone for an arbitrary user, so those fields are read-only.
 */
const props = defineProps<{
  userId: string
  mode: 'view' | 'edit'
}>()
const emit = defineEmits<{ close: []; saved: [] }>()

const { $t } = useNuxtApp()
const { get, put, del } = useApi()

const loading = ref(true)
const error = ref('')
const saving = ref(false)
const okMsg = ref('')

type ApiUser = {
  id: string
  firstName?: string
  lastName?: string
  email?: string
  tel?: string
  phone?: string
  enabled?: boolean
  role?: string
  roles?: string[]
}
const u = ref<ApiUser | null>(null)

const roleMap: Record<string, string> = {
  Client: 'ROLE_BUYER',
  Driver: 'ROLE_DELIVERY',
  Vendor: 'ROLE_SELLER',
  Admin:  'ROLE_SUPER_ADMIN',
}
const selectedRole = ref<string>('')

const roleLabelFromApi = (user: ApiUser) => {
  const r = user.role ?? ''
  if (r.includes('SUPER_ADMIN') || r.includes('ADMIN')) return 'Admin'
  if (r.includes('DELIVERY')) return 'Driver'
  if (r.includes('SELLER')) return 'Vendor'
  if (r.includes('BUYER')) return 'Client'
  return 'Client'
}

onMounted(async () => {
  try {
    // GET /api/admin/users/{userId} — this specific user's real data
    const res = await get<ApiUser>(`/api/admin/users/${props.userId}`)
    u.value = res
    selectedRole.value = roleLabelFromApi(res)
  } catch (e: any) {
    error.value = `Failed to load user (${e?.status ?? 'network error'})`
  } finally {
    loading.value = false
  }
})

const fullName = computed(() =>
  u.value ? `${u.value.firstName ?? ''} ${u.value.lastName ?? ''}`.trim() : ''
)
const phone = computed(() => u.value?.tel ?? u.value?.phone ?? '—')

const ROLE_LABELS: Record<string, string> = {
  ROLE_BUYER: 'Client (buyer)',
  ROLE_SELLER: 'Vendor (seller)',
  ROLE_DELIVERY: 'Driver',
  ROLE_SUPER_ADMIN: 'Super admin',
  ROLE_MARKET_ADMIN: 'Market admin',
  ROLE_XENDER_ADMIN: 'Xender admin',
}
const userRoles = computed(() => (u.value?.roles?.length ? u.value.roles : (u.value?.role ? [u.value.role] : [])))
const removingRole = ref('')
const deleting = ref(false)

// DELETE /api/admin/users/{id}/roles/{role}: the person keeps their other roles.
async function removeRole(role: string) {
  if (!u.value) return
  const label = ROLE_LABELS[role] ?? role
  if (!confirm(`Remove ${label} access from ${u.value.email}? They keep their other roles.`)) return
  error.value = ''
  okMsg.value = ''
  removingRole.value = role
  try {
    u.value = await del<ApiUser>(`/api/admin/users/${u.value.id}/roles/${role}`)
    selectedRole.value = roleLabelFromApi(u.value)
    okMsg.value = `${label} access removed.`
    emit('saved')
  } catch (e: any) {
    error.value = e?.data?.message || 'Could not remove this role.'
  } finally {
    removingRole.value = ''
  }
}

async function deleteUser() {
  if (!u.value) return
  if (!confirm(`Delete ${u.value.email}? They will no longer be able to sign in. Their past orders stay in the records.`)) return
  deleting.value = true
  try {
    await del(`/api/admin/users/${u.value.id}`)
    emit('saved')
    emit('close')
  } catch (e: any) {
    error.value = e?.data?.message || 'Could not delete this user.'
  } finally {
    deleting.value = false
  }
}

async function saveRole() {
  if (!u.value) return
  okMsg.value = ''
  saving.value = true
  try {
    const roleName = roleMap[selectedRole.value]
    // PUT /api/admin/users/{id}/role?newRoleName=ROLE_X  (query param, not body)
    await put(
      `/api/admin/users/${u.value.id}/role?newRoleName=${encodeURIComponent(roleName)}`,
      {}
    )
    okMsg.value = 'Role updated.'
    emit('saved')
    setTimeout(() => emit('close'), 900)
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
          <h3 class="modal-title">{{ mode === 'edit' ? 'Edit user' : 'User details' }}</h3>
          <button class="modal-close" @click="$emit('close')">×</button>
        </div>

        <div v-if="loading" class="modal-state">Loading…</div>
        <div v-else-if="error" class="modal-state modal-state--error">{{ error }}</div>

        <template v-else-if="u">
          <div class="modal-body">
            <div class="fg">
              <label>Name</label>
              <input class="fi" :value="fullName" disabled />
            </div>
            <div class="fg">
              <label>Email</label>
              <input class="fi" :value="u.email" disabled />
            </div>
            <div class="fg">
              <label>Phone</label>
              <input class="fi" :value="phone" disabled />
            </div>
            <div class="fg">
              <label>Status</label>
              <input class="fi" :value="u.enabled ? 'Active' : 'Inactive'" disabled />
            </div>
            <div class="fg">
              <label>Roles on this account</label>
              <div class="roles">
                <span v-for="r in userRoles" :key="r" class="role-chip">
                  {{ ROLE_LABELS[r] ?? r }}
                  <button
                    v-if="mode === 'edit' && userRoles.length > 1"
                    class="role-chip__remove"
                    :disabled="!!removingRole"
                    :title="`Remove ${ROLE_LABELS[r] ?? r}`"
                    @click="removeRole(r)"
                  >{{ removingRole === r ? '…' : 'Remove' }}</button>
                </span>
              </div>
            </div>
            <div class="fg">
              <label>Primary role</label>
              <select v-model="selectedRole" class="fi" :disabled="mode === 'view'">
                <option value="Client">{{ $t('role.client') }}</option>
                <option value="Driver">{{ $t('role.driver') }}</option>
                <option value="Vendor">{{ $t('role.vendor') }}</option>
                <option value="Admin">Admin</option>
              </select>
            </div>

            <!-- Only role changes persist (no backend endpoint for editing other users' name/email/phone) -->
            <p v-if="okMsg" class="msg msg--ok">{{ okMsg }}</p>
          </div>

          <div class="modal-footer">
            <button v-if="mode === 'edit'" class="btn-danger" :disabled="deleting" @click="deleteUser">
              {{ deleting ? 'Deleting…' : 'Delete user' }}
            </button>
            <button class="btn-secondary" @click="$emit('close')">Close</button>
            <button v-if="mode === 'edit'" class="btn-primary" :disabled="saving" @click="saveRole">
              {{ saving ? 'Saving…' : 'Save role' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-card { background: #fff; border-radius: 0.75rem; width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1.1rem 1.25rem 0; }
.modal-title { font-size: 1rem; font-weight: 700; margin: 0; color: var(--color-text-primary); }
.modal-close { background: none; border: none; font-size: 1.25rem; cursor: pointer; color: var(--color-text-muted); line-height: 1; }
.modal-state { padding: 2rem; text-align: center; font-size: 0.8125rem; color: var(--color-text-secondary); }
.modal-state--error { color: #DC2626; }
.modal-body { padding: 1.25rem; display: flex; flex-direction: column; gap: 0.875rem; }
.fg { display: flex; flex-direction: column; gap: 0.3rem; }
.fg label { font-size: 0.8125rem; font-weight: 500; color: var(--color-text-primary); }
.fi { border: 1px solid var(--color-border); border-radius: 0.375rem; padding: 0.5rem 0.75rem; font-size: 0.875rem; outline: none; background: #fff; color: var(--color-text-primary); }
.fi:disabled { background: #F9FAFB; color: var(--color-text-secondary); }
.fi:focus { border-color: var(--color-primary); }
.msg--ok { color: #16A34A; font-size: 0.8125rem; margin: 0; }
.modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 1rem 1.25rem; border-top: 1px solid var(--color-border); }
.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: 0.5rem; padding: 0.55rem 1.25rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-danger { margin-right: auto; background: transparent; color: #DC2626; border: 1px solid #FCA5A5; border-radius: 0.5rem; padding: 0.55rem 1rem; font-size: 0.875rem; cursor: pointer; }
.btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }
.roles { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.role-chip { display: inline-flex; align-items: center; gap: 0.4rem; background: #F3F4F6; border-radius: 999px; padding: 0.25rem 0.65rem; font-size: 0.8125rem; }
.role-chip__remove { background: none; border: none; color: #DC2626; font-size: 0.75rem; font-weight: 600; cursor: pointer; padding: 0; }
.role-chip__remove:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: 0.5rem; padding: 0.55rem 1.25rem; font-size: 0.875rem; cursor: pointer; }
</style>
