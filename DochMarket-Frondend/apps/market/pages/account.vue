<script setup lang="ts">
useHead({ title: 'My Account — Doch Market' })

const { history } = useOrder()
const { count: wishlistCount } = useWishlist()
const { format } = useCurrency()
const { user, logout } = useAuth()
const { get, put } = useApi()
const { notify } = useNotify()
const { $t } = useNuxtApp()

async function signOut() {
  await logout()
  notify({ title: $t('market.account.signOut'), message: $t('market.account.signedOut'), variant: 'info' })
  navigateTo('/login')
}

type Tab = 'dashboard' | 'orders' | 'addresses' | 'profile'
const tab = ref<Tab>('dashboard')

const nav = computed<{ id: Tab; label: string; icon: string }[]>(() => [
  { id: 'dashboard', label: $t('market.account.dashboard'), icon: 'i-heroicons-squares-2x2' },
  { id: 'orders', label: $t('market.account.orders'), icon: 'i-heroicons-shopping-bag' },
  { id: 'addresses', label: $t('market.account.addresses'), icon: 'i-heroicons-map-pin' },
  { id: 'profile', label: $t('market.account.profile'), icon: 'i-heroicons-user' },
])

// Real profile from GET /api/users/me; real orders from GET /api/orders/me.
const profile = reactive({
  name: user.value?.name || '',
  email: user.value?.email || '',
  phone: user.value?.phone || '',
})

const orders = ref<Array<{ id: string; reference: string; placedAt: number; total: number; status?: string }>>([])
const saving = ref(false)

onMounted(async () => {
  try {
    const me: any = await get('/api/users/me')
    if (me) {
      profile.name = `${me.firstName ?? ''} ${me.lastName ?? ''}`.trim() || profile.name
      profile.email = me.email ?? profile.email
      profile.phone = me.phone ?? me.tel ?? profile.phone
    }
  } catch { /* keep fallback */ }

  try {
    const list: any[] = await history()
    orders.value = (list ?? []).map(o => ({
      id: o.orderId,
      reference: o.reference ?? '—',
      placedAt: o.createdAt ? new Date(o.createdAt).getTime() : Date.now(),
      total: o.totalAmount ?? 0,
      status: o.status,
    }))
  } catch { /* leave empty */ }
})

async function saveProfile() {
  saving.value = true
  try {
    const parts = profile.name.trim().split(/\s+/)
    await put('/api/users/me', {
      firstName: parts[0] || profile.name,
      lastName: parts.slice(1).join(' ') || (parts[0] || ''),
      phone: profile.phone,
    })
    notify({ title: 'Profile updated', variant: 'success' })
  } catch (e: any) {
    const msg = e?.status === 403 || e?.status === 500
      ? `Couldn't update profile — server rejected the request (${e.status}).`
      : `Couldn't update profile (${e?.status ?? 'network error'}).`
    notify({ title: 'Update failed', message: msg, variant: 'error' })
  } finally {
    saving.value = false
  }
}

const orderDate = (ts: number) =>
  new Date(ts).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
</script>

