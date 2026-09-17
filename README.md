# SupplyBase Partners

The professional-facing platform for SupplyBase: service professionals join, complete
verification and onboarding, attend a skill session, obtain a starter kit, complete
training, become active, find nearby jobs, and track earnings. Admin staff review
applications, manage the catalog, and run the job/money/community back office.

This is a **new, standalone project** — it does not read from or write to any
existing SupplyBase production system.

Stack: **React (Vite) · Spring Boot 4 / Java 21 · MySQL 8 · Flyway · Docker Compose.**

For the assumptions this build makes beyond the source brief (schema choices, gate
rules, provider adapters, etc.), see [`docs/ASSUMPTIONS.md`](docs/ASSUMPTIONS.md).

## Repository layout

```
backend/    Spring Boot API (Java 21, Maven Wrapper)
frontend/   React + Vite SPA
infra/      docker-compose.yml
docs/       Assumptions, API reference, this project's design notes
```

## Quick start (Docker Compose)

Requires Docker Desktop (or an equivalent Docker engine) with Compose v2.

```bash
cp .env.example .env
# edit .env if you want non-default ports/credentials
docker compose -f infra/docker-compose.yml --env-file .env up --build
```

- Frontend: http://localhost:8081
- Backend API: http://localhost:8080/api/v1
- Backend health: http://localhost:8080/actuator/health

On first boot the backend runs all Flyway migrations against a fresh MySQL
database (including the dev/demo seed data — see below) and creates the first
`ADMIN` account from `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` in
`.env` (defaults: `admin` / `ChangeMe123!` — **change this immediately** if you
keep it running). Sign in to the admin app at `/admin/login`.

To stop: `docker compose -f infra/docker-compose.yml down` (add `-v` to also drop
the MySQL data volume and start clean next time).

## Running natively (without Docker)

### Backend

Requires JDK 21 and a MySQL 8.x server reachable at `localhost:3306` (create an
empty database first, e.g. `CREATE DATABASE supplybase_partners CHARACTER SET
utf8mb4;`).

```powershell
cd backend
$env:DB_URL = "jdbc:mysql://localhost:3306/supplybase_partners?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-mysql-password>"
$env:ADMIN_BOOTSTRAP_USERNAME = "admin"
$env:ADMIN_BOOTSTRAP_PASSWORD = "ChangeMe123!"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

```bash
# Linux/macOS
cd backend
DB_URL="jdbc:mysql://localhost:3306/supplybase_partners?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=UTC" \
DB_USERNAME=root DB_PASSWORD=<your-mysql-password> \
ADMIN_BOOTSTRAP_USERNAME=admin ADMIN_BOOTSTRAP_PASSWORD=ChangeMe123! \
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile additionally applies `backend/src/main/resources/db/seed/*`
(provisional categories, cities, a demo training course, screening slots, and
synthetic partners/jobs at every onboarding stage) so the app is immediately
usable. **These migrations never run under the `prod` profile.**

### Frontend

Requires Node 20+.

```bash
cd frontend
npm install
npm run dev
```

Opens on http://localhost:5173, with Vite's dev server proxying `/api/*` to
`http://localhost:8080` (see `frontend/vite.config.js`) — start the backend first.

### Ports (defaults)

| Service | Port |
|---|---|
| Backend | 8080 |
| Frontend (dev) | 5173 |
| Frontend (Docker/nginx) | 8081 |
| MySQL | 3306 (Docker Compose keeps this internal to the compose network only) |

## Demo / synthetic accounts

Seed data (`local`/`docker` profiles only) includes phone numbers `+9198000000{01..07}`
covering a new applicant, pending verification, a rejected/resubmit case, a booked
screening session, a partner mid-starter-kit, a partner mid-training, and one fully
**active** partner (`+919800000007`) with an in-progress and a completed job already
on their Money tab. Since there is no real SMS provider wired up by default
(`SMS_PROVIDER_MODE=dev_local`), the OTP code for any of these numbers — or a new
one you sign up with — can be read from:

```
GET /api/v1/auth/otp/dev/last-code?phone=<phone>
```

This endpoint is compiled out of the `prod` profile.

The bootstrapped admin account (see above) can sign in to categories/cities/kit
catalog management, verification review, screening slot creation, training
content, job/customer creation, money adjustments, and community moderation
under `/admin`.

## Environment variables

See [`.env.example`](.env.example) for the full list. The important ones:

- `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` — first admin account.
- `SMS_PROVIDER_MODE`, `IDENTITY_PROVIDER_MODE`, `KIT_PAYMENTS_MODE`, `PAYOUTS_MODE`
  — provider adapters; local/demo defaults never claim to send/verify/charge
  anything real (see `docs/ASSUMPTIONS.md` for what each mode actually does).
- `WEBHOOKS_SHARED_SECRET` — HMAC secret for `/api/v1/webhooks/*`.
- `CORS_ALLOWED_ORIGINS` — comma-separated allowlist.

Under the `prod` Spring profile, `ProdSafetyConfig` refuses to start if it
detects any of these still on a local/demo default (`dev_local` SMS mode, the
default admin password, the default webhook secret, or a non-HTTPS session
cookie) — see `backend/src/main/java/com/supplybase/partners/config/ProdSafetyConfig.java`.

## Build & test commands

```bash
# Backend build
cd backend && ./mvnw clean package

# Backend tests (needs a Docker engine -- Testcontainers starts a real MySQL
# per test class; H2 is deliberately not used, since it doesn't reproduce
# MySQL's locking/constraint behavior closely enough for the concurrency tests)
cd backend && ./mvnw test

# Frontend build
cd frontend && npm run build

# Frontend lint
cd frontend && npm run lint
```

## What's implemented vs. scaffolded

This is a large brief; everything in `docs/ASSUMPTIONS.md`'s "acceptance status"
section is either implemented-and-manually-exercised, implemented-but-not-yet
exercised end-to-end, or a documented gap. In short:

- **Fully wired, API → MySQL, with a working UI:** phone-OTP auth + resumable
  onboarding (all 8 wizard steps), identity verification submit/review,
  screening slot booking/reschedule/cancel/check-in + admin outcome decision,
  starter-kit quote/booking/simulated-payment/admin fulfillment, training
  modules + server-graded assessments, activation gating, nearby-job matching
  + atomic accept + full job lifecycle, the earnings ledger + payout request,
  community posts/comments/reports/announcements, support tickets, the
  notification inbox, and an admin app covering dashboard/partners/
  verification/support/community/audit logs.
- **API-complete, minimal/no dedicated admin UI screen yet:** catalog CRUD
  (categories/services/cities/areas/estimates/policies), starter-kit and
  training-course authoring, kit-order fulfillment status updates, screening
  slot creation, and money adjustments/reversals all have working REST
  endpoints (see `docs/openapi.yaml`) that were exercised via HTTP directly,
  not yet through a dedicated admin screen.
- **Documented extension points, not real integrations:** SMS/WhatsApp,
  identity verification, kit payments, and payouts all ship a working
  `dev_local`/`dev_simulator` adapter and fail loudly (never silently) if
  switched to a mode without a real provider wired up.

## Documentation

- [`docs/ASSUMPTIONS.md`](docs/ASSUMPTIONS.md) — every implementation decision
  made beyond the literal source brief, and why.
- [`docs/openapi.yaml`](docs/openapi.yaml) — API reference.
- [`docs/SupplyBase-Partners.postman_collection.json`](docs/SupplyBase-Partners.postman_collection.json) — importable Postman collection.
