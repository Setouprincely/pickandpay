#!/usr/bin/env node
/**
 * DOCH End-to-End Workflow Validation (Staging Acceptance Test Plan, workflows A-H + surprise challenge).
 *
 * Runs against a REAL backend (database, object storage, Fapshi SANDBOX). Creates fresh test records only.
 *
 *   node acceptance/run-acceptance.mjs
 *
 * Environment:
 *   BASE_URL            backend URL                         (default http://localhost:8080)
 *   ADMIN_EMAIL         super admin e-mail                  (default superadmin@doch.com)
 *   ADMIN_PASSWORD      super admin password                (required)
 *   OTP_DB_CONTAINER    postgres container used as the "test inbox" for e-mail OTPs (default doch-postgres)
 *   OTP_DB_NAME         database name                       (default doch)
 *   OTP_DB_USER         database user                       (default postgres)
 *   PRODUCT_NAME        surprise-challenge product name     (default Gari)
 *   PRODUCT_PRICE       unit price in XAF                   (default 1500)
 *   PRODUCT_WEIGHT      weight per unit                     (default 2kg)
 *   ORDER_QUANTITY      quantity ordered                    (default 3)
 *   CUSTOMER_ADDRESS    delivery address                    (default "Bastos, Yaounde, Cameroon")
 *   SELLER_ADDRESS      seller warehouse address            (default "Marche Mokolo, Yaounde, Cameroon")
 *   PAY_SUCCESS_PHONE   Fapshi sandbox number that succeeds (default 670000000)
 *   PAY_FAIL_PHONE      Fapshi sandbox number that fails    (default 670000001)
 *
 * E-mail addresses use Resend's test domain (delivered+label@resend.dev) so real e-mails are sent
 * without bouncing. Verification codes are read from the database, which plays the role of the inbox.
 */
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const cfg = {
  baseUrl: process.env.BASE_URL || 'http://localhost:8080',
  adminEmail: process.env.ADMIN_EMAIL || 'superadmin@doch.com',
  adminPassword: process.env.ADMIN_PASSWORD,
  dbContainer: process.env.OTP_DB_CONTAINER || 'doch-postgres',
  dbName: process.env.OTP_DB_NAME || 'doch',
  dbUser: process.env.OTP_DB_USER || 'postgres',
  productName: process.env.PRODUCT_NAME || 'Gari',
  productPrice: Number(process.env.PRODUCT_PRICE || 1500),
  productWeight: process.env.PRODUCT_WEIGHT || '2kg',
  quantity: Number(process.env.ORDER_QUANTITY || 3),
  customerAddress: process.env.CUSTOMER_ADDRESS || 'Bastos, Yaounde, Cameroon',
  sellerAddress: process.env.SELLER_ADDRESS || 'Marche Mokolo, Yaounde, Cameroon',
  paySuccess: process.env.PAY_SUCCESS_PHONE || '670000000',
  payFail: process.env.PAY_FAIL_PHONE || '670000001',
}
if (!cfg.adminPassword) {
  console.error('ADMIN_PASSWORD is required')
  process.exit(2)
}

// 1x1 PNG used for product, sealed-package and proof-of-delivery photos.
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')
const RUN = Date.now().toString(36)
const report = { run: RUN, startedAt: new Date().toISOString(), config: { ...cfg, adminPassword: '***' }, steps: [], records: {} }
let failures = 0

// ------------------------------------------------------------------ helpers
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

async function api(method, path, { token, json, form } = {}) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let body
  if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json) }
  if (form) body = form
  const res = await fetch(cfg.baseUrl + path, { method, headers, body })
  const text = await res.text()
  let data = text
  try { data = text ? JSON.parse(text) : null } catch { /* plain text */ }
  return { status: res.status, data }
}

async function ok(method, path, opts) {
  const r = await api(method, path, opts)
  if (r.status >= 300) throw new Error(`${method} ${path} -> ${r.status} ${JSON.stringify(r.data)}`)
  return r.data
}

async function expectStatus(expected, method, path, opts) {
  const r = await api(method, path, opts)
  const list = Array.isArray(expected) ? expected : [expected]
  if (!list.includes(r.status)) throw new Error(`${method} ${path} expected ${list.join('/')} but got ${r.status} ${JSON.stringify(r.data)}`)
  return r
}

function check(cond, message) {
  if (!cond) throw new Error('Assertion failed: ' + message)
}

async function step(id, title, fn) {
  const started = Date.now()
  try {
    const evidence = await fn()
    report.steps.push({ id, title, result: 'PASS', ms: Date.now() - started, evidence })
    console.log(`PASS  ${id}  ${title}`)
  } catch (e) {
    failures++
    report.steps.push({ id, title, result: 'FAIL', ms: Date.now() - started, error: e.message })
    console.log(`FAIL  ${id}  ${title}\n      ${e.message}`)
  }
}

function sql(query) {
  return execFileSync('docker', ['exec', cfg.dbContainer, 'psql', '-U', cfg.dbUser, '-d', cfg.dbName, '-tAc', query],
    { encoding: 'utf8' }).trim()
}

function latestOtp(email) {
  const safe = email.replace(/'/g, "''")
  return sql(`select v.otp_code from verification_token v join users u on u.id = v.c_user_id
              where u.email = '${safe}' and v.is_used = false order by v.created_time desc limit 1`)
}

function imageForm(field = 'image') {
  const form = new FormData()
  form.append(field, new Blob([PNG], { type: 'image/png' }), 'photo.png')
  return form
}

async function login(email, password) {
  const r = await ok('POST', '/api/auth/login', { json: { email, password } })
  check(r.access_token, 'login returns an access token')
  return { accessToken: r.access_token, userId: r.user_id, role: r.role }
}

async function waitPayment(token, paymentId, expected, timeoutMs = 150000) {
  const deadline = Date.now() + timeoutMs
  let last
  while (Date.now() < deadline) {
    last = await ok('GET', `/api/payments/${paymentId}/status`, { token })
    if (last.status === expected) return last
    if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(last.status) && last.status !== expected) break
    await sleep(11000)
  }
  throw new Error(`payment ${paymentId} ended as ${last?.status} (providerStatus ${last?.providerStatus}), expected ${expected}`)
}

const ctx = {}
const pw = `Test#${RUN}Pw!`
const mail = (label) => `delivered+${label}-${RUN}@resend.dev`

// ------------------------------------------------------------------ Workflow A
await step('A.1', 'Administrator authenticates', async () => {
  ctx.admin = await login(cfg.adminEmail, cfg.adminPassword)
  check(ctx.admin.role === 'ROLE_SUPER_ADMIN', 'admin role')
  return { adminUserId: ctx.admin.userId }
})