<template>
  <div>
    <MarketBreadcrumb :items="[{ label: $t('market.account.profile') }]" />

    <section class="acc">
      <div class="acc__inner">
        <!-- Sidebar -->
        <aside class="acc-side">
          <div class="acc-side__user">
            <div class="acc-side__avatar"><UIcon name="i-heroicons-user" /></div>
            <div>
              <p class="acc-side__name">{{ profile.name }}</p>
              <p class="acc-side__email">{{ profile.email }}</p>
            </div>
          </div>
          <nav class="acc-side__nav">
            <button
              v-for="n in nav" :key="n.id"
              class="acc-side__link" :class="{ 'acc-side__link--on': tab === n.id }"
              @click="tab = n.id"
            >
              <UIcon :name="n.icon" /> {{ n.label }}
            </button>
            <NuxtLink to="/wishlist" class="acc-side__link">
              <UIcon name="i-heroicons-heart" /> {{ $t('market.account.wishlist') }}
              <span v-if="wishlistCount" class="acc-side__badge">{{ wishlistCount }}</span>
            </NuxtLink>
            <button class="acc-side__link acc-side__link--out" @click="signOut">
              <UIcon name="i-heroicons-arrow-left-on-rectangle" /> {{ $t('market.account.signOut') }}
            </button>
          </nav>
        </aside>

        <!-- Content -->
        <div class="acc-main">
          <!-- Dashboard -->
          <div v-if="tab === 'dashboard'">
            <h1 class="acc-h1">{{ $t('market.account.dashboard') }}</h1>
            <p class="acc-lead" v-html="$t('market.account.dashboardLead', { name: `<strong>${profile.name}</strong>` })"></p>
            <div class="acc-stats">
              <div class="acc-stat">
                <UIcon name="i-heroicons-shopping-bag" class="acc-stat__icon" />
                <span class="acc-stat__num">{{ orders.length }}</span>
                <span class="acc-stat__label">{{ $t('market.account.orders') }}</span>
              </div>
              <div class="acc-stat">
                <UIcon name="i-heroicons-heart" class="acc-stat__icon" />
                <span class="acc-stat__num">{{ wishlistCount }}</span>
                <span class="acc-stat__label">{{ $t('market.account.wishlist') }}</span>
              </div>
              <div class="acc-stat">
                <UIcon name="i-heroicons-map-pin" class="acc-stat__icon" />
                <span class="acc-stat__num">{{ orders.filter(o => ['PENDING', 'PAYMENT_FAILED'].includes(o.status ?? '')).length }}</span>
                <span class="acc-stat__label">Awaiting payment</span>
              </div>
            </div>
          </div>

          <!-- Orders -->
          <div v-else-if="tab === 'orders'">
            <h1 class="acc-h1">{{ $t('market.account.myOrders') }}</h1>
            <div v-if="orders.length" class="acc-orders">
              <div class="acc-orders__head">
                <span>{{ $t('market.account.colOrder') }}</span><span>{{ $t('market.account.colDate') }}</span><span>{{ $t('market.account.colStatus') }}</span><span>{{ $t('market.account.colTotal') }}</span>
              </div>
              <div v-for="o in orders" :key="o.reference" class="acc-orders__row">
                <NuxtLink :to="`/orders/${o.id}`" class="acc-orders__ref">{{ o.reference }}</NuxtLink>
                <span>{{ orderDate(o.placedAt) }}</span>
                <span><span class="acc-pill">{{ (o.status ?? '').replace(/_/g, ' ').toLowerCase() }}</span></span>
                <span class="acc-orders__total">{{ format(o.total) }}</span>
              </div>
            </div>
            <div v-else class="acc-empty">
              <UIcon name="i-heroicons-shopping-bag" class="acc-empty__icon" />
              <p class="acc-empty__title">{{ $t('market.account.noOrders') }}</p>
              <NuxtLink to="/shop" class="acc-empty__btn">{{ $t('market.account.startShopping') }}</NuxtLink>
            </div>
          </div>

          <!-- Addresses -->
          <div v-else-if="tab === 'addresses'">
            <h1 class="acc-h1">{{ $t('market.account.addressesTitle') }}</h1>
            <div v-if="lastOrder" class="acc-card">
              <p class="acc-card__tag">{{ $t('market.account.defaultShipping') }}</p>
              <p class="acc-addr__name">{{ lastOrder.customer.name }}</p>
              <p class="acc-addr__line">{{ lastOrder.customer.address }}</p>
              <p class="acc-addr__line">{{ lastOrder.customer.city }}, {{ lastOrder.customer.region }}</p>
              <p class="acc-addr__line">{{ lastOrder.customer.phone }}</p>
            </div>
            <div v-else class="acc-empty">
              <UIcon name="i-heroicons-map-pin" class="acc-empty__icon" />
              <p class="acc-empty__title">{{ $t('market.account.noAddresses') }}</p>
              <p class="acc-empty__text">{{ $t('market.account.noAddressesText') }}</p>
            </div>
          </div>

          <!-- Profile -->
          <div v-else>
            <h1 class="acc-h1">{{ $t('market.account.profileDetails') }}</h1>
            <form class="acc-form" @submit.prevent="saveProfile">
              <label class="acc-field">
                <span class="acc-field__label">{{ $t('market.account.fullName') }}</span>
                <input v-model="profile.name" type="text" class="acc-field__input" />
              </label>
              <label class="acc-field">
                <span class="acc-field__label">{{ $t('market.account.email') }}</span>
                <input v-model="profile.email" type="email" class="acc-field__input" />
              </label>
              <label class="acc-field">
                <span class="acc-field__label">{{ $t('market.account.phone') }}</span>
                <input v-model="profile.phone" type="tel" class="acc-field__input" placeholder="+237 …" />
              </label>
              <button type="submit" class="acc-save">{{ $t('market.account.saveChanges') }}</button>
            </form>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.acc__inner { max-width: 1280px; margin: 0 auto; padding: 1.75rem 1.5rem 3rem; display: grid; grid-template-columns: 250px 1fr; gap: 1.5rem; align-items: start; }

