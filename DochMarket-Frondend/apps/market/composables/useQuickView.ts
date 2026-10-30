import type { Product } from '~/apps/market/types'

/**
 * Shared state for the product quick-view modal. Any product card calls open(product);
 * the single <MarketProductQuickView> in the layout renders it.
 */
export function useQuickView() {
  const product = useState<Product | null>('market-quickview', () => null)

  const open = (p: Product) => { product.value = p }
  const close = () => { product.value = null }

  return { product, open, close }
}