async function registerAndVerify(kind, label, payload) {
  const email = mail(label)
  const path = kind === 'seller' ? '/api/auth/seller/register' : '/api/auth/register'
  const created = await ok('POST', path, { json: { email, password: pw, firstName: 'E2E', lastName: label, userName: `${label}${RUN}`, phone: '650000000', doB: '1990-01-01', ...payload } })
  const before = await api('POST', '/api/auth/login', { json: { email, password: pw } })
  check(before.status >= 400, `unverified ${label} cannot log in (got ${before.status})`)
  const otp = latestOtp(email)
  check(/^\d+$/.test(otp), `verification code issued for ${email}`)
  await ok('POST', '/api/auth/verify', { json: { email, token: otp } })
  const session = await login(email, pw)
  return { email, session, created }
}

await step('A.2', 'Customer account: create, verify, log in, access own profile', async () => {
  const c = await registerAndVerify('customer', 'customer', { role: 'ROLE_BUYER' })
  ctx.customer = c
  const me = await ok('GET', '/api/users/me', { token: c.session.accessToken })
  check(c.session.role === 'ROLE_BUYER', 'customer role is ROLE_BUYER')
  report.records.customer = { email: c.email, userId: c.session.userId }
  return { email: c.email, userId: c.session.userId, role: c.session.role, profileEmail: me.email }
})

await step('A.3', 'Second customer account (used for privacy checks)', async () => {
  ctx.customer2 = await registerAndVerify('customer', 'customer2', { role: 'ROLE_BUYER' })
  return { email: ctx.customer2.email, userId: ctx.customer2.session.userId }
})

await step('A.4', 'Seller account: register with store details, verify, log in', async () => {
  const s = await registerAndVerify('seller', 'seller', {
    storeName: `E2E ${cfg.productName} Store ${RUN}`, city: 'Yaounde', address: cfg.sellerAddress, contactNumber: '655000000',
  })
  ctx.seller = s
  const profiles = await ok('GET', `/api/sellers/byUserId?userId=${s.session.userId}`, { token: s.session.accessToken })
  ctx.sellerId = profiles[0].id
  check(profiles[0].isVerified === false, 'new seller starts unverified')
  report.records.seller = { email: s.email, userId: s.session.userId, sellerId: ctx.sellerId }
  return report.records.seller
})

await step('A.5', 'Administrator reviews and approves the seller', async () => {
  const all = await ok('GET', '/api/sellers/all', { token: ctx.admin.accessToken })
  check(all.some(s => s.id === ctx.sellerId && s.isVerified === false), 'seller listed as pending')
  await expectStatus(400, 'PUT', `/api/sellers/verify/${ctx.sellerId}?isVerified=false`, { token: ctx.admin.accessToken })
  const approved = await ok('PUT', `/api/sellers/verify/${ctx.sellerId}?isVerified=true`, { token: ctx.admin.accessToken })
  check(approved.isVerified === true && approved.verifiedAt, 'seller verified with timestamp')
  return { sellerId: ctx.sellerId, verifiedAt: approved.verifiedAt }
})

await step('A.6', 'Driver account: onboarded by admin, activated via invitation code', async () => {
  const email = mail('driver')
  await ok('POST', '/api/admin/onboard-driver', { token: ctx.admin.accessToken, json: {
    email, firstName: 'E2E', lastName: 'Driver', phone: '677000000', vehicleType: 'Suzuki Carry', plateNumber: `LT-${RUN}`.toUpperCase() } })
  const before = await api('POST', '/api/auth/login', { json: { email, password: pw } })
  check(before.status >= 400, 'driver cannot log in before activation')
  await ok('POST', '/api/auth/staff-setup', { json: { email, otpCode: latestOtp(email), newPassword: pw } })
  ctx.driver = { email, session: await login(email, pw) }
  check(ctx.driver.session.role === 'ROLE_DELIVERY', 'driver role')
  report.records.driver = { email, userId: ctx.driver.session.userId }
  return report.records.driver
})

await step('A.7', 'Role-based permissions are enforced by the backend', async () => {
  const s = ctx.seller.session.accessToken, d = ctx.driver.session.accessToken, c = ctx.customer.session.accessToken
  const results = {}
  results.sellerOrdersAll = (await expectStatus(403, 'GET', '/api/orders/all', { token: s })).status
  results.sellerAdminUsers = (await expectStatus(403, 'GET', '/api/admin/users', { token: s })).status
  results.sellerSettings = (await expectStatus(403, 'PUT', '/api/settings/pricing', { token: s, json: { minPrice: 1 } })).status
  results.driverOrdersAll = (await expectStatus(403, 'GET', '/api/orders/all', { token: d })).status
  results.driverRevenue = (await expectStatus(403, 'GET', '/api/stats/revenue', { token: d })).status
  results.driverPayouts = (await expectStatus(403, 'GET', '/api/admin/payouts/eligible', { token: d })).status
  results.driverAudit = (await expectStatus(403, 'GET', '/api/admin/audit', { token: d })).status
  results.customerAdminUsers = (await expectStatus(403, 'GET', '/api/admin/users', { token: c })).status
  results.customerCategory = (await expectStatus(403, 'POST', '/api/category/create', { token: c, json: { name: 'x' } })).status
  results.customerAssign = (await expectStatus(403, 'POST', '/api/deliveries/x/assign', { token: c, json: { driverId: 'x', vehicleId: 'x' } })).status
  results.anonymousOrders = (await expectStatus(401, 'GET', '/api/orders/me')).status
  return results
})

await step('A.8', 'Account information is consistent after logout and login', async () => {
  const token = ctx.customer.session.accessToken
  await expectStatus([200, 204], 'POST', '/api/auth/logout', { token })
  await expectStatus(401, 'GET', '/api/users/me', { token })
  ctx.customer.session = await login(ctx.customer.email, pw)
  const me = await ok('GET', '/api/users/me', { token: ctx.customer.session.accessToken })
  check(ctx.customer.session.userId === report.records.customer.userId, 'same user id after re-login')
  return { userId: ctx.customer.session.userId, email: me.email, revokedTokenRejected: true }
})

