<script setup lang="ts">
import UserTable from '~/components/users/UserTable.vue'
import AssignRoleModal from '~/components/users/AssignRoleModal.vue'
import AddUserModal from '~/components/users/AddUserModal.vue'
import FilterModal from '~/components/users/FilterModal.vue'

const showAssignRole = ref(false)
const showAddUser    = ref(false)
const showFilter     = ref(false)

const filters = ref({ role: 'All', status: 'All', dateFrom: '', dateTo: '' })

// ref to the table so we can call its exposed refresh() after changes
const tableRef = ref<{ refresh: () => void } | null>(null)

function onSaved() {
  // reload the user list after a create / role change
  tableRef.value?.refresh()
}

function onApplyFilters(f: typeof filters.value) {
  filters.value = f
}
</script>

<template>
  <div class="users-page">

    <div class="users-actions">
      <button class="btn-filter" aria-label="Filter" @click="showFilter = true">
        <UIcon name="i-heroicons-funnel" class="btn-filter__icon" />
        {{ $t('common.filter') }}
      </button>
      <button class="btn-primary" @click="showAssignRole = true">
        <span>+</span> {{ $t('users.assignRole') }}
      </button>
      <button class="btn-primary" @click="showAddUser = true">
        <span>+</span> {{ $t('users.addUser') }}
      </button>
    </div>

    <UserTable ref="tableRef" :filters="filters" />

    <AssignRoleModal v-if="showAssignRole" @close="showAssignRole = false" @saved="onSaved" />
    <AddUserModal    v-if="showAddUser"    @close="showAddUser = false"    @saved="onSaved" />
    <FilterModal     v-if="showFilter"     @close="showFilter = false"     @apply="onApplyFilters" />

  </div>
</template>

<style scoped>
.users-page    { display: flex; flex-direction: column; gap: 1.25rem; padding: 1rem; min-width: 0; }
.users-actions { display: flex; justify-content: flex-end; align-items: center; gap: 0.75rem; flex-wrap: wrap; }

.btn-primary {
  display: flex; align-items: center; gap: 0.375rem;
  background: var(--color-primary); color: #fff;
  border: none; border-radius: 0.5rem;
  padding: 0.625rem 1.125rem;
  font-size: 0.875rem; font-weight: 600; cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: var(--color-primary-hover); }

.btn-filter {
  display: flex; align-items: center; gap: 0.375rem;
  background: transparent; color: var(--color-text-secondary);
  border: 1px solid var(--color-border); border-radius: 0.5rem;
  padding: 0.625rem 1rem; font-size: 0.875rem; font-weight: 500; cursor: pointer;
  transition: background 0.15s;
}
.btn-filter:hover { background: var(--color-page-bg); }
.btn-filter__icon { width: 1rem; height: 1rem; }

@media (min-width: 1024px) { .users-page { padding: 1.5rem; } }
</style>