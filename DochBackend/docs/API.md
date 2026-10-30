# DOCH API — workflow endpoints and permissions

The complete, always-current contract (request/response schemas) is served by SpringDoc at `/swagger-ui.html`
(`/v3/api-docs` for the JSON). This page summarises the endpoints used by the acceptance workflows and who may call them.

Conventions

* Authentication: `Authorization: Bearer <access_token>` from `POST /api/auth/login` (response fields are snake_case:
  `access_token`, `refresh_token`, `user_id`, `role`).
* Errors: `{ "status", "error", "message", "timestamp" }`. `400` validation, `401` not authenticated / disabled account,
  `403` not permitted or not the owner, `404` not found, `409` illegal state transition or concurrent update,
  `413` upload too large, `429` too many authentication attempts. Internal details are never returned.
* Ownership is enforced in the service layer in addition to the permission checks below — a customer only sees their
  orders, a seller only their items/packages, a driver only packages assigned to them.

## 1. Roles and permissions

| Permission | Super admin | Market admin | Xender admin | Seller | Buyer | Driver |
| --- | :-: | :-: | :-: | :-: | :-: | :-: |
| `MANAGE_SYSTEM` (settings, payment reconcile, fleet) | ✔ | | | | | |
| `MANAGE_USERS` (staff, drivers, user status) | ✔ | | | | | |
| `MANAGE_MARKET` (product moderation, all orders, seller verification) | ✔ | ✔ | | | | |
| `MANAGE_XENDER` (fleet, Xender requests) | ✔ | | ✔ | | | |
| `ASSIGN_DRIVER` (dispatch, hub scan, return, cancel package, OTP reset) | ✔ | ✔ | ✔ | | | |
| `VIEW_ALL_DELIVERIES` (logistics queue) | ✔ | ✔ | ✔ | | | |
| `VIEW_*_STATS` (dashboard) | ✔ | ✔ | partly | | | |
| `CREATE_PRODUCT` | ✔ | ✔ | | ✔ | | |
| `PLACE_ORDER` (buy, pay, Xender request, review) | | | | ✔ | ✔ | |
| `DELIVER_PRODUCT` (driver actions) | | | | | | ✔ |
| `VIEW_OWN_FINANCE` (wallet) | | | | ✔ | ✔ | ✔ |

Role permissions are refreshed from `common/Initializer` on every start-up and authorities are re-read from the database on
every request, so a role change or account suspension takes effect immediately.

**One person, one account, several roles.** Buyer, seller and driver roles can be combined on the same email and password
(table `user_extra_roles`; `users.user_role_id` stays the primary role). Signing up on another portal with an existing
email adds that role after the account's current password is checked; an admin onboarding an existing customer/seller as a
driver adds the driver role. The login response returns `role` (primary) and `roles` (all), and each portal admits the
account when `roles` contains its role. Admin/staff accounts stay single-role.

## 2. Endpoints

### Public (no token)

| Method & path | Purpose |
| --- | --- |
| `POST /api/auth/register`, `/seller/register`, `/verify`, `/login`, `/logout`, `/staff-setup`, `/forgot-password`, `/reset-password`; `GET /api/auth/resend` | accounts (rate limited per IP) |
| `GET /api/product`, `GET /api/product/{id}`, `GET /api/product/category/{id}`, `GET /api/category/**` | published catalogue only |
| `GET /api/tracking/{trackingNumber}` | public package tracking (status, timeline, no personal data, no delivery code) |
| `POST /api/payments/webhook/fapshi` | Fapshi callback; requires `x-wh-secret` when `FAPSHI_WEBHOOK_SECRET` is set; status is always re-queried from Fapshi |

### Catalogue (Workflow B)

| Method & path | Permission | Notes |
| --- | --- | --- |
| `POST /api/product/productWithImage` (multipart) | `CREATE_PRODUCT` | verified seller only; creates `PENDING_REVIEW`; weight + dimensions |
| `GET /api/product/mine` | `CREATE_PRODUCT` | seller's own products, every publication status |
| `PUT /api/product/{id}` | owner | edits of published products go back to review |
| `PATCH /api/product/{id}/active?active=` | owner or admin | hide / show |
| `GET /api/product/admin?status=` | `MANAGE_MARKET` | moderation queue |
| `PUT /api/product/{id}/approve`, `PUT /api/product/{id}/reject` `{reason}` | `MANAGE_MARKET` | audited |
| `PUT /api/sellers/verify/{sellerId}` `{verified, note}` | `MANAGE_MARKET` | rejection requires a note; seller notified |

### Orders & payments (Workflows C, D, G, H)