// ------------------------------------------------------------------ Workflow B
await step('B.1', `Seller creates "${cfg.productName}" with photo; listing awaits moderation`, async () => {
  const cats = await ok('GET', '/api/category')
  let food = cats.find(c => c.name === 'Food')
  if (!food) {
    const id = await ok('POST', '/api/category/create', { token: ctx.admin.accessToken, json: { name: 'Food', description: 'Food and groceries' } })
    food = { id: typeof id === 'string' ? id.replace(/"/g, '') : id }
  }
  ctx.categoryId = food.id
  const form = imageForm()
  form.append('productName', cfg.productName)
  form.append('description', `${cfg.productName} - acceptance run ${RUN}`)
  form.append('price', String(cfg.productPrice))
  form.append('currency', 'XAF')
  form.append('stockQuantity', '50')
  form.append('weight', cfg.productWeight)
  form.append('SKU', `E2E-${RUN}`)
  form.append('categoryId', ctx.categoryId)
  const p = await ok('POST', '/api/product/productWithImage', { token: ctx.seller.session.accessToken, form })
  ctx.product = p
  check(p.sellerId === ctx.sellerId, 'product linked to seller')
  check(p.publicationStatus === 'PENDING_REVIEW' && p.visibleToCustomers === false, 'pending review, not visible')
  check(p.images?.length === 1, 'image stored')
  report.records.product = { productId: p.id, name: p.name, price: p.price, weightKg: p.weightKg }
  return { productId: p.id, publicationStatus: p.publicationStatus, weightKg: p.weightKg, imageStored: true }
})

await step('B.2', 'Unapproved product is invisible to customers', async () => {
  const list = await ok('GET', `/api/product?q=${encodeURIComponent(cfg.productName)}`)
  check(!list.some(p => p.id === ctx.product.id), 'not in catalogue')
  await expectStatus(404, 'GET', `/api/product/${ctx.product.id}`)
  await expectStatus(400, 'POST', '/api/orders/preview', { token: ctx.customer.session.accessToken,
    json: { items: [{ productId: ctx.product.id, quantity: 1 }], deliveryAddress: cfg.customerAddress, transportType: 'SINGLE_PARCEL' } })
  return { inCatalogue: false, productPage: 404, purchasable: false }
})

await step('B.3', 'Administrator approves the listing', async () => {
  const queue = await ok('GET', '/api/product/admin?status=PENDING_REVIEW&size=100', { token: ctx.admin.accessToken })
  check(queue.content.some(p => p.id === ctx.product.id), 'listing in moderation queue')
  const approved = await ok('PUT', `/api/product/${ctx.product.id}/approve`, { token: ctx.admin.accessToken })
  check(approved.publicationStatus === 'PUBLISHED' && approved.visibleToCustomers, 'published and visible')
  return { publicationStatus: approved.publicationStatus }
})

await step('B.4', 'Customer finds the product; product page shows accurate data', async () => {
  const list = await ok('GET', `/api/product?q=${encodeURIComponent(cfg.productName)}`, { token: ctx.customer.session.accessToken })
  check(list.some(p => p.id === ctx.product.id), 'found by search')
  const page = await ok('GET', `/api/product/${ctx.product.id}`)
  check(page.price === cfg.productPrice, 'price')
  check(page.sellerId === ctx.sellerId, 'seller')
  check(page.categories?.[0]?.id === ctx.categoryId, 'category')
  check(page.stockQuantity === 50, 'availability')
  return { price: page.price, seller: page.sellerName, category: page.categories[0].name, stock: page.stockQuantity }
})

await step('B.5', 'Editing and deactivating the listing updates the customer interface', async () => {
  const t = ctx.seller.session.accessToken
  const off = await ok('PATCH', `/api/product/${ctx.product.id}/active`, { token: t, json: { active: false } })
  check(off.visibleToCustomers === false, 'deactivated')
  let list = await ok('GET', `/api/product?q=${encodeURIComponent(cfg.productName)}`)
  check(!list.some(p => p.id === ctx.product.id), 'hidden when inactive')
  await ok('PATCH', `/api/product/${ctx.product.id}/active`, { token: t, json: { active: true } })
  const desc = `${cfg.productName} (updated description ${RUN})`
  await ok('PUT', `/api/product/${ctx.product.id}`, { token: t, json: {
    productName: cfg.productName, weight: cfg.productWeight, description: desc, price: cfg.productPrice, currency: 'XAF',
    stockQuantity: 50, categoryId: ctx.categoryId, SKU: `E2E-${RUN}` } })
  const page = await ok('GET', `/api/product/${ctx.product.id}`)
  check(page.description === desc, 'edit visible to customers')
  await expectStatus(403, 'PUT', `/api/product/${ctx.product.id}`, { token: ctx.customer.session.accessToken, json: {
    productName: 'hijack', weight: '1', price: 1, stockQuantity: 1, categoryId: ctx.categoryId } })
  list = await ok('GET', `/api/product?q=${encodeURIComponent(cfg.productName)}`)
  check(list.some(p => p.id === ctx.product.id), 'visible again')
  return { deactivatedHidden: true, editReflected: true, otherUserEditBlocked: 403 }
})

// ------------------------------------------------------------------ Workflow C
async function placeOrder(token, quantity, phone) {
  return ok('POST', '/api/orders', { token, json: {
    items: [{ productId: ctx.product.id, quantity }], deliveryAddress: cfg.customerAddress, contactPhone: '650000000',
    paymentMethod: 'MTN_MOMO', paymentPhone: phone, transportType: 'SINGLE_PARCEL' } })
}

await step('C.1', `Customer previews and places an order (qty ${cfg.quantity}) and pays in the Fapshi sandbox`, async () => {
  const t = ctx.customer.session.accessToken
  const preview = await ok('POST', '/api/orders/preview', { token: t, json: {
    items: [{ productId: ctx.product.id, quantity: cfg.quantity }], deliveryAddress: cfg.customerAddress, transportType: 'SINGLE_PARCEL' } })
  const order = await placeOrder(t, cfg.quantity, cfg.paySuccess)
  ctx.order = order
  check(order.reference?.startsWith('DO-'), 'order reference')
  check(order.paymentDetails?.paymentId, 'payment initiated')
  check(order.totalAmount === preview.totalAmount, 'order total equals preview')
  const paid = await waitPayment(t, order.paymentDetails.paymentId, 'COMPLETED')
  report.records.order = { orderId: order.orderId, reference: order.reference, paymentId: paid.paymentId,
    paymentReference: paid.providerReference, totalAmount: order.totalAmount }
  return { ...report.records.order, productTotal: order.productTotal, deliveryFee: order.deliveryFee, paymentStatus: paid.status }
})

await step('C.2', 'Order record is correct and linked to customer, seller, product, payment and package', async () => {
  const o = await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.customer.session.accessToken })
  check(o.status === 'PROCESSING', `order PROCESSING (got ${o.status})`)
  check(o.customerId === report.records.customer.userId, 'customer link')
  check(o.items.length === 1 && o.items[0].productId === ctx.product.id && o.items[0].quantity === cfg.quantity, 'item/quantity')
  check(o.items[0].unite_price === cfg.productPrice && o.productTotal === cfg.productPrice * cfg.quantity, 'price')
  check(o.items[0].sellerId === ctx.sellerId, 'seller link')
  check(o.paymentDetails.status === 'COMPLETED' && o.paymentDetails.amount === o.totalAmount, 'payment amount equals order total')
  check(o.deliveryAddress === cfg.customerAddress, 'delivery details')
  const leg = o.deliveryLegs[0]
  check(leg.status === 'PENDING_PREPARATION' && leg.trackingNumber?.startsWith('DCH-') && leg.deliveryOtp, 'package created with tracking + OTP')
  ctx.leg = leg
  const product = await ok('GET', `/api/product/${ctx.product.id}`)
  check(product.stockQuantity === 50 - cfg.quantity, 'stock reserved')
  report.records.order.trackingNumber = leg.trackingNumber
  report.records.order.deliveryId = leg.deliveryId
  return { status: o.status, paidAt: o.paidAt, trackingNumber: leg.trackingNumber, packageStatus: leg.status,
    requiredVehicle: leg.requiredVehicleCategory, packageWeightKg: leg.packageWeightKg, stockAfter: product.stockQuantity }
})

