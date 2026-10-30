// Shared product shape for the market storefront.
// Storefront product shape, mapped from the backend ProductResponse in composables/useProducts.ts.

export type BadgeVariant = 'discount' | 'hot' | 'best' | 'sale' | 'sold'

export interface ProductBadge {
  text: string
  variant?: BadgeVariant
}

export interface Product {
  id: string | number
  title: string
  /** Base price in XAF (FCFA). Formatted/converted for display via useCurrency(). */
  price: number
  /** Original price in XAF (FCFA), for the struck-through "was" price. */
  oldPrice?: number
  /** Image URL or path (e.g. '/images/market/products/xbox.jpg' or a CDN URL from the backend). */
  image?: string
  vendor?: string
  vendorLocation?: string
  stock?: string
  rating?: number
  reviews?: string | number
  /** Used by the Featured Products tabs: 'Smart Phone' | 'Laptop' | 'Headphone' | 'TV' */
  category?: string
  badges?: ProductBadge[]
  soldOut?: boolean
  /** Render the large card variant (with rating row + Add to Cart). */
  detailed?: boolean

  // ── Optional fields shown in the quick-view detail modal ──
  /** Stock-keeping unit, e.g. 'A264671'. Falls back to the id. */
  sku?: string
  brand?: string
  /** e.g. 'In Stock' / 'Out of Stock'. Falls back from `soldOut`. */
  availability?: string
  /** Explicit discount %; otherwise computed from oldPrice/price. */
  discountPercent?: number
  /** Total feedback count for "(N) User feedback". Falls back to `reviews`. */
  userFeedback?: string | number
  /** Extra images for the thumbnail strip; falls back to [image]. */
  gallery?: string[]
  /** Selectable options — each renders its own selector if present. */
  colors?: string[]          // hex values
  sizes?: string[]
  memoryOptions?: string[]
  storageOptions?: string[]
}
