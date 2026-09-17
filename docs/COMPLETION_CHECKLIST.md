# Completion checklist

Status against the build brief's §10 acceptance criteria. Legend:
✅ implemented and reasoned through end-to-end · 🟡 implemented, not yet
exercised in a running environment · ⬜ not implemented.

| # | Behavior | Status | Where |
|---|---|---|---|
| 1 | OTP expiration, replay prevention, rate limits, prod rejection of demo auth | ✅ | `OtpService`, `OtpFlowIntegrationTest`, `ProdSafetyConfig` |
| 2 | Persistence/resumption after logout/reload; direct-route access can't bypass onboarding gates | ✅ (server-side gates); 🟡 (full browser reload cycle not run against a live backend) | `PartnerService` stage machine, every step endpoint's own precondition check, `OnboardingResumeGuard` (client convenience only) |
| 3 | Partner ownership boundaries, admin permissions, private document retrieval, sensitive-field filtering | ✅ | `CurrentUser`, every `require*Owned*` check, `SecurityConfig` role rules, `NearbyJobResponse` vs `AssignedJobResponse` |
| 4 | Screening capacity under concurrent booking; atomic reschedule | ✅ written test, 🟡 not executed (no Docker in build env) | `ScreeningSlotRepository#tryReserveSeat`, `ScreeningConcurrencyIntegrationTest` |
| 5 | Starter-kit double-click/payment-event idempotency, honest failure states | ✅ written test, 🟡 not executed | `KitOrder.idempotencyKey` unique constraint, `KitOrderIdempotencyIntegrationTest` |
| 6 | Activation denied until every configured requirement passes | ✅ | `ActivationService#checkEligibility` (re-derives from source tables, not just the stage) |
| 7 | Nearby-job filtering; only one winner under concurrent acceptance | ✅ written test, 🟡 not executed | `ServiceRequestRepository#tryAssign`, `JobAcceptanceConcurrencyIntegrationTest` |
| 8 | Exactly one earning entry per completed job; accurate arithmetic; reversals; duplicate payout-event handling | ✅ | `earning_entry` unique constraint + `MoneyService#recordEarningForCompletedJob`, `#reverseEarning`, `#handlePayoutWebhookEvent` (idempotent on `provider_event_id`) |
| 9 | Browser camera/location denial with usable manual fallback | ✅ | `pages/onboarding/Permissions.jsx` (denied/unavailable states shown; manual city/file-upload paths exist elsewhere) |
| 10 | One end-to-end synthetic partner journey, signup through active + completed job + earnings | 🟡 walkthrough written, not run against a live backend | `docs/MANUAL_QA.md` §1–2; seed data (`db/seed/V904`, `V905`) pre-populates an already-active partner with a completed job as a shortcut past the full walkthrough |
| 11 | Frontend build, backend build, MySQL migrations on empty DB, mobile/desktop layout checks | 🟡 partial | Frontend build: ✅ run (`npm run build`, clean). Backend build: ✅ run (`./mvnw clean package` via test-compile checks). Migrations against a real empty MySQL: ⬜ not run (no Docker/DB in build env — see below). Mobile layout: ✅ checked at 375×812 in-browser (Welcome page); desktop: ✅ checked at pane width. |

## What was actually run, right now, in this build

```
✅ ./mvnw -q compile        (repeated after every backend change)
✅ ./mvnw -q test-compile   (integration tests compile cleanly)
✅ npm run build             (frontend, twice — after dashboard tabs and after admin app)
✅ Welcome + OTP-entry pages rendered in-browser, desktop and 375px mobile widths
✅ OTP request against a backend with no reachable database confirmed a clean
   error message in the UI (502 handled gracefully, no crash) rather than a
   silent failure
⬜ Flyway migrations against a real MySQL instance
⬜ ./mvnw test (Testcontainers — needs a Docker engine)
⬜ docker compose up (full stack)
⬜ Any authenticated flow beyond the OTP-request screen (needs a live backend)
```

A local MySQL 8 service was available on this machine, but the user asked to
validate via Docker Compose later rather than have this session use it — see
`docs/ASSUMPTIONS.md`. **Before relying on this build, run at minimum
`docker compose -f infra/docker-compose.yml up --build` and walk through
`docs/MANUAL_QA.md`.**
