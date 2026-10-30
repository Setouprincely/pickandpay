import type { Product } from '~/apps/market/types'

// Shopping cart — pure client-side state (persisted to localStorage in the layout).
// The backend is only needed at CHECKOUT, to place the order.

export interface CartItem {
  product: Product
  qty: number
}

export function useCart() {
  const items = useState<CartItem[]>('market-cart', () => [])
  const isOpen = useState<boolean>('market-cart-open', () => false)
  const { notify } = useNotify()

  function add(product: Product, qty = 1) {
    if (product.soldOut) return
    const found = items.value.find(i => i.product.id === product.id)
    if (found) found.qty += qty
    else items.value.push({ product, qty })
    notify({ title: 'Added to cart', message: product.title, icon: 'i-heroicons-shopping-cart' })
  }

  function remove(id: Product['id']) {
    items.value = items.value.filter(i => i.product.id !== id)
  }

  function setQty(id: Product['id'], qty: number) {
    const found = items.value.find(i => i.product.id === id)
    if (found) found.qty = Math.max(1, qty)
  }

  function clear() {
    if (!items.value.length) return
    items.value = []
    notify({ title: 'Cart cleared', icon: 'i-heroicons-trash', variant: 'info' })
  }

  /** Number of distinct products. */
  const lineCount = computed(() => items.value.length)
  /** Total quantity across all lines. */
  const totalQty = computed(() => items.value.reduce((n, i) => n + i.qty, 0))
  /** Products sub-total in XAF. Delivery fees are quoted by the backend at checkout (per seller, by distance). */
  const subtotal = computed(() => items.value.reduce((s, i) => s + i.product.price * i.qty, 0))

  const openCart = () => { isOpen.value = true }
  const closeCart = () => { isOpen.value = false }
  const toggleCart = () => { isOpen.value = !isOpen.value }

  return {
    items, isOpen,
    add, remove, setQty, clear,
    lineCount, totalQty, subtotal,
    openCart, closeCart, toggleCart,
  }
}
