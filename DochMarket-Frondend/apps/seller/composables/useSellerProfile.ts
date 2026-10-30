// The logged-in seller's own Seller Profile row (store name, city, address,
// etc.) — separate from the User account itself. GET /api/sellers/byUserId
// takes ?userId= as a query param and returns an ARRAY of seller profiles
// for that user (not a single object), matching the same pattern already
// used in useSellerProducts.ts.
export interface SellerProfile {
  id: string
  storeName: string
  city: string
  address: string
  rating: string
  contactNumber: string
  isVerified: boolean
}

export function useSellerProfile() {
  const { get } = useApi()
  const { user } = useAuth()

  const profile = useState<SellerProfile | null>('seller-own-profile', () => null)
  const loading = useState<boolean>('seller-own-profile-loading', () => false)
  const loaded = useState<boolean>('seller-own-profile-loaded', () => false)

  async function load(force = false) {
    if (loaded.value && !force) return profile.value
    if (!user.value?.id) return null
    loading.value = true
    try {
      const rows = await get<SellerProfile[]>(`/api/sellers/byUserId?userId=${encodeURIComponent(user.value.id)}`)
      profile.value = Array.isArray(rows) && rows.length ? rows[0] : null
      loaded.value = true
      return profile.value
    } catch {
      return null
    } finally {
      loading.value = false
    }
  }

  // Two-letter initials for the avatar badge, from the store name.
  const initials = computed(() => {
    const name = profile.value?.storeName?.trim()
    if (!name) return '··'
    const parts = name.split(/\s+/).filter(Boolean)
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
    return (parts[0][0] + parts[1][0]).toUpperCase()
  })

  return { profile, loading, loaded, load, initials }
}
