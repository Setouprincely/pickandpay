<script setup lang="ts">
defineProps<{
  index: number
  state: 'completed' | 'active' | 'pending'
  time: string
  address: string
  urgent?: boolean
}>()

defineEmits<{ call: []; validate: []; start: [] }>()

const { $t } = useNuxtApp()
</script>

<template>
  <div class="task" :class="`task--${state}`">

    <!-- Header: number + title + time -->
    <div class="task-head">
      <span class="task-num" :class="`task-num--${state}`">{{ index }}</span>
      <span class="task-title">{{ $t('driver.task.title') }}</span>
      <span class="task-time">{{ time }}</span>
    </div>

    <!-- Urgent tag (active only) -->
    <div v-if="urgent" class="task-tag-row">
      <span class="task-urgent">{{ $t('driver.task.urgent') }}</span>
    </div>

    <!-- Address -->
    <div class="task-address">
      <UIcon name="i-heroicons-map-pin" class="task-address__icon" />
      <span>{{ address }}</span>
    </div>

    <!-- Completed banner -->
    <div v-if="state === 'completed'" class="task-done">
      <UIcon name="i-heroicons-check-circle" class="task-done__icon" />
      {{ $t('driver.task.completed') }}
    </div>

    <!-- Action buttons -->
    <div v-else class="task-actions">
      <button class="btn-call" @click="$emit('call')">
        <UIcon name="i-heroicons-phone" class="btn-call__icon" />
        {{ $t('driver.task.call') }}
      </button>

      <button v-if="state === 'active'" class="btn-validate" @click="$emit('validate')">
        <UIcon name="i-heroicons-check-circle" class="btn-validate__icon" />
        {{ $t('driver.task.validate') }}
      </button>
      <button v-else class="btn-start" @click="$emit('start')">
        <UIcon name="i-heroicons-paper-airplane" class="btn-start__icon" />
        {{ $t('driver.task.start') }}
      </button>
    </div>

    <!-- Checklist / report link -->
    <NuxtLink to="/delivery-checklist" class="task-report">
      <UIcon name="i-heroicons-clipboard-document-check" class="task-report__icon" />
      {{ $t('driver.task.checklist') }}
    </NuxtLink>

  </div>
</template>

<style scoped>
.task {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-left-width: 4px;
  border-radius: 0.625rem;
  padding: 1rem 1.25rem;
}
.task--completed { border-left-color: #16A34A; }
.task--active    { border-left-color: #2563EB; }
.task--pending   { border-left-color: #E5E7EB; }

/* Header */
.task-head { display: flex; align-items: center; gap: 0.75rem; }
.task-num  { width: 1.75rem; height: 1.75rem; border-radius: 50%; flex-shrink: 0;
             display: flex; align-items: center; justify-content: center;
             font-size: 0.8125rem; font-weight: 700; }
.task-num--completed { background: #DCFCE7; color: #16A34A; }
.task-num--active    { background: #2563EB; color: #fff; }
.task-num--pending   { background: #9CA3AF; color: #fff; }
.task-title { font-size: 0.9375rem; font-weight: 700; color: var(--color-text-primary); }
.task-time  { margin-left: auto; font-size: 0.8125rem; color: var(--color-text-secondary); }

/* Urgent */
.task-tag-row { display: flex; justify-content: flex-end; margin-top: 0.5rem; }
.task-urgent  { font-size: 0.625rem; font-weight: 600; color: #DC2626; background: #FEE2E2;
                padding: 0.15rem 0.6rem; border-radius: 999px; }

/* Address */
.task-address { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.75rem;
                background: #F3F4F6; border-radius: 0.5rem; padding: 0.625rem 0.75rem;
                font-size: 0.8125rem; color: var(--color-text-primary); }
.task-address__icon { width: 1rem; height: 1rem; color: var(--color-text-muted); flex-shrink: 0; }

/* Completed banner */
.task-done { display: flex; align-items: center; justify-content: center; gap: 0.5rem;
             margin-top: 0.75rem; background: #DCFCE7; color: #16A34A;
             border-radius: 0.5rem; padding: 0.625rem; font-size: 0.8125rem; font-weight: 600; }
.task-done__icon { width: 1.125rem; height: 1.125rem; }

/* Actions */
.task-actions { display: flex; gap: 0.625rem; margin-top: 0.75rem; }
.btn-call {
  display: flex; align-items: center; gap: 0.375rem;
  background: #F3F4F6; color: var(--color-text-secondary);
  border: 1px solid var(--color-border); border-radius: 0.5rem;
  padding: 0.625rem 0.875rem; font-size: 0.8125rem; cursor: pointer; flex-shrink: 0;
}
.btn-call:hover  { background: #E5E7EB; }
.btn-call__icon  { width: 0.9375rem; height: 0.9375rem; }

.btn-validate {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  background: #2563EB; color: #fff; border: none; border-radius: 0.5rem;
  padding: 0.625rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer;
}
.btn-validate:hover { background: #1D4ED8; }
.btn-validate__icon { width: 1.0625rem; height: 1.0625rem; }

.btn-start {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  background: #fff; color: #2563EB; border: 1px solid #2563EB; border-radius: 0.5rem;
  padding: 0.625rem 1rem; font-size: 0.8125rem; font-weight: 600; cursor: pointer;
}
.btn-start:hover { background: #EFF6FF; }
.btn-start__icon { width: 1rem; height: 1rem; }

/* Checklist link */
.task-report {
  display: inline-flex; align-items: center; gap: 0.375rem;
  margin-top: 0.75rem; font-size: 0.75rem; font-weight: 500;
  color: var(--color-text-secondary); text-decoration: none;
}
.task-report:hover { color: var(--color-primary); }
.task-report__icon { width: 0.9375rem; height: 0.9375rem; }
</style>
