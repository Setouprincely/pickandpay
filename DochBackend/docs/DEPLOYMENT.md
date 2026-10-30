# DOCH — Staging deployment and rollback plan

## 1. Components

| Component | Runtime | Default port |
| --- | --- | --- |
| API (`DochBackend`) | Docker image built from `Dockerfile` (Java 25, Spring Boot 4) | 8080 |
| Database | PostgreSQL 18 (`docker-compose.yml` service `db`) | 5432 |
| Admin / Driver / Seller / Xender / Market portals (`DochMarket-Frondend`) | Nuxt 3 apps (`pnpm build` → Node server or static hosting) | 3000 / 3001 / 3003 / 3004 / 3005 |
| Object storage | S3-compatible bucket (MinIO or Backblaze B2) | — |
| E-mail | Resend | — |
| Payments | Fapshi (sandbox for staging) | — |
| Geocoding / road distance | OpenStreetMap Nominatim + OSRM (`GEO_PROVIDER=osm`) or Google Maps (`GEO_PROVIDER=google`, billing required) | — |

## 2. Configuration (environment variables)

Copy `.env.example` to `.env` (git-ignored) and set at least:

| Variable | Staging value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` (or `prod` with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | dedicated staging database — **never** reuse the development database |
| `JWT_SECRET_KEY` | ≥ 64 random characters |
| `BOOTSTRAP_SUPER_ADMIN_EMAIL`, `BOOTSTRAP_SUPER_ADMIN_PASSWORD` | first super-admin account, created only if it does not exist; no default password is compiled in |
| `FAPSHI_API_USER`, `FAPSHI_API_KEY`, `FAPSHI_BASE_URL=https://sandbox.fapshi.com` | sandbox merchant credentials |
| `FAPSHI_WEBHOOK_SECRET` | same secret as configured in the Fapshi dashboard; webhook URL `https://<api-host>/api/payments/webhook/fapshi` |
| `RESEND_API_KEY` | key for a **verified** sending domain (otherwise mails can only go to the account owner / `@resend.dev`) |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET_NAME` | bucket for product, sealed-package and proof-of-delivery photos |
| `GEO_PROVIDER`, `GOOGLE_MAPS_API_KEY` | `osm`, or `google` with a billed key |
| `CORS_ALLOWED_ORIGINS` | exact origins of the five portals |
| `APP_MARKET_FRONTEND_URL`, `APP_MARKET_PAYMENT_REDIRECT_URL`, `APP_XENDER_FRONTEND_URL`, `APP_XENDER_PAYMENT_REDIRECT_URL` | public URLs of Market / Xender (`…/payment-status`) |
| `AUTH_AUTO_VERIFY` | `false` (accounts must verify their e-mail) |
| optional tuning | `AUTH_RATE_LIMIT_PER_MINUTE` (20), `ORDER_UNPAID_CANCEL_HOURS` (48), `PAYMENT_RECONCILE_INTERVAL_MS` (120000), `PAYMENT_EXPIRE_HOURS` (24), `DELIVERY_MAX_OTP_ATTEMPTS` (5) |

Frontend: every portal reads `NUXT_PUBLIC_API_BASE` (see `DochMarket-Frondend/README.md`).

## 3. Deployment steps

1. **Back up** the target database (skip on a brand-new database):
   ```bash
   docker exec doch-postgres pg_dump -U postgres -d doch -Fc -f /tmp/doch_$(date +%Y%m%d%H%M).dump
   docker cp doch-postgres:/tmp/doch_<timestamp>.dump ./backups/
   ```
2. **Tag the currently running image** so it can be restored: `docker tag doch-app:latest doch-app:previous`.
3. **Build and start** the new version:
   ```bash
   docker compose build app
   docker compose up -d app
   docker compose logs -f app   # wait for "Started DochApplication"
   ```
   On start-up Flyway applies pending migrations in order:
   * empty database → `V1__baseline_schema`, `V2__workflow_hardening`, `V3__backfill_package_vehicle_requirements`;
   * database previously created by Hibernate `ddl-auto=update` → baselined at V1, then V2 and V3.

   Hibernate then validates the schema (`ddl-auto=validate`); the application refuses to start on any mismatch.
4. **Smoke test**: `GET /swagger-ui.html`, log in to Admin with the bootstrap account, check *Dashboard*, *Orders*,
   *Dispatch* and *Settings*.
5. **Acceptance run** (staging only — creates test accounts, products and orders):
   `node acceptance/run-acceptance.mjs` (see [ACCEPTANCE.md](ACCEPTANCE.md) §5). Archive the JSON produced in
   `acceptance/results/` as sign-off evidence.
6. Build and deploy the portals (`pnpm install && pnpm build` in `DochMarket-Frondend`, then run each app's `.output`
   server or deploy to the hosting provider) with `NUXT_PUBLIC_API_BASE` pointing at the staging API.
7. Configure the Fapshi dashboard webhook URL and secret; place one sandbox order and confirm that the payment becomes
   `COMPLETED` through the webhook (the reconciliation job also catches missed callbacks within 2 minutes).

## 4. Rollback

Flyway migrations are forward-only. Choose the rollback path according to what failed:

| Situation | Action |
| --- | --- |
| New application version misbehaves, migrations succeeded | Redeploy the previous image of **this** release line (`docker tag doch-app:previous doch-app:latest && docker compose up -d app`). Rolling back to the pre-Flyway demo version is not supported against a migrated database (its `DemoDataSeeder` and Hibernate `update` mode would write into the new schema) — restore the backup instead (row 3). |
| A migration failed part-way | The application does not start. PostgreSQL runs each Flyway migration in a transaction, so the failed migration is rolled back automatically; fix the cause and redeploy, or redeploy the previous image. Check `flyway_schema_history` for a `success = false` row and remove it before retrying. |
| Data must be restored to the pre-deployment state | Stop the app, then restore the backup taken in step 1: `pg_restore --clean --if-exists -U postgres -d doch /tmp/doch_<timestamp>.dump`, start the previous image. Payments made at Fapshi after the backup must be reconciled manually from the Fapshi dashboard. |

## 5. Operations checklist

* Scheduled jobs (enabled by default, `application.jobs.enabled`): payment reconciliation every 2 min (pending payments are
  re-checked with Fapshi and failed once older than `PAYMENT_EXPIRE_HOURS`), and every 15 min cancellation of orders still
  unpaid after `ORDER_UNPAID_CANCEL_HOURS` (stock is released).
* Audit trail: Admin → *Audit log* or `audit_log` table (payment changes, dispatch actions, OTP lockouts/resets,
  cancellations, refunds required, moderation, settings changes).
* Refunds: search the audit log for `PAYMENT_REFUND_REQUIRED` and pay out through Fapshi.
* Backups: schedule a daily `pg_dump` and keep the object-storage bucket versioned.
