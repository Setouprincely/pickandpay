<script setup lang="ts">
// Cascading Cameroon address picker for checkout: Region → City → Subdivision →
// area/landmark. Each select only shows what lives inside the previous choice, so
// the lists stay short. The final "area" is searchable over the subdivision's
// known quartiers, but free text is accepted too (no fixed list covers every
// corner). Emits a structured PickedLocation, or null until a subdivision is set.
import type { PickedLocation } from '../composables/useLocations'

const props = defineProps<{
  modelValue: PickedLocation | null
  /** Marks the whole group invalid (parent-driven). */
  invalid?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [PickedLocation | null] }>()

const { $t } = useNuxtApp()
const { regions, citiesFor, subdivisionsFor, getSubdivision, build } = useLocations()

// Internal selection state — seeded from any incoming value.
const regionId = ref(props.modelValue?.regionId ?? '')
const cityId = ref(props.modelValue?.cityId ?? '')
const subId = ref(props.modelValue?.subdivisionId ?? '')
const landmark = ref(props.modelValue?.landmark ?? '')

// Cascade resets: changing a level clears everything below it.
watch(regionId, () => { cityId.value = ''; subId.value = ''; landmark.value = '' })
watch(cityId, () => { subId.value = ''; landmark.value = '' })
watch(subId, () => { landmark.value = ''; suggestOpen.value = false })

// Emit the structured value (or null) whenever the selection changes.
watch([regionId, cityId, subId, landmark], () => {
  emit('update:modelValue', build(regionId.value, cityId.value, subId.value, landmark.value))
})

// ---- options ----
const cityOptions = computed(() => citiesFor(regionId.value))
const subOptions = computed(() => subdivisionsFor(regionId.value, cityId.value))
const quartiers = computed(() => getSubdivision(regionId.value, cityId.value, subId.value)?.quartiers ?? [])

// ---- area/landmark searchable suggestions ----
const suggestOpen = ref(false)
const suggestions = computed(() => {
  const q = landmark.value.trim().toLowerCase()
  if (!q) return quartiers.value
  return quartiers.value.filter(x => x.toLowerCase().includes(q))
})
function pickQuartier(name: string) {
  landmark.value = name
  suggestOpen.value = false
}
</script>

<template>
  <div class="lp" :class="{ 'lp--invalid': invalid }">
    <div class="lp__grid">
      <!-- Region -->
      <label class="field">
        <span class="field__label">{{ $t('market.picker.region') }}</span>
        <div class="field__control">
          <UIcon name="i-heroicons-globe-africa" class="field__lead" />
          <select v-model="regionId" class="field__input field__input--icon field__select">
            <option value="" disabled>{{ $t('market.picker.regionPh') }}</option>
            <option v-for="r in regions()" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select>
        </div>
      </label>

      <!-- City -->
      <label class="field">
        <span class="field__label">{{ $t('market.picker.city') }}</span>
        <div class="field__control">
          <UIcon name="i-heroicons-building-office-2" class="field__lead" />
          <select v-model="cityId" class="field__input field__input--icon field__select" :disabled="!regionId">
            <option value="" disabled>{{ regionId ? $t('market.picker.cityPh') : $t('market.picker.cityWait') }}</option>
            <option v-for="c in cityOptions" :key="c.id" :value="c.id" :disabled="c.comingSoon">
              {{ c.name }}{{ c.comingSoon ? ` — ${$t('market.picker.comingSoon')}` : '' }}
            </option>
          </select>
        </div>
      </label>
    </div>

    <!-- Subdivision -->
    <label class="field">
      <span class="field__label">{{ $t('market.picker.subdivision') }}</span>
      <div class="field__control">
        <UIcon name="i-heroicons-map" class="field__lead" />
        <select v-model="subId" class="field__input field__input--icon field__select" :disabled="!cityId">
          <option value="" disabled>{{ cityId ? $t('market.picker.subdivisionPh') : $t('market.picker.subdivisionWait') }}</option>
          <option v-for="s in subOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </div>
    </label>

    <!-- Area / landmark (searchable + free text) -->
    <label class="field">
      <span class="field__label">{{ $t('market.picker.area') }} <span class="field__opt">{{ $t('market.picker.optional') }}</span></span>
      <div class="field__control">
        <UIcon name="i-heroicons-map-pin" class="field__lead" />
        <input
          v-model="landmark"
          type="text"
          class="field__input field__input--icon"
          :disabled="!subId"
          :placeholder="subId ? $t('market.picker.areaPh') : $t('market.picker.areaWait')"
          autocomplete="off"
          @focus="suggestOpen = true"
          @input="suggestOpen = true"
        />
        <!-- suggestion dropdown -->
        <div v-if="suggestOpen && subId && suggestions.length" class="lp-sugg">
          <button
            v-for="q in suggestions"
            :key="q"
            type="button"
            class="lp-sugg__item"
            @click="pickQuartier(q)"
          >
            <UIcon name="i-heroicons-map-pin" /> {{ q }}
          </button>
        </div>
      </div>
      <p v-if="subId" class="field__hint">{{ $t('market.picker.areaHint') }}</p>
    </label>

    <!-- click-away closes the suggestion list -->
    <div v-if="suggestOpen" class="lp-scrim" @click="suggestOpen = false" />
  </div>
</template>

<style scoped>
.lp { position: relative; display: flex; flex-direction: column; gap: 1rem; grid-column: 1 / -1; }
.lp--invalid { /* the inner selects show their own error border via parent if needed */ }
.lp__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }

.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.field__opt { color: #9CA3AF; font-weight: 500; }
.field__control { position: relative; display: flex; align-items: center; }
.field__lead { position: absolute; left: 0.7rem; width: 1rem; height: 1rem; color: #9CA3AF; pointer-events: none; }
.field__input { width: 100%; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.6rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; background: #fff; transition: border-color 0.15s; }
.field__input--icon { padding-left: 2.25rem; }
.field__select { appearance: none; cursor: pointer; }
.field__input:focus { border-color: var(--color-primary, #E8202A); }
.field__input:disabled { background: #F9FAFB; color: #9CA3AF; cursor: not-allowed; }
.lp--invalid .field__select { border-color: var(--color-primary, #E8202A); background: #FEF2F2; }
.field__hint { font-size: 0.7rem; color: #9CA3AF; margin: 0.1rem 0 0; }

/* suggestion dropdown */
.lp-sugg { position: absolute; top: calc(100% + 0.25rem); left: 0; right: 0; z-index: 20; max-height: 12rem; overflow-y: auto; background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; box-shadow: 0 12px 28px -10px rgba(0,0,0,0.2); padding: 0.25rem; }
.lp-sugg__item { width: 100%; display: flex; align-items: center; gap: 0.5rem; background: none; border: none; cursor: pointer; font-size: 0.82rem; color: #374151; padding: 0.5rem 0.6rem; border-radius: 0.4rem; text-align: left; }
.lp-sugg__item:hover { background: #FEF2F2; color: var(--color-primary, #E8202A); }
.lp-sugg__item :deep(svg) { width: 0.95rem; height: 0.95rem; color: #9CA3AF; flex-shrink: 0; }
.lp-scrim { position: fixed; inset: 0; z-index: 10; }

@media (max-width: 520px) {
  .lp__grid { grid-template-columns: 1fr; }
}
</style>