| Method & path | Permission | Notes |
| --- | --- | --- |
| `POST /api/orders/preview` | `PLACE_ORDER` | server quote: subtotal, delivery fee per seller, total |
| `POST /api/orders` `{items, deliveryAddress, contactPhone, paymentPhone?}` | `PLACE_ORDER` | locks stock, creates `DO-` reference, packages `DCH-…`, starts Fapshi payment (`paymentPhone` → phone prompt, otherwise payment link) |
| `GET /api/orders/me` | authenticated | customer's orders with packages, timeline and delivery codes |
| `GET /api/orders/{id}` | owner / seller of an item / admin | perspective-filtered |
| `GET /api/orders/seller/me` | `ROLE_SELLER` | only the seller's items of paid orders |
| `GET /api/orders/all?q=&status=&from=&to=&page=&size=` | `MANAGE_MARKET` / `VIEW_ALL_DELIVERIES` | `q` matches order id, `DO-` reference, `DCH-` tracking number, customer name/e-mail/phone |
| `POST /api/orders/{id}/cancel` `{reason}` | owner (before dispatch) or admin | restores stock, reverses seller earnings, flags refund when paid |
| `POST /api/orders/{id}/review` `{rating, deliveryRating, comment}` | `PLACE_ORDER`, owner | delivered orders only, once |
| `POST /api/payments/initiate` `{orderId \| deliveryId, phone?}` | `PLACE_ORDER`, owner | retry after failure; amount always computed by the server |
| `GET /api/payments/{id}/status` | owner / admin | re-queries Fapshi (throttled) |
| `POST /api/payments/{id}/reconcile` | admin | force a provider re-check |

### Logistics (Workflows E, F, H)

| Method & path | Permission | Notes |
| --- | --- | --- |
| `POST /api/deliveries/preview`, `POST /api/deliveries` (multipart) | `PLACE_ORDER` | DochXender request + payment |
| `POST /api/deliveries/market/{id}/ready` (multipart sealed photo) | `ROLE_SELLER`, owner | `PENDING_PREPARATION → PENDING_ASSIGNMENT` |
| `GET /api/deliveries/all?status=&q=&page=` , `GET /api/deliveries/summary` | `VIEW_ALL_DELIVERIES` | dispatch queue + counters |
| `POST /api/deliveries/{id}/assign` `{driverId, vehicleId, scheduledFor?, note?}` | `ASSIGN_DRIVER` | assign or reassign; vehicle category must meet the package requirement; driver notified |
| `POST /api/deliveries/{id}/unassign` `{reason}` | `ASSIGN_DRIVER` | driver unavailable |
| `POST /api/deliveries/{id}/hub-scan` `{scannedCode, hub, note}` | `ASSIGN_DRIVER` | code must match |
| `POST /api/deliveries/{id}/return` / `cancel` `{reason}` | `ASSIGN_DRIVER` | stock + earnings handled, refund flagged |
| `POST /api/deliveries/{id}/reset-otp` | `ASSIGN_DRIVER` | unlocks after 5 wrong codes |
| `GET /api/drivers/me/deliveries`, `/me/history`, `/me/profile`, `/me/performance` | `DELIVER_PRODUCT` | driver's own jobs only |
| `PATCH /api/drivers/me/availability`, `PUT /api/drivers/me/location` | `DELIVER_PRODUCT` | |
| `GET /api/deliveries/me/optimized-route`, `GET /api/deliveries/{id}/route` | `DELIVER_PRODUCT` | multi-stop sequence with addresses and package references |
| `POST /api/deliveries/{id}/pickup` `{scannedCode, lat, lng}` | `DELIVER_PRODUCT`, assigned driver | rejects mismatched codes |
| `POST /api/deliveries/{id}/not-collected` `{reason}` | assigned driver | back to dispatch queue |
| `POST /api/deliveries/{id}/in-transit` | assigned driver | out for delivery / re-attempt |
| `POST /api/deliveries/{id}/fail` `{reason: RECIPIENT_UNAVAILABLE \| REFUSED \| ADDRESS_NOT_FOUND \| DAMAGED …}` | assigned driver | |
| `POST /api/deliveries/{id}/complete` (multipart `otp`, `proofImage`, `lat`, `lng`) | assigned driver | wrong code counted; 5 → locked |
| `GET /api/drivers/available`, `GET /api/fleet` | `ASSIGN_DRIVER` / fleet permissions | dispatch pickers |

### Administration

| Method & path | Permission |
| --- | --- |
| `GET /api/stats/dashboard` | `VIEW_STATS` |
| `GET/PUT /api/settings/pricing` (fees, vehicle decision matrix, moderation switch) | read: `MANAGE_SYSTEM` / `MANAGE_XENDER`, write: `MANAGE_SYSTEM` |
| `GET /api/admin/audit?entityType=&entityId=&action=&page=` | `MANAGE_SYSTEM` / `MANAGE_MARKET` / `MANAGE_XENDER` |
| `POST /api/admin/onboard-driver`, `/onboard-staff`, `PATCH /api/admin/users/{id}/status` | `MANAGE_USERS` |
| `DELETE /api/admin/users/{id}/roles/{role}` — remove one role (seller → store un-verified, products hidden; driver → refused while carrying packages) | `MANAGE_USERS` |
| `DELETE /api/admin/users/{id}` — soft delete: cannot sign in, hidden from lists, orders/payments/audit kept, email reusable | `MANAGE_USERS` |