await step('C.3', 'Failed payment is not marked successful; order offers retry', async () => {
  const t = ctx.customer.session.accessToken
  const order = await placeOrder(t, 1, cfg.payFail)
  ctx.order2 = order
  const failed = await waitPayment(t, order.paymentDetails.paymentId, 'FAILED')
  const o = await ok('GET', `/api/orders/${order.orderId}`, { token: t })
  check(o.status === 'PAYMENT_FAILED' && o.canPay === true, 'order PAYMENT_FAILED and payable')
  check(o.deliveryLegs[0].status === 'PENDING_PAYMENT', 'no fulfilment for unpaid order')
  report.records.failedPayment = { orderId: order.orderId, reference: order.reference, paymentId: failed.paymentId }
  return { reference: order.reference, paymentStatus: failed.status, providerStatus: failed.providerStatus, orderStatus: o.status }
})

await step('C.4', 'Forged "SUCCESSFUL" webhook cannot mark a failed payment as paid', async () => {
  const pid = report.records.failedPayment.paymentId
  await ok('POST', '/api/payments/webhook/fapshi', { json: { externalId: pid, status: 'SUCCESSFUL', transId: 'forged' } })
  await ok('POST', '/api/payments/webhook/fapshi', { json: { externalId: pid, status: 'SUCCESSFUL' } })
  const st = await ok('GET', `/api/payments/${pid}/status`, { token: ctx.customer.session.accessToken })
  check(st.status === 'FAILED', 'still FAILED')
  const o = await ok('GET', `/api/orders/${ctx.order2.orderId}`, { token: ctx.customer.session.accessToken })
  check(o.status === 'PAYMENT_FAILED', 'order not paid')
  return { paymentStatus: st.status, orderStatus: o.status }
})

await step('C.5', 'Customer retries the failed order payment successfully', async () => {
  const t = ctx.customer.session.accessToken
  const retry = await ok('POST', '/api/payments/initiate', { token: t, json: { orderId: ctx.order2.orderId, paymentMethod: 'MTN_MOMO', phone: cfg.paySuccess, amount: 1 } })
  check(retry.amount === ctx.order2.totalAmount, 'server-side amount (client amount ignored)')
  const paid = await waitPayment(t, retry.paymentId, 'COMPLETED')
  const o = await ok('GET', `/api/orders/${ctx.order2.orderId}`, { token: t })
  check(o.status === 'PROCESSING' && o.paymentDetails.paymentId === retry.paymentId, 'order paid by retry attempt')
  ctx.leg2 = o.deliveryLegs[0]
  report.records.retriedOrder = { orderId: o.orderId, reference: o.reference, paymentId: paid.paymentId, trackingNumber: ctx.leg2.trackingNumber }
  return report.records.retriedOrder
})

await step('C.6', 'Duplicate provider callbacks do not duplicate charges or fulfilment', async () => {
  const pid = report.records.order.paymentId
  const before = sql(`select count(*) from payment_splits where payment_id = '${pid}'`)
  const legsBefore = sql(`select count(*) from delivery d join doch_market_delivery_request m on m.id = d.market_request_id where m.order_id = '${ctx.order.orderId}'`)
  for (let i = 0; i < 3; i++) {
    await ok('POST', '/api/payments/webhook/fapshi', { json: { externalId: pid, status: 'SUCCESSFUL', transId: report.records.order.paymentReference } })
  }
  const after = sql(`select count(*) from payment_splits where payment_id = '${pid}'`)
  const legsAfter = sql(`select count(*) from delivery d join doch_market_delivery_request m on m.id = d.market_request_id where m.order_id = '${ctx.order.orderId}'`)
  const credits = sql(`select count(*) from wallet_transactions where description like '%${ctx.order.reference}%' and type = 'CREDIT_PENDING'`)
  check(before === after && legsBefore === legsAfter && credits === '1', 'no duplicates')
  return { paymentSplits: Number(after), packages: Number(legsAfter), sellerPendingCredits: Number(credits) }
})

await step('C.7', 'Abandoned unpaid order can be cancelled; reserved stock is released', async () => {
  const t = ctx.customer.session.accessToken
  const stockBefore = (await ok('GET', `/api/product/${ctx.product.id}`)).stockQuantity
  const order = await placeOrder(t, 2, cfg.payFail)
  await waitPayment(t, order.paymentDetails.paymentId, 'FAILED')
  const reserved = (await ok('GET', `/api/product/${ctx.product.id}`)).stockQuantity
  check(reserved === stockBefore - 2, 'stock reserved while unpaid')
  await expectStatus(403, 'POST', `/api/orders/${order.orderId}/cancel`, { token: ctx.customer2.session.accessToken, json: {} })
  const cancelled = await ok('POST', `/api/orders/${order.orderId}/cancel`, { token: t, json: { reason: 'Changed my mind' } })
  check(cancelled.status === 'CANCELLED' && cancelled.deliveryLegs[0].status === 'CANCELLED', 'order and package cancelled')
  const after = (await ok('GET', `/api/product/${ctx.product.id}`)).stockQuantity
  check(after === stockBefore, 'stock released')
  report.records.cancelledOrder = { orderId: order.orderId, reference: order.reference }
  return { reference: order.reference, stockBefore, reserved, stockAfter: after }
})

