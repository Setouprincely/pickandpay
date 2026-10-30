<script setup lang="ts">
// Authenticated app chrome for Xender. Logo + primary nav + user menu.
const { $t } = useNuxtApp()
const { user, logout } = useAuth()
const { notify } = useNotify()
const route = useRoute()

const menuOpen = ref(false)

const initials = computed(() => {
  const n = user.value?.name?.trim()
  if (!n) return 'DX'
  const p = n.split(/\s+/).filter(Boolean)
  const firstChar = p[0]?.[0] ?? ''
  const secondChar = p[1]?.[0] ?? ''
  const combined = (firstChar + secondChar).toUpperCase()
  return combined || 'DX'
})

function isActive(path: string) {
  return route.path === path
}

function signOut() {
  menuOpen.value = false
  logout()
  notify({ title: 'Signed out', message: 'See you soon!', variant: 'info' })
  navigateTo('/login')
}
</script>

<template>
  <header class="xn-hd">
    <div class="xn-hd__inner">
      <NuxtLink to="/" class="xn-hd__brand">
        <img src="/images/logos/Logo-01-trim.png" alt="DOCH Xender" class="xn-hd__logo" />
      </NuxtLink>

      <nav class="xn-hd__nav">
        <NuxtLink to="/" class="xn-hd__link" :class="{ 'xn-hd__link--active': isActive('/') }">
          <UIcon name="i-heroicons-squares-2x2" /> {{ $t('xender.header.shipments') }}
        </NuxtLink>
        <NuxtLink to="/send" class="xn-hd__link" :class="{ 'xn-hd__link--active': isActive('/send') }">
          <UIcon name="i-heroicons-paper-airplane" /> {{ $t('xender.header.send') }}
        </NuxtLink>
      </nav>

      <div class="xn-hd__right">
        <NuxtLink to="/send" class="xn-hd__cta">
          <UIcon name="i-heroicons-plus" /> {{ $t('xender.header.sendShort') }}
        </NuxtLink>

        <div class="xn-hd__user" @click="menuOpen = !menuOpen">
          <span class="xn-hd__avatar">{{ initials }}</span>
          <span class="xn-hd__name">{{ user?.name || $t('xender.header.account') }}</span>
          <UIcon name="i-heroicons-chevron-down" class="xn-hd__caret" />

          <div v-if="menuOpen" class="xn-hd__menu" @click.stop>
            <div class="xn-hd__menu-head">
              <p class="xn-hd__menu-name">{{ user?.name }}</p>
              <p class="xn-hd__menu-phone">+237 {{ user?.phone }}</p>
            </div>
            <button class="xn-hd__menu-item" @click="signOut">
              <UIcon name="i-heroicons-arrow-left-on-rectangle" /> {{ $t('xender.header.signOut') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- click-away to close the menu -->
    <div v-if="menuOpen" class="xn-hd__scrim" @click="menuOpen = false" />
  </header>
</template>

<style scoped>
.xn-hd { position: sticky; top: 0; z-index: 40; background: #fff; border-bottom: 1px solid #E5E7EB; }
.xn-hd__inner { max-width: 1080px; margin: 0 auto; padding: 0 1.25rem; height: 4rem; display: flex; align-items: center; gap: 1.5rem; }
.xn-hd__brand { display: inline-flex; flex-shrink: 0; }
.xn-hd__logo { height: 1.9rem; width: auto; display: block; }

.xn-hd__nav { display: flex; align-items: center; gap: 0.25rem; flex: 1; }
.xn-hd__link { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.85rem; font-weight: 600; color: #6B7280; text-decoration: none; padding: 0.5rem 0.75rem; border-radius: 0.5rem; }
.xn-hd__link:hover { background: #F9FAFB; color: #111827; }
.xn-hd__link--active { color: #E8202A; background: #FEF2F2; }
.xn-hd__link :deep(svg) { width: 1.05rem; height: 1.05rem; }

.xn-hd__right { display: flex; align-items: center; gap: 0.75rem; }
.xn-hd__cta { display: none; align-items: center; gap: 0.35rem; background: #E8202A; color: #fff; font-size: 0.8rem; font-weight: 700; padding: 0.5rem 0.9rem; border-radius: 0.5rem; text-decoration: none; }
.xn-hd__cta:hover { background: #C8181F; }
.xn-hd__cta :deep(svg) { width: 1rem; height: 1rem; }

.xn-hd__user { position: relative; display: flex; align-items: center; gap: 0.5rem; cursor: pointer; padding: 0.3rem 0.4rem; border-radius: 0.5rem; }
.xn-hd__user:hover { background: #F9FAFB; }
.xn-hd__avatar { width: 2rem; height: 2rem; border-radius: 50%; background: #E8202A; color: #fff; font-size: 0.72rem; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.xn-hd__name { font-size: 0.82rem; font-weight: 600; color: #111827; }
.xn-hd__caret { width: 1rem; height: 1rem; color: #9CA3AF; }

.xn-hd__menu { position: absolute; top: calc(100% + 0.5rem); right: 0; z-index: 50; min-width: 14rem; background: #fff; border: 1px solid #E5E7EB; border-radius: 0.6rem; box-shadow: 0 15px 35px -10px rgba(0,0,0,0.2); overflow: hidden; }
.xn-hd__menu-head { padding: 0.85rem 1rem; border-bottom: 1px solid #F3F4F6; }
.xn-hd__menu-name { font-size: 0.85rem; font-weight: 700; color: #111827; margin: 0; }
.xn-hd__menu-phone { font-size: 0.75rem; color: #6B7280; margin: 0.15rem 0 0; }
.xn-hd__menu-item { width: 100%; display: flex; align-items: center; gap: 0.5rem; background: none; border: none; cursor: pointer; font-size: 0.82rem; font-weight: 600; color: #4B5563; padding: 0.75rem 1rem; text-align: left; }
.xn-hd__menu-item:hover { background: #FEF2F2; color: #E8202A; }
.xn-hd__menu-item :deep(svg) { width: 1.05rem; height: 1.05rem; }
.xn-hd__scrim { position: fixed; inset: 0; z-index: 30; }

@media (max-width: 640px) {
  .xn-hd__nav { display: none; }
  /* Nav was the flex spacer; with it hidden, push the profile cluster back to
     the right edge (the header padding keeps it from flushing). */
  .xn-hd__right { margin-left: auto; }
  .xn-hd__cta { display: inline-flex; }
  .xn-hd__name { display: none; }
}
</style>
