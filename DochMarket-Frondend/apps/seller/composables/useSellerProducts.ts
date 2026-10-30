import type { SellerProduct, ProductDraft, ProductStatus } from '~/apps/seller/types'

// Seller catalogue store backed by the real API (no local-only state changes).
//   load      GET    /api/product/mine            (all own products, any moderation status)
//   add       POST   /api/product/productWithImage (multipart)
//   update    PUT    /api/product/{id}
//   setStatus PATCH  /api/product/{id}/active
//   remove    DELETE /api/product/{id}
export function useSellerProducts() {
  const { get, post, put, patch, del } = useApi()

  const products = useState<SellerProduct[]>('seller-products', () => [])
  const loading = useState<boolean>('seller-products-loading', () => false)
  const error = useState<string>('seller-products-error', () => '')
  const categories = useState<any[]>('seller-categories', () => [])

  const errText = (e: any, what: string) => e?.data?.message ?? `${what} (${e?.status ?? 'network error'})`

  function mapFromApi(p: any): SellerProduct {
    const stock = p.stockQuantity ?? 0
    const status: ProductStatus = p.active === false ? 'draft' : stock > 0 ? 'active' : 'out_of_stock'
    return {
      id: p.id,
      title: p.name ?? '',
      description: p.description ?? '',
      image: (p.images && p.images[0]) || '',
      price: p.price ?? 0,
      category: (p.categories && p.categories[0]?.name) || '',
      sku: p.SKU ?? '',
      stockCount: stock,
      sold: p.quantitySold ?? 0,
      status,
      createdAt: (p.createdAt ?? '').slice(0, 10),
      currency: p.currency ?? 'XAF',
      weightKg: p.weightKg ?? undefined,
      lengthCm: p.lengthCm ?? undefined,
      widthCm: p.widthCm ?? undefined,
      heightCm: p.heightCm ?? undefined,
      publicationStatus: p.publicationStatus,
      rejectionReason: p.rejectionReason ?? undefined,
      visibleToCustomers: !!p.visibleToCustomers,
    } as SellerProduct
  }

  async function resolveCategoryId(name?: string): Promise<string> {
    if (!name) return ''
    if (!categories.value.length) categories.value = (await get<any[]>('/api/category')) ?? []
    return categories.value.find((c: any) => (c.name ?? '').toLowerCase() === name.toLowerCase())?.id ?? ''
  }

  async function load() {
    loading.value = true
    error.value = ''
    try {
      const list = await get<any[]>('/api/product/mine')
      products.value = (list ?? []).map(mapFromApi)
    } catch (e: any) {
      error.value = errText(e, "Couldn't load products")
    } finally {
      loading.value = false
    }
  }

  const byId = (id: string | number) => products.value.find(p => String(p.id) === String(id))

  function bodyFrom(draft: Partial<ProductDraft>, existing: SellerProduct | undefined, categoryId: string) {
    return {
      productName: draft.title ?? existing?.title,
      weight: String(draft.weightKg ?? existing?.weightKg ?? ''),
      SKU: draft.sku ?? existing?.sku ?? '',
      description: draft.description ?? existing?.description ?? '',
      price: draft.price ?? existing?.price,
      currency: 'XAF',
      stockQuantity: draft.stockCount ?? existing?.stockCount ?? 0,
      categoryId,
      lengthCm: draft.lengthCm ?? existing?.lengthCm ?? null,
      widthCm: draft.widthCm ?? existing?.widthCm ?? null,
      heightCm: draft.heightCm ?? existing?.heightCm ?? null,
    }
  }

  async function add(draft: ProductDraft): Promise<{ ok: boolean; product?: SellerProduct; error?: string }> {
    error.value = ''
    try {
      const categoryId = await resolveCategoryId(draft.category)
      if (!categoryId) return { ok: false, error: `Category "${draft.category}" was not found.` }
      if (!draft.imageFile) return { ok: false, error: 'Please add a product photo.' }

      const body = bodyFrom(draft, undefined, categoryId)
      const form = new FormData()
      form.append('image', draft.imageFile, draft.imageFile.name)
      Object.entries(body).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') form.append(k, String(v)) })

      let created = mapFromApi(await post<any>('/api/product/productWithImage', form))
      if (draft.status === 'draft') {
        created = mapFromApi(await patch<any>(`/api/product/${created.id}/active`, { active: false }))
      }
      products.value = [created, ...products.value]
      return { ok: true, product: created }
    } catch (e: any) {
      const msg = errText(e, "Couldn't create product")
      error.value = msg
      return { ok: false, error: msg }
    }
  }

  async function update(id: string | number, patchData: Partial<ProductDraft>): Promise<{ ok: boolean; error?: string }> {
    error.value = ''
    try {
      const existing = byId(id)
      const categoryId = await resolveCategoryId(patchData.category ?? existing?.category)
      let updated = await put<any>(`/api/product/${id}`, bodyFrom(patchData, existing, categoryId))
      const wantActive = patchData.status ? patchData.status !== 'draft' : undefined
      if (wantActive !== undefined && wantActive !== updated.active) {
        updated = await patch<any>(`/api/product/${id}/active`, { active: wantActive })
      }
      const mapped = mapFromApi(updated)
      products.value = products.value.map(p => String(p.id) === String(id) ? mapped : p)
      return { ok: true }
    } catch (e: any) {
      const msg = errText(e, "Couldn't update product")
      error.value = msg
      return { ok: false, error: msg }
    }
  }

  async function setStatus(id: string | number, status: ProductStatus) {
    const updated = await patch<any>(`/api/product/${id}/active`, { active: status !== 'draft' })
    const mapped = mapFromApi(updated)
    products.value = products.value.map(p => String(p.id) === String(id) ? mapped : p)
  }

  async function setStock(id: string | number, stockCount: number) {
    return update(id, { stockCount: Math.max(0, Math.floor(stockCount || 0)) })
  }

  async function remove(id: string | number) {
    await del(`/api/product/${id}`)
    products.value = products.value.filter(p => String(p.id) !== String(id))
  }

  async function removeMany(ids: Array<string | number>) {
    for (const id of ids) await remove(id)
  }

  async function activateMany(ids: Array<string | number>) {
    for (const id of ids) await setStatus(id, 'active')
  }

  return {
    products, loading, error, load,
    byId, add, update, remove, removeMany, setStatus, setStock, activateMany,
  }
}