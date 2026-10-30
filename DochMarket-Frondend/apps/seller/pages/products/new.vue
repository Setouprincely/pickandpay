<script setup lang="ts">
import type { ProductDraft } from '~/apps/seller/types'
import { useSellerProducts } from '../../composables/useSellerProducts'

const { $t } = useNuxtApp()
const { add } = useSellerProducts()
const { notify } = useNotify()

const submitting = ref(false)

async function onSubmit(draft: ProductDraft) {
  submitting.value = true
  const res = await add(draft)
  submitting.value = false

  if (res.ok) {
    notify({ title: $t('seller.editor.created') })
    navigateTo('/products')
  } else {
    // Specific, readable message (surfaces the known backend 403/500 clearly).
    notify({ title: res.error || 'Could not create product', variant: 'error' })
  }
}
</script>

<template>
  <div class="editor">
    <button class="back" @click="navigateTo('/products')">
      <UIcon name="i-lucide-arrow-left" />
      {{ $t('seller.editor.back') }}
    </button>
    <h1 class="editor__title">{{ $t('seller.editor.newTitle') }}</h1>

    <SellerProductForm :submitting="submitting" @submit="onSubmit" @cancel="navigateTo('/products')" />
  </div>
</template>

<style scoped>
.editor { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; min-width: 0; }
.back {
  display: inline-flex; align-items: center; gap: 0.375rem; background: none; border: none; cursor: pointer;
  color: var(--color-text-secondary); font-size: 0.825rem; font-weight: 500; padding: 0; width: fit-content;
}
.back:hover { color: var(--color-primary); }
.back :deep(svg) { width: 1rem; height: 1rem; }
.editor__title { font-size: 1.25rem; font-weight: 800; color: var(--color-text-primary); margin: 0; }
@media (min-width: 1024px) { .editor { padding: 1.5rem; gap: 1.25rem; } }
</style>
