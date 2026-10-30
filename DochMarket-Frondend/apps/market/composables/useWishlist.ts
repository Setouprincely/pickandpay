import type { Product } from '~/apps/market/types'

// Wishlist — client-side state (persisted to localStorage in the layout, like the cart).
// Stores whole Product objects so the wishlist panel can render without a backend round-trip.

export function useWishlist() {
  const items = useState<Product[]>('market-wishlist', () => [])
  const isOpen = useState<boolean>('market-wishlist-open', () => false)
  const { notify } = useNotify()

  const has = (id: Product['id']) => items.value.some(p => p.id === id)

  function add(product: Product) {
    if (has(product.id)) return
    items.value = [...items.value, product]
    notify({ title: 'Added to wishlist', message: product.title, icon: 'i-heroicons-heart' })
  }

  function remove(id: Product['id']) {
    items.value = items.value.filter(p => p.id !== id)
  }

  /** Add if absent, remove if present. Returns the new state. */
  function toggle(product: Product) {
    if (has(product.id)) {
      remove(product.id)
      notify({ title: 'Removed from wishlist', message: product.title, icon: 'i-heroicons-heart', variant: 'info' })
      return false
    }
    add(product)
    return true
  }

  function clear() {
    items.value = []
  }

  const count = computed(() => items.value.length)

  const openWishlist = () => { isOpen.value = true }
  const closeWishlist = () => { isOpen.value = false }
  const toggleWishlist = () => { isOpen.value = !isOpen.value }

  return {
    items, isOpen,
    has, add, remove, toggle, clear,
    count,
    openWishlist, closeWishlist, toggleWishlist,
  }
}