// ------------------------------------------------------------------ Workflow D
await step('D.1', 'Order is discoverable by ID, reference and tracking number in admin management', async () => {
  const t = ctx.admin.accessToken
  const byRef = await ok('GET', `/api/orders/all?q=${ctx.order.reference}`, { token: t })
  const byTracking = await ok('GET', `/api/orders/all?q=${ctx.leg.trackingNumber}`, { token: t })
  const byId = await ok('GET', `/api/orders/all?q=${ctx.order.orderId}`, { token: t })
  const byStatus = await ok('GET', `/api/orders/all?status=PROCESSING&size=100`, { token: t })
  const paged = await ok('GET', `/api/orders/all?size=1&page=0`, { token: t })
  check(byRef.totalElements === 1 && byTracking.totalElements === 1 && byId.totalElements === 1, 'search finds exactly the order')
  check(byStatus.content.some(o => o.orderId === ctx.order.orderId), 'status filter')
  check(paged.content.length === 1 && paged.totalElements >= 3, 'pagination')
  const detail = await ok('GET', `/api/orders/${ctx.order.reference}`, { token: t })
  check(detail.customerEmail === ctx.customer.email && detail.paymentDetails.status === 'COMPLETED', 'admin detail')
  return { searchByReference: 1, searchByTracking: 1, searchById: 1, totalOrders: paged.totalElements }
})

await step('D.2', 'Seller sees the paid order (own items only, no payment data)', async () => {
  const list = await ok('GET', '/api/orders/seller/me', { token: ctx.seller.session.accessToken })
  const o = list.find(x => x.orderId === ctx.order.orderId)
  check(o, 'order visible to seller')
  check(!o.paymentDetails && o.deliveryFee == null, 'payment and logistics fees hidden')
  check(o.sellerTrackingNumber === ctx.leg.trackingNumber && o.sellerDeliveryStatus === 'PENDING_PREPARATION', 'fulfilment request visible')
  check(!list.some(x => x.orderId === report.records.cancelledOrder.orderId), 'unpaid/cancelled orders not pushed to seller')
  return { reference: o.reference, packageStatus: o.sellerDeliveryStatus, items: o.items.length }
})

await step('D.3', 'Logistics dispatch queue shows the package at the correct stage', async () => {
  const page = await ok('GET', `/api/deliveries/all?q=${ctx.order.reference}`, { token: ctx.admin.accessToken })
  const pkg = page.content[0]
  check(pkg && pkg.trackingNumber === ctx.leg.trackingNumber && pkg.status === 'PENDING_PREPARATION', 'package in queue')
  check(!pkg.deliveryOtp, 'OTP never exposed to staff')
  return { trackingNumber: pkg.trackingNumber, status: pkg.status, orderReference: pkg.orderReference }
})

await step('D.4', 'Unauthorized users cannot view the order', async () => {
  const other = await expectStatus(403, 'GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.customer2.session.accessToken })
  const driver = await expectStatus(403, 'GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.driver.session.accessToken })
  const anon = await expectStatus(401, 'GET', `/api/orders/${ctx.order.orderId}`)
  return { otherCustomer: other.status, driver: driver.status, anonymous: anon.status }
})

// ------------------------------------------------------------------ Workflow E
await step('E.1', 'Seller seals the package (photo) -> ready for dispatch', async () => {
  const d = await ok('POST', `/api/deliveries/market/${ctx.leg.deliveryId}/ready`, { token: ctx.seller.session.accessToken, form: imageForm() })
  check(d.status === 'PENDING_ASSIGNMENT', 'ready for assignment')
  return { status: d.status }
})

await step('E.2', 'Fleet: bicycle and Suzuki Carry registered; driver online', async () => {
  const t = ctx.admin.accessToken
  ctx.bike = await ok('POST', '/api/fleet', { token: t, json: { name: `Bike ${RUN}`, vehicleType: 'Bicycle', category: 'BICYCLE', plateNumber: `BK-${RUN}`.toUpperCase(), maxLoadKg: 5 } })
  ctx.truck = await ok('POST', '/api/fleet', { token: t, json: { name: `Carry ${RUN}`, vehicleType: 'Suzuki Carry', category: 'K_TRUCK', plateNumber: `KT-${RUN}`.toUpperCase(), maxLoadKg: 350 } })
  await ok('PATCH', '/api/drivers/me/availability', { token: ctx.driver.session.accessToken, json: { isAvailable: true } })
  report.records.vehicles = { bicycle: ctx.bike.id, kTruck: ctx.truck.id }
  return report.records.vehicles
})

await step('E.3', 'Vehicle decision matrix enforced; driver + vehicle assigned', async () => {
  const t = ctx.admin.accessToken
  const pkg = await ok('GET', `/api/deliveries/${ctx.leg.deliveryId}`, { token: t })
  const expected = pkg.packageWeightKg <= 5 ? 'BICYCLE' : pkg.packageWeightKg <= 350 ? 'K_TRUCK' : 'VAN'
  check(pkg.requiredVehicleCategory === expected, `required ${expected} for ${pkg.packageWeightKg} kg`)
  let rejectedBike = 'n/a'
  if (expected !== 'BICYCLE') {
    rejectedBike = (await expectStatus(400, 'POST', `/api/deliveries/${ctx.leg.deliveryId}/assign`, { token: t, json: { driverId: ctx.driver.session.userId, vehicleId: ctx.bike.id } })).status
  }
  const vehicleId = expected === 'BICYCLE' ? ctx.bike.id : ctx.truck.id
  const assigned = await ok('POST', `/api/deliveries/${ctx.leg.deliveryId}/assign`, { token: t, json: { driverId: ctx.driver.session.userId, vehicleId } })
  check(assigned.status === 'DRIVER_ASSIGNED' && assigned.driverId === ctx.driver.session.userId && assigned.vehicle.id === vehicleId, 'assignment')
  ctx.vehicleId = vehicleId
  return { packageWeightKg: pkg.packageWeightKg, requiredVehicle: pkg.requiredVehicleCategory, bicycleRejected: rejectedBike,
    assignedVehicle: assigned.vehicle.name, driver: assigned.driverName }
})

await step('E.4', 'Driver app receives the job with stops, vehicle and route', async () => {
  const jobs = await ok('GET', '/api/drivers/me/deliveries', { token: ctx.driver.session.accessToken })
  const job = jobs.find(j => j.deliveryId === ctx.leg.deliveryId)
  check(job && job.vehicle?.id === ctx.vehicleId && job.pickupLocationAddress && job.receiverAddress, 'job details')
  check(!job.deliveryOtp, 'driver never sees the OTP')
  const route = await ok('GET', '/api/deliveries/me/optimized-route', { token: ctx.driver.session.accessToken })
  const stops = route.filter(s => s.deliveryId === ctx.leg.deliveryId)
  check(stops.length === 2 && stops[0].stopType === 'PICKUP' && stops[1].stopType === 'DROP_OFF' && stops[0].sequence < stops[1].sequence, 'pickup before drop-off')
  const profile = await ok('GET', '/api/drivers/me/profile', { token: ctx.driver.session.accessToken })
  return { jobs: jobs.length, stops: route.map(s => `${s.sequence}:${s.stopType}:${s.trackingNumber}`), driverVehicle: profile.vehicleName }
})

await step('E.5', 'Assignment visible consistently to dispatcher, admin, customer and seller', async () => {
  const admin = await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.admin.accessToken })
  const cust = await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.customer.session.accessToken })
  const seller = await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.seller.session.accessToken })
  for (const v of [admin, cust, seller]) {
    check(v.deliveryLegs[0].status === 'DRIVER_ASSIGNED' && v.deliveryLegs[0].driverName === 'E2E Driver', 'same assignment everywhere')
  }
  return { admin: admin.deliveryLegs[0].vehicleName, customer: cust.deliveryLegs[0].driverName, seller: seller.deliveryLegs[0].status }
})

