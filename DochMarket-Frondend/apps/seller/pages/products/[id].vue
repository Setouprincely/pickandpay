<script setup lang="ts">
import type { ProductDraft } from '~/apps/seller/types'
import { useSellerProducts } from '../../composables/useSellerProducts'

const { $t } = useNuxtApp()
const route = useRoute()
const { byId, update, products, load } = useSellerProducts()
const { notify } = useNotify()

const id = computed(() => route.params.id as string)
const product = computed(() => byId(id.value))

// If the catalogue hasn't been loaded yet (e.g. deep-link to the edit page),
// fetch it so byId can resolve.
onMounted(() => { if (!products.value.length) load() })

const submitting = ref(false)

async function onSubmit(draft: ProductDraft) {
  submitting.value = true
  const res = await update(id.value, draft)
  submitting.value = false

  if (res.ok) {
    notify({ title: $t('seller.editor.updated') })
    navigateTo('/products')
  } else {
    notify({ title: res.error || 'Could not update product', variant: 'error' })
  }
}
</script>

<template>
  <div class="editor">
    <button class="back" @click="() => { navigateTo('/products') }">
      <UIcon name="i-lucide-arrow-left" />
      {{ $t('seller.editor.back') }}
    </button>
    <h1 class="editor__title">{{ $t('seller.editor.editTitle') }}</h1>

    <SellerProductForm
      v-if="product"
      :key="product.id"
      :product="product"
      :submitting="submitting"
      @submit="onSubmit"
      @cancel="() => { navigateTo('/products') }"
    />
    <SellerEmptyState
      v-else
      icon="i-lucide-search-x"
      :title="$t('seller.products.empty.title')"
      :sub="$t('seller.products.empty.sub')"
    >
      <UButton variant="soft" @click="() => { navigateTo('/products') }">{{ $t('seller.editor.back') }}</UButton>
    </SellerEmptyState>
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
