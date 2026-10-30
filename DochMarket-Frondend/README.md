# DOCH Frontend Portals

Look at the [Nuxt documentation](https://nuxt.com/docs/getting-started/introduction) to learn more.

## Setup

Make sure to install dependencies:

```bash
# npm
npm install

# pnpm
pnpm install

# yarn
yarn install

# bun
bun install
```

## Development Servers

All five portals share this Nuxt project. Run each command in a separate
terminal from this directory:

| Portal | Command | URL | Account role |
| --- | --- | --- | --- |
| Admin | `npm run dev:admin` | http://localhost:3000 | Admin or staff |
| Driver | `npm run dev:driver` | http://localhost:3001 | `ROLE_DELIVERY` |
| Seller | `npm run dev:seller` | http://localhost:3003 | `ROLE_SELLER` |
| Xender | `npm run dev:xender` | http://localhost:3004 | Sender |
| Market / customer | `npm run dev:market` | http://localhost:3005 | `ROLE_BUYER` |

`npm run dev` starts the admin portal. `npm run dev:customer` is an alias for
the market portal; there is no separate customer app.

For local development, set `NUXT_PUBLIC_API_BASE=http://localhost:8080` in
your ignored `.env` file and start the backend first. Configure the backend's
`CORS_ALLOWED_ORIGINS` to include each URL above. Use `localhost` consistently:
`127.0.0.1` is a different browser origin and must be allowed separately.

Each portal has its own Nuxt build directory and Vite dependency cache so
different portals can run concurrently. Run only one instance of each
portal at a time, including the market/customer aliases.

Driver routes are protected by
[global auth middleware](./apps/driver/middleware/auth.global.ts), including
direct URL access and refreshes. There is no named `auth` middleware to attach
in driver page metadata.

There are no demo accounts or seeded data. Create accounts through the real
flows: customers and sellers sign up in their portals (e-mail verification),
an administrator approves sellers in **Admin → Vendor Agreement**, drivers and
staff are invited from **Admin → User management**. The first super admin is
created by the backend from `BOOTSTRAP_SUPER_ADMIN_PASSWORD`.

## Operational screens

| Portal | Page | Purpose |
| --- | --- | --- |
| Admin | `/orders` | Search orders by ID, `DO-` reference, `DCH-` tracking number or customer; payment and package history; cancel |
| Admin | `/dispatch` | Queue by stage, assign/reassign driver + vehicle (decision matrix enforced), hub scan, return, cancel, reset delivery code |
| Admin | `/products` | Approve / reject seller listings |
| Admin | `/vendor-agreement` | Approve / reject / suspend sellers |
| Admin | `/audit` | Audit trail |
| Seller | `/orders` | Paid orders to fulfil; upload sealed-package photo to release to dispatch |
| Market | `/orders`, `/orders/:id` | Order history, payment retry, tracking timeline, delivery code, cancel, rating |
| Driver | `/deliveries` | Pickup scan (camera QR or typed code), out for delivery, failed attempt / not collected, OTP + proof photo, trip stops |
| Xender | `/track/:trackingNumber` | Public tracking (no personal data, no delivery code) |

Payments use Fapshi (MTN MoMo / Orange Money). In the Fapshi **sandbox**,
`670000000` always succeeds and `670000001` always fails.

## Production

Build the application for production:

```bash
# npm
npm run build

# pnpm
pnpm build

# yarn
yarn build

# bun
bun run build
```

Locally preview production build:

```bash
# npm
npm run preview

# pnpm
pnpm preview

# yarn
yarn preview

# bun
bun run preview
```

Check out the [deployment documentation](https://nuxt.com/docs/getting-started/deployment) for more information.
