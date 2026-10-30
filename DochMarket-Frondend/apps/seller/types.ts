// Seller catalogue types. The seller app is the *supply side* of the market
// storefront, so a seller-managed product is the market `Product` plus the
// extra fields a seller needs to manage it (status, real stock count, units
// sold, created date). When wired to the backend these objects should serialize
// down to the market `Product` shape (see apps/market/types.ts).

import type { Product } from '~/apps/market/types'

/** active = visible when published; draft = deactivated by the seller; out_of_stock = active but no stock. */
export type ProductStatus = 'active' | 'draft' | 'out_of_stock'

export interface SellerProduct extends Product {
  /** Lifecycle/visibility on the storefront. */
  status: ProductStatus
  /** Real numeric stock on hand (the market `stock` string is display-only). */
  stockCount: number
  /** Lifetime units sold — drives the catalogue "Sold" column and analytics. */
  sold: number
  /** ISO date the product was added. */
  createdAt: string
  /** Long-form description shown on the product detail page. */
  description?: string
  weightKg?: number
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  /** Marketplace moderation: PENDING_REVIEW | PUBLISHED | REJECTED */
  publicationStatus?: string
  rejectionReason?: string
  /** True when customers can currently see and buy it. */
  visibleToCustomers?: boolean
}

/** Payload from the product editor form (id is assigned on create). */
export type ProductDraft = Omit<SellerProduct, 'id' | 'sold' | 'createdAt'> & {
  id?: string | number
  /** Real File selected for upload. Only meaningful when creating a product —
   * the backend has no endpoint to change a product's image after creation. */
  imageFile?: File | null
}

// ── Analytics ──────────────────────────────────────────────────────────────

export interface KpiStat {
  id: string
  label: string
  value: string
  trend: string
  trendType: 'positive' | 'stable' | 'neutral'
  icon: string
  iconColor: string
}

export interface SeriesPoint {
  label: string
  value: number
}

export interface CategorySales {
  category: string
  units: number
  revenue: number
}