// ------------------------------------------------------------------ Workflow F
await step('F.1', 'Pickup requires the correct scan; status follows the permitted sequence', async () => {
  const t = ctx.driver.session.accessToken, id = ctx.leg.deliveryId
  const early = await expectStatus(400, 'POST', `/api/deliveries/${id}/complete`, { token: t, form: (() => { const f = imageForm(); f.append('otp', '123456'); return f })() })
  const wrong = await expectStatus(400, 'POST', `/api/deliveries/${id}/pickup`, { token: t, json: { trackingCode: 'DCH-WRONGCODE' } })
  const other = await expectStatus(403, 'POST', `/api/deliveries/${id}/pickup`, { token: ctx.customer.session.accessToken, json: { trackingCode: ctx.leg.trackingNumber } })
  const picked = await ok('POST', `/api/deliveries/${id}/pickup`, { token: t, json: { trackingCode: ctx.leg.trackingNumber, latitude: '3.8721', longitude: '11.5148' } })
  check(picked.status === 'PICKED_UP' && picked.pickedUpAt, 'picked up')
  const o = await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.customer.session.accessToken })
  check(o.status === 'SHIPPED', 'order SHIPPED')
  return { completeBeforePickup: early.status, wrongScan: wrong.status, nonDriver: other.status, status: picked.status, orderStatus: o.status }
})

await step('F.2', 'Hub processing (scan, inspect, sort) and onward trip assignment', async () => {
  const t = ctx.admin.accessToken, id = ctx.leg.deliveryId
  const hub = await ok('POST', `/api/deliveries/${id}/hub-scan`, { token: t, json: { trackingCode: ctx.leg.trackingNumber, hubName: 'Yaounde Central Hub', note: 'Inspected, sealed, sorted' } })
  check(hub.status === 'AT_HUB' && hub.currentHub === 'Yaounde Central Hub', 'at hub')
  const onward = await ok('POST', `/api/deliveries/${id}/assign`, { token: t, json: { driverId: ctx.driver.session.userId, vehicleId: ctx.vehicleId, note: 'Last-mile leg' } })
  check(onward.status === 'DRIVER_ASSIGNED', 'onward trip assigned')
  const picked = await ok('POST', `/api/deliveries/${id}/pickup`, { token: ctx.driver.session.accessToken, json: { trackingCode: ctx.leg.trackingNumber } })
  check(picked.status === 'PICKED_UP', 'collected from hub')
  return { hub: hub.currentHub, onwardStatus: onward.status, afterHubPickup: picked.status }
})

await step('F.3', 'Out for delivery; invalid OTP cannot confirm delivery', async () => {
  const t = ctx.driver.session.accessToken, id = ctx.leg.deliveryId
  const transit = await ok('POST', `/api/deliveries/${id}/in-transit`, { token: t, json: {} })
  check(transit.status === 'IN_TRANSIT', 'in transit')
  const form = imageForm(); form.append('otp', '000000')
  const bad = await expectStatus(400, 'POST', `/api/deliveries/${id}/complete`, { token: t, form })
  const d = await ok('GET', `/api/deliveries/${id}`, { token: ctx.admin.accessToken })
  check(d.status === 'IN_TRANSIT' && d.otpFailedAttempts === 1, 'attempt recorded, not delivered')
  return { status: d.status, invalidOtp: bad.status, failedAttempts: d.otpFailedAttempts }
})

await step('F.4', 'Recipient gives OTP; driver records delivery with proof photo', async () => {
  const mine = await ok('GET', '/api/orders/me', { token: ctx.customer.session.accessToken })
  const otp = mine.find(o => o.orderId === ctx.order.orderId).deliveryLegs[0].deliveryOtp
  check(/^\d{6}$/.test(otp), 'customer can read OTP')
  const form = imageForm(); form.append('otp', otp); form.append('latitude', '3.8850'); form.append('longitude', '11.5160')
  const done = await ok('POST', `/api/deliveries/${ctx.leg.deliveryId}/complete`, { token: ctx.driver.session.accessToken, form })
  check(done.status === 'DELIVERED' && done.deliveredAt, 'delivered')
  const staff = await ok('GET', `/api/deliveries/${ctx.leg.deliveryId}`, { token: ctx.admin.accessToken })
  check(staff.proofOfDeliveryUrl && staff.driverId === ctx.driver.session.userId && staff.deliveredAt, 'time, driver and proof recorded')
  report.records.order.deliveredAt = staff.deliveredAt
  return { status: done.status, deliveredAt: staff.deliveredAt, driver: staff.driverName, proofStored: true }
})

await step('F.5', 'Complete package journey recorded without gaps, duplicates or contradictions', async () => {
  const d = await ok('GET', `/api/deliveries/${ctx.leg.deliveryId}`, { token: ctx.admin.accessToken })
  const statuses = d.events.filter(e => e.toStatus && (e.fromStatus !== e.toStatus || e.eventType === 'CREATED')).map(e => e.toStatus)
  const expected = ['PENDING_PAYMENT', 'PENDING_PREPARATION', 'PENDING_ASSIGNMENT', 'DRIVER_ASSIGNED', 'PICKED_UP', 'AT_HUB',
    'DRIVER_ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED']
  check(JSON.stringify(statuses) === JSON.stringify(expected), `sequence ${statuses.join(' > ')}`)
  const pub = await ok('GET', `/api/tracking/${ctx.leg.trackingNumber}`)
  const pubText = JSON.stringify(pub)
  check(pub.status === 'DELIVERED' && !pubText.includes(ctx.customer.email) && !pubText.includes('deliveryOtp') && !pubText.includes(cfg.customerAddress), 'public tracking without personal data')
  return { sequence: statuses.join(' > '), events: d.events.length, publicTrackingStatus: pub.status }
})