/* Sidebar */
.acc-side { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; position: sticky; top: 1rem; }
.acc-side__user { display: flex; align-items: center; gap: 0.75rem; padding-bottom: 1rem; border-bottom: 1px solid var(--color-border, #E5E7EB); margin-bottom: 0.75rem; }
.acc-side__avatar { width: 2.75rem; height: 2.75rem; border-radius: 50%; background: #FEF2F2; color: var(--color-primary, #E8202A); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.acc-side__avatar :deep(svg) { width: 1.4rem; height: 1.4rem; }
.acc-side__name { font-size: 0.875rem; font-weight: 700; color: #111827; margin: 0; }
.acc-side__email { font-size: 0.75rem; color: #9CA3AF; margin: 0.15rem 0 0; overflow: hidden; text-overflow: ellipsis; }
.acc-side__nav { display: flex; flex-direction: column; gap: 0.25rem; }
.acc-side__link { display: flex; align-items: center; gap: 0.625rem; width: 100%; text-align: left; background: none; border: none; cursor: pointer; padding: 0.6rem 0.75rem; border-radius: 0.375rem; font-size: 0.8125rem; color: #4B5563; text-decoration: none; }
.acc-side__link:hover { background: #F9FAFB; color: #111827; }
.acc-side__link--on { background: #FEF2F2; color: var(--color-primary, #E8202A); font-weight: 600; }
.acc-side__link--out { margin-top: 0.5rem; border-top: 1px solid var(--color-border, #E5E7EB); border-radius: 0; padding-top: 0.85rem; color: var(--color-primary, #E8202A); }
.acc-side__link--out:hover { background: #FEF2F2; color: var(--color-primary, #E8202A); }
.acc-side__link :deep(svg) { width: 1.05rem; height: 1.05rem; }
.acc-side__badge { margin-left: auto; background: var(--color-primary, #E8202A); color: #fff; font-size: 0.6875rem; font-weight: 700; border-radius: 999px; min-width: 1.15rem; height: 1.15rem; padding: 0 0.3rem; display: flex; align-items: center; justify-content: center; }

/* Main */
.acc-main { background: #fff; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.5rem 1.75rem; min-height: 22rem; }
.acc-h1 { font-size: 1.25rem; font-weight: 800; color: #111827; margin: 0 0 0.75rem; }
.acc-lead { font-size: 0.875rem; color: #6B7280; margin: 0 0 1.5rem; line-height: 1.6; }

.acc-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.acc-stat { display: flex; flex-direction: column; align-items: flex-start; gap: 0.25rem; background: #F9FAFB; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; }
.acc-stat__icon { width: 1.5rem; height: 1.5rem; color: var(--color-primary, #E8202A); margin-bottom: 0.25rem; }
.acc-stat__num { font-size: 1.5rem; font-weight: 800; color: #111827; }
.acc-stat__label { font-size: 0.75rem; color: #6B7280; }

/* Orders table */
.acc-orders__head, .acc-orders__row { display: grid; grid-template-columns: 1.2fr 1fr 1fr 0.8fr; gap: 1rem; align-items: center; padding: 0.875rem 0; }
.acc-orders__head { font-size: 0.6875rem; font-weight: 700; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.03em; border-bottom: 1px solid var(--color-border, #E5E7EB); }
.acc-orders__row { border-bottom: 1px solid var(--color-border, #E5E7EB); font-size: 0.8125rem; color: #4B5563; }
.acc-orders__ref { font-weight: 700; color: #111827; }
.acc-orders__total { font-weight: 700; color: var(--color-primary, #E8202A); }
.acc-pill { background: #FEF3C7; color: #B45309; font-size: 0.6875rem; font-weight: 700; padding: 0.2rem 0.55rem; border-radius: 999px; }

/* Cards / empty */
.acc-card { background: #F9FAFB; border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.5rem; padding: 1.25rem; max-width: 24rem; }
.acc-card__tag { font-size: 0.6875rem; font-weight: 700; color: var(--color-primary, #E8202A); text-transform: uppercase; letter-spacing: 0.03em; margin: 0 0 0.625rem; }
.acc-addr__name { font-size: 0.875rem; font-weight: 700; color: #111827; margin: 0 0 0.4rem; }
.acc-addr__line { font-size: 0.8125rem; color: #6B7280; margin: 0 0 0.3rem; }

.acc-empty { text-align: center; padding: 3rem 1rem; }
.acc-empty__icon { width: 2.5rem; height: 2.5rem; color: #D1D5DB; }
.acc-empty__title { font-size: 1rem; font-weight: 700; color: #374151; margin: 0.75rem 0 0.5rem; }
.acc-empty__text { font-size: 0.8125rem; color: #9CA3AF; margin: 0 0 1rem; }
.acc-empty__btn { background: var(--color-primary, #E8202A); color: #fff; text-decoration: none; border-radius: 0.375rem; padding: 0.55rem 1.5rem; font-size: 0.8125rem; font-weight: 700; }

/* Profile form */
.acc-form { display: flex; flex-direction: column; gap: 1rem; max-width: 26rem; }
.acc-field { display: flex; flex-direction: column; gap: 0.35rem; }
.acc-field__label { font-size: 0.75rem; font-weight: 600; color: #6B7280; }
.acc-field__input { border: 1px solid var(--color-border, #E5E7EB); border-radius: 0.375rem; padding: 0.6rem 0.75rem; font-size: 0.875rem; color: #111827; outline: none; }
.acc-field__input:focus { border-color: var(--color-primary, #E8202A); }
.acc-save { align-self: flex-start; background: var(--color-primary, #E8202A); color: #fff; border: none; border-radius: 0.375rem; padding: 0.65rem 1.75rem; font-size: 0.8125rem; font-weight: 700; cursor: pointer; }
.acc-save:hover { background: var(--color-primary-hover, #C8181F); }

@media (max-width: 860px) {
  .acc__inner { grid-template-columns: 1fr; }
  .acc-side { position: static; }
}
@media (max-width: 560px) {
  .acc__inner { padding: 1.25rem 1rem 2.5rem; gap: 1rem; }
  .acc-side { padding: 1rem; }
  .acc-main { padding: 1.1rem; min-height: 0; }
  .acc-h1 { font-size: 1.05rem; }
  .acc-lead { font-size: 0.8rem; margin-bottom: 1.1rem; }
  /* Stats: 2-up instead of full-width blocks so they don't look oversized */
  .acc-stats { grid-template-columns: 1fr 1fr; gap: 0.7rem; }
  .acc-stat { padding: 0.85rem; }
  .acc-stat__num { font-size: 1.25rem; }
  .acc-stat__icon { width: 1.25rem; height: 1.25rem; }
  .acc-orders__head { display: none; }
  .acc-orders__row { grid-template-columns: 1fr 1fr; }
}
</style>
