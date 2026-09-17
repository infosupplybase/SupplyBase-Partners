# Manual QA checklist

Steps actually reasoned through against the implemented API/UI during this
build. None of these were run against a live MySQL instance in the build
environment (see `docs/ASSUMPTIONS.md`) — treat this as a structured
walkthrough script for you to execute against `docker compose up`, not a
report of tests already passed.

## 1. Onboarding, end to end

1. Open the frontend, click **Join as a Partner**.
2. Enter a 10-digit mobile number → **Send OTP**.
3. Fetch the dev code: `GET /api/v1/auth/otp/dev/last-code?phone=<phone>` (or
   use the on-screen dev hint) → enter it → verify.
4. Fill basic details → continue.
5. Pick a category (e.g. Cleaning) → continue.
6. Pick a city (e.g. Mumbai) → continue.
7. Expand and accept Terms + Privacy → continue.
8. Confirm the earning estimate renders and is labelled "Demo estimate" →
   continue.
9. Pick working hours → continue.
10. Try Allow on location/camera (accept and deny both paths) → continue
    regardless of the outcome.
11. Land on the dashboard **Progress** tab with 5 tiles, all locked except
    **Identity verification**.
12. **Refresh the page / log out and back in** — confirm you land back on the
    dashboard (not re-asked for basic details), i.e. resume works.

## 2. Verification → Screening → Kit → Profile → Training → Activation

1. Submit a document on the Verification tile (any JPEG/PNG/PDF).
2. As admin (`/admin/login`), open **Verification**, approve it → the
   partner's Screening tile unlocks.
3. As the partner, book an available slot, check in only once its check-in
   window opens (adjust a seed slot's `starts_at` if testing immediately), or
   as admin decide the outcome PASSED once checked in → Starter Kit tile
   unlocks.
4. Book the starter kit (or confirm it auto-skips if no kit is configured for
   that category) → Profile tile unlocks.
5. Fill working hours/languages/bio, save, **Mark profile complete** →
   Training tile unlocks.
6. Complete each module, take the assessment, pass it →
   `ACTIVATION_REVIEW`.
7. As admin, open the partner detail page → **Activation eligibility** should
   show all green → **Activate**.
8. As the partner, confirm the Around You tab now shows nearby jobs instead of
   the "finish onboarding" prompt.

## 3. Concurrency (the acceptance-criteria-mandated checks)

- **Screening double-booking**: with a slot at `capacity=1, booked_count=0`,
  fire two `POST /api/v1/screening/bookings` for two different partners at
  the same instant (curl in two terminals, or the `ScreeningConcurrencyTest`
  integration test) — exactly one should succeed, the other a 409.
- **Job double-accept**: same pattern against
  `POST /api/v1/jobs/{id}/accept` for an `OPEN` job with two active partners
  eligible for it.
- **Kit order idempotency**: POST `/api/v1/kit-orders` twice with the same
  `idempotencyKey` — the second call should return the *same* order, not
  create a second one.
- **Earning exactly-once**: confirm completion on the same job twice (second
  call should 400 — status is no longer `COMPLETION_SUBMITTED`) and check
  `earning_entry` has exactly one row for that `service_request_id`.

## 4. Auth edges

- Wrong OTP code 5 times in a row → 6th attempt is rejected as "too many
  attempts" without even checking the code.
- Request OTP, then request again immediately → 429 with a "wait Ns" message
  until the resend cooldown elapses.
- Verified, non-staff session hitting any `/api/v1/admin/**` route → 403.
- No session hitting `/api/v1/partners/me` → 401 JSON body (not an HTML login
  redirect).

## 5. Ownership boundaries

- Partner A's session calling `GET /api/v1/jobs/{jobId-assigned-to-B}` → 403.
- Partner A's session downloading Partner B's identity document via
  `/api/v1/partners/me/verification/documents/{id}/file` → 403 (the
  `documentId` there is scoped to the caller's own partner id lookup, so this
  should actually 400/403 rather than leak another partner's file).

## 6. Build/deploy sanity

```bash
cd backend && ./mvnw clean package     # backend builds
cd frontend && npm run build           # frontend builds
docker compose -f infra/docker-compose.yml up --build   # full stack, fresh DB
```

Confirm: Flyway applies all 10 core + 6 seed migrations cleanly on an empty
database, the backend health check passes, and the frontend loads through
nginx with `/api/*` proxied through to the backend (check a network request in
devtools resolves against the frontend's own origin, not `localhost:8080`
directly).