await step('F.6', 'Customer, seller, admin and logistics all see the final status', async () => {
  const views = {
    customer: (await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.customer.session.accessToken })),
    seller: (await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.seller.session.accessToken })),
    admin: (await ok('GET', `/api/orders/${ctx.order.orderId}`, { token: ctx.admin.accessToken })),
  }
  for (const [who, v] of Object.entries(views)) {
    check(v.status === 'DELIVERED' && v.deliveryLegs[0].status === 'DELIVERED', `${who} sees DELIVERED`)
  }
  const queue = await ok('GET', `/api/deliveries/all?q=${ctx.leg.trackingNumber}`, { token: ctx.admin.accessToken })
  check(queue.content[0].status === 'DELIVERED', 'logistics sees DELIVERED')
  const history = await ok('GET', '/api/drivers/me/history', { token: ctx.driver.session.accessToken })
  check(history.some(h => h.deliveryId === ctx.leg.deliveryId), 'driver history')
  const wallet = await ok('GET', '/api/wallets/me', { token: ctx.seller.session.accessToken })
  return { customer: views.customer.status, seller: views.seller.status, admin: views.admin.status, logistics: queue.content[0].status,
    sellerWallet: { available: wallet.availableBalance, pending: wallet.pendingBalance } }
})

// ------------------------------------------------------------------ Workflow G
await step('G.1', 'Customer notifications reference the order and tracking ID', async () => {
  const notes = await ok('GET', '/api/notifications', { token: ctx.customer.session.accessToken })
  const text = JSON.stringify(notes)
  check(text.includes(ctx.order.reference) && text.includes(ctx.leg.trackingNumber), 'order reference and tracking in notifications')
  const other = JSON.stringify(await ok('GET', '/api/notifications', { token: ctx.customer2.session.accessToken }))
  check(!other.includes(ctx.order.reference), 'no exposure to other customers')
  return { notifications: Array.isArray(notes) ? notes.length : 'n/a', emails: 'sent after commit via Resend (see server log "EMAIL:")' }
})

await step('G.2', 'Customer rates the delivered order; review linked to order and customer', async () => {
  const t = ctx.customer.session.accessToken
  const reviewed = await ok('POST', `/api/orders/${ctx.order.orderId}/review`, { token: t, json: { rating: 5, deliveryRating: 4, comment: 'Fresh Gari, fast delivery' } })
  check(reviewed.review?.rating === 5 && reviewed.canReview === false, 'review stored')
  const dup = await expectStatus(400, 'POST', `/api/orders/${ctx.order.orderId}/review`, { token: t, json: { rating: 1 } })
  const notOwner = await expectStatus(403, 'POST', `/api/orders/${ctx.order.orderId}/review`, { token: ctx.customer2.session.accessToken, json: { rating: 1 } })
  const notDelivered = await expectStatus(400, 'POST', `/api/orders/${ctx.order2.orderId}/review`, { token: t, json: { rating: 3 } })
  const driver = await ok('GET', `/api/drivers/${ctx.driver.session.userId}/profile`, { token: ctx.admin.accessToken })
  check(driver.rating === 4, 'driver rating computed from review')
  const linked = sql(`select count(*) from order_review where order_id = '${ctx.order.orderId}' and customer_id = '${report.records.customer.userId}'`)
  check(linked === '1', 'linked in database')
  return { rating: 5, deliveryRating: 4, duplicateReview: dup.status, otherCustomer: notOwner.status, beforeDelivery: notDelivered.status, driverRating: driver.rating }
})

await step('G.3', 'Order appears in history and reporting', async () => {
  const mine = await ok('GET', '/api/orders/me', { token: ctx.customer.session.accessToken })
  check(mine.some(o => o.orderId === ctx.order.orderId && o.status === 'DELIVERED'), 'customer history')
  const delivered = await ok('GET', '/api/orders/all?status=DELIVERED&size=100', { token: ctx.admin.accessToken })
  check(delivered.content.some(o => o.orderId === ctx.order.orderId), 'admin reporting filter')
  const stats = await ok('GET', '/api/stats/dashboard', { token: ctx.admin.accessToken })
  return { historyOrders: mine.length, deliveredOrders: delivered.totalElements, dashboard: stats }
})

// ------------------------------------------------------------------ Workflow H
const id2 = () => ctx.leg2.deliveryId
await step('H.1', 'Package not collected -> back to dispatch; driver unavailable -> unassign & reassign', async () => {
  const a = ctx.admin.accessToken, d = ctx.driver.session.accessToken
  await ok('POST', `/api/deliveries/market/${id2()}/ready`, { token: ctx.seller.session.accessToken, form: imageForm() })
  const pkg = await ok('GET', `/api/deliveries/${id2()}`, { token: a })
  const vehicleId = pkg.requiredVehicleCategory === 'BICYCLE' ? ctx.bike.id : ctx.truck.id
  await ok('POST', `/api/deliveries/${id2()}/assign`, { token: a, json: { driverId: ctx.driver.session.userId, vehicleId } })
  const nc = await ok('POST', `/api/deliveries/${id2()}/not-collected`, { token: d, json: { reason: 'PACKAGE_NOT_READY', note: 'Seller closed' } })
  check(nc.status === 'PENDING_ASSIGNMENT', 'not collected -> queue')
  await ok('POST', `/api/deliveries/${id2()}/assign`, { token: a, json: { driverId: ctx.driver.session.userId, vehicleId } })
  const un = await ok('POST', `/api/deliveries/${id2()}/unassign`, { token: a, json: { reason: 'DRIVER_UNAVAILABLE' } })
  check(un.status === 'PENDING_ASSIGNMENT' && !un.driverId, 'unassigned')
  const re = await ok('POST', `/api/deliveries/${id2()}/assign`, { token: a, json: { driverId: ctx.driver.session.userId, vehicleId } })
  check(re.status === 'DRIVER_ASSIGNED', 'reassigned')
  ctx.vehicle2 = vehicleId
  return { requiredVehicle: pkg.requiredVehicleCategory, notCollected: nc.status, unassigned: un.status, reassigned: re.status }
})

await step('H.2', 'Recipient unavailable -> failed attempt -> rescheduled re-attempt', async () => {
  const d = ctx.driver.session.accessToken
  await ok('POST', `/api/deliveries/${id2()}/pickup`, { token: d, json: { trackingCode: ctx.leg2.trackingNumber } })
  await ok('POST', `/api/deliveries/${id2()}/in-transit`, { token: d, json: {} })
  const failed = await ok('POST', `/api/deliveries/${id2()}/fail`, { token: d, json: { reason: 'RECIPIENT_UNAVAILABLE', note: 'No answer at gate' } })
  check(failed.status === 'FAILED' && failed.failureReason.includes('Recipient unavailable'), 'failed with reason')
  const order = await ok('GET', `/api/orders/${ctx.order2.orderId}`, { token: ctx.customer.session.accessToken })
  check(order.status === 'SHIPPED', 'order unchanged by recoverable failure')
  const retry = await ok('POST', `/api/deliveries/${id2()}/in-transit`, { token: d, json: { note: 'Second attempt' } })
  check(retry.status === 'IN_TRANSIT', 're-attempt')
  return { failed: failed.status, reason: failed.failureReason, reattempt: retry.status }
})

await step('H.3', 'Incorrect OTP lockout and supervised reset', async () => {
  const d = ctx.driver.session.accessToken
  let last
  for (let i = 0; i < 5; i++) {
    const f = imageForm(); f.append('otp', '999999')
    last = await expectStatus(400, 'POST', `/api/deliveries/${id2()}/complete`, { token: d, form: f })
  }
  check(/locked/i.test(JSON.stringify(last.data)), 'locked after 5 attempts')
  const mine = await ok('GET', '/api/orders/me', { token: ctx.customer.session.accessToken })
  const realOtp = mine.find(o => o.orderId === ctx.order2.orderId).deliveryLegs[0].deliveryOtp
  const f = imageForm(); f.append('otp', realOtp)
  await expectStatus(400, 'POST', `/api/deliveries/${id2()}/complete`, { token: d, form: f })
  const reset = await ok('POST', `/api/deliveries/${id2()}/reset-otp`, { token: ctx.admin.accessToken, json: { note: 'Identity checked by phone' } })
  check(reset.otpFailedAttempts === 0 && reset.otpLocked === false, 'reset')
  const fresh = (await ok('GET', '/api/orders/me', { token: ctx.customer.session.accessToken })).find(o => o.orderId === ctx.order2.orderId).deliveryLegs[0].deliveryOtp
  check(fresh !== realOtp, 'new code issued')
  return { lockedAfter: 5, correctOtpWhileLocked: 400, resetBy: 'dispatcher', newCodeIssued: true }
})

await step('H.4', 'Damaged package -> hub -> returned to sender; stock and seller earnings restored; refund flagged', async () => {
  const d = ctx.driver.session.accessToken, a = ctx.admin.accessToken
  const stockBefore = (await ok('GET', `/api/product/${ctx.product.id}`)).stockQuantity
  const walletBefore = await ok('GET', '/api/wallets/me', { token: ctx.seller.session.accessToken })
  await ok('POST', `/api/deliveries/${id2()}/fail`, { token: d, json: { reason: 'DAMAGED', note: 'Bag torn' } })
  await ok('POST', `/api/deliveries/${id2()}/hub-scan`, { token: a, json: { trackingCode: ctx.leg2.trackingNumber, hubName: 'Yaounde Central Hub' } })
  const returned = await ok('POST', `/api/deliveries/${id2()}/return`, { token: a, json: { reason: 'DAMAGED', note: 'Returned to seller' } })
  check(returned.status === 'RETURNED', 'returned')
  const order = await ok('GET', `/api/orders/${ctx.order2.orderId}`, { token: a })
  check(order.status === 'RETURNED', 'order RETURNED')
  const stockAfter = (await ok('GET', `/api/product/${ctx.product.id}`)).stockQuantity
  check(stockAfter === stockBefore + 1, 'stock restored')
  const walletAfter = await ok('GET', '/api/wallets/me', { token: ctx.seller.session.accessToken })
  check(walletAfter.pendingBalance < walletBefore.pendingBalance, 'pending earnings reversed')
  const audit = await ok('GET', `/api/admin/audit?entityType=DELIVERY&entityId=${id2()}`, { token: a })
  check(audit.content.some(e => e.action === 'PAYMENT_REFUND_REQUIRED'), 'refund flagged in audit trail')
  const invalid = await expectStatus([400, 403], 'POST', `/api/deliveries/${id2()}/in-transit`, { token: d, json: {} })
  return { status: returned.status, orderStatus: order.status, stockBefore, stockAfter,
    pendingBefore: walletBefore.pendingBalance, pendingAfter: walletAfter.pendingBalance, changeAfterReturn: invalid.status }
})

await step('H.5', 'Audit trail and persistence after refresh / re-login', async () => {
  const a = ctx.admin.accessToken
  const orderAudit = await ok('GET', `/api/admin/audit?entityType=ORDER&entityId=${ctx.order.orderId}`, { token: a })
  const payAudit = await ok('GET', `/api/admin/audit?entityType=PAYMENT&entityId=${report.records.order.paymentId}`, { token: a })
  check(orderAudit.content.length > 0 && payAudit.content.some(e => e.action === 'PAYMENT_COMPLETED'), 'audit entries')
  const sessions = {
    customer: await login(ctx.customer.email, pw), seller: await login(ctx.seller.email, pw), admin: await login(cfg.adminEmail, cfg.adminPassword),
  }
  const views = await Promise.all(Object.values(sessions).map(s => ok('GET', `/api/orders/${ctx.order.orderId}`, { token: s.accessToken })))
  check(views.every(v => v.status === 'DELIVERED' && v.reference === ctx.order.reference), 'records persist after re-login')
  return { orderAuditEntries: orderAudit.totalElements, paymentAudit: payAudit.content.map(e => e.action) }
})

// ------------------------------------------------------------------ report
report.finishedAt = new Date().toISOString()
report.summary = { total: report.steps.length, passed: report.steps.length - failures, failed: failures }
const outDir = join(dirname(fileURLToPath(import.meta.url)), 'results')
mkdirSync(outDir, { recursive: true })
const file = join(outDir, `acceptance-${RUN}.json`)
writeFileSync(file, JSON.stringify(report, null, 2))
console.log(`\n${report.summary.passed}/${report.summary.total} steps passed. Evidence: ${file}`)
process.exit(failures ? 1 : 0)
