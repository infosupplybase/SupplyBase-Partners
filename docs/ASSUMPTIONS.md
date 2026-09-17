# Implementation assumptions and decisions

The `URBAN COMPANY PARTNER-1.pdf` reference is a four-page written functional
flow, not a screenshot catalog or a database spec. Per the build brief's source
priority (explicit instructions > PDF for workflow > recorded assumptions
below), this document is the authoritative record of every decision made to
turn that flow into a working, normalized, tested application.

## Platform choices

- **Spring Boot 4.1.1 / Spring Framework 7**, not the 3.x line. At build time,
  `start.spring.io` reported `3.x` as out of its supported compatibility range
  (`>=4.0.0` only) — 4.1.1 was the latest non-milestone release, so it is the
  "compatible, currently supported" choice, not a deliberate bleeding-edge
  pick. One consequence worth flagging for future contributors: **Jackson 3**
  ships under the new `tools.jackson.*` package/group (e.g.
  `tools.jackson.databind.ObjectMapper`), not the classic
  `com.fasterxml.jackson.databind`; `com.fasterxml.jackson.annotation.*`
  (the annotations themselves) is unchanged.
- **Testcontainers 1.21.4** is pinned explicitly rather than trusting Boot's
  own BOM property (`testcontainers.version=2.0.5`), because that 2.x version
  is not yet published to Maven Central for the `mysql`/`junit-jupiter`
  modules at time of writing.
- Same story for `TestRestTemplate`: Boot 4 moved it out of `spring-boot-test`
  into a dedicated `spring-boot-resttestclient` module, under package
  `org.springframework.boot.resttestclient` (not
  `org.springframework.boot.test.web.client`). Both this and the Jackson 3
  move above were caught by actually compiling against 4.1.1 rather than
  assuming 3.x-era package names still applied.
- MySQL migrations were reviewed carefully by hand but **not executed against
  a live MySQL instance during this build** (no Docker engine was available in
  the build environment, and the user asked to validate later via `docker
  compose` rather than the machine's separate local MySQL install). Run
  `./mvnw test` (Testcontainers-backed) or `docker compose up` as your first
  real-MySQL check.

## Schema

The PDF's own database section is treated as a set of suggestions, not
evidence of the original app's real schema, per the build brief. The actual
schema (`backend/src/main/resources/db/migration/V1..V10`) is designed from
scratch, normalized, with FKs/unique constraints/indexes/optimistic locking
throughout. Notable consolidations: partner language/skill/coverage/status-history
are separate small tables rather than JSON blobs on `partner`, so they can be
queried and audited directly; `service_requirement` (kit/training requirement
per service) is deliberately one row per service rather than per category, so
future catalog changes can vary requirements at the finer grain without a
migration.

Money is stored as **integer paise** (`BIGINT`), never floating point, per the
brief. Timestamps are `DATETIME(3)` UTC; the business timezone (`Asia/Kolkata`)
is applied only at display/aggregation time (see `MoneyController`'s month
boundary calculation).

Seed data lives in a **separate Flyway location** (`db/seed`, only applied
under `local`/`docker`/`test` profiles — see `application-prod.yml`, which
points `spring.flyway.locations` at `db/migration` alone) so synthetic
partners/customers/jobs can never end up in a production database.

## Onboarding state machine

`Partner.onboardingStage` is a single backend-owned enum
(`BASIC_DETAILS → WORK_CATEGORY → CITY → TERMS_PRIVACY → EARNING_POTENTIAL →
WORKING_HOURS → PERMISSIONS → VERIFICATION → SCREENING → STARTER_KIT →
PROFILE → TRAINING → ACTIVATION_REVIEW → COMPLETE`). Each step's own service
method only ever advances the stage when the partner is *exactly* at that
step's "from" stage (`PartnerService#advanceIfAt`) — so calling a step
endpoint out of order is a no-op on the stage (data still saves), and editing
already-completed data later (e.g. changing your category from profile
settings) never perturbs progress. Verification/screening/kit/training each
call back into `PartnerService#advanceStageIfCurrent` once their own
server-side gate is satisfied (verification approved, screening outcome
PASSED, kit delivered/exempted, training completed) — the client never sets
`onboardingStage` directly, and there is no endpoint that accepts it as input.

**Frontend route split**: the first 7 stages (`BASIC_DETAILS`…`PERMISSIONS`)
are a linear full-screen wizard, matching the PDF's sections A–H. From
`VERIFICATION` onward, the PDF's own section K ("Progress stages: Session |
Starter Kit | Profile | Training") describes a *dashboard tile* layout, not a
forced linear wizard — so this build routes those stages to the dashboard's
Progress tab, where each tile links to its own detail screen. This is a
deliberate interpretation, not an observed PDF screen.

**Activation** (`ActivationService`) re-derives eligibility directly from the
underlying tables (current-version consent rows, verification status,
screening outcome, training completion, profile field presence) rather than
only trusting `onboardingStage == ACTIVATION_REVIEW`, since this is the
highest-consequence transition (unlocks jobs and payouts) and defense in depth
felt warranted. It is always an explicit, audited `ADMIN` action
(`POST /admin/partners/{id}/activate`), never automatic.

## Starter kit

The PDF only says the kit "can be viewed after booking an order"; it does not
specify pricing/payment mechanics. This build implements the explicit
interpretation named in the brief: a kit-order **introduction** screen
(required items, full price/fee breakdown, terms) with a **Book Starter Kit**
action that immediately creates an order and (in `dev_simulator` mode)
synchronously "pays" it — clearly logged as simulated, never claiming a real
charge. Booking is idempotent on a client-supplied key
(`KitOrder.idempotencyKey`, unique-constrained), so a retried request cannot
double-charge/double-order. If no `starter_kit` row exists for a partner's
category, the gate is treated as not applicable and is skipped automatically
(`KitOrderService#skipIfNotRequired`) — the alternative, an explicit admin
exemption with a recorded reason, exists as
`POST /admin/kit-orders/exempt/{partnerId}`.

## Verification

`VerificationRequest.provider` is either `MANUAL_REVIEW` (the default — an
admin/`REVIEWER` approves or rejects with a reason) or `DEV_SIMULATOR` (local
convenience only, auto-approves on submit). Neither path is ever labelled
"Aadhaar verified" in an API response — only `provider` + `status` are
returned, and the frontend's copy is written off that pairing rather than
assuming manual review implies government verification, per the brief's
explicit warning against that.

## Screening

Capacity is enforced by a single conditional `UPDATE ... WHERE booked_count <
capacity` (`ScreeningSlotRepository#tryReserveSeat`), not a read-then-write,
so two simultaneous booking attempts on the last seat cannot both succeed.
Reschedule composes release-old-slot + reserve-new-slot in one transaction. A
check-in only records attendance *intent* (`ScreeningBooking.status =
CHECKED_IN`); only a subsequent, separate `REVIEWER`/`ADMIN` decision
(`POST /admin/screening/bookings/{id}/decision`) sets the PASS/FAIL outcome —
checking in can never itself imply a pass, per the brief.

## Jobs

There is no real customer-facing app in this build (explicitly out of scope —
this is the *partner* platform). Section 3's "synthetic customers and service
requests" requirement is implemented as admin-created `Customer` /
`CustomerAddress` / `ServiceRequest` rows
(`AdminJobsController`), which is also how completion gets *confirmed*
(`POST /admin/jobs/service-requests/{id}/confirm-completion` stands in for the
brief's "admin/customer-confirmation adapter").

Acceptance is one conditional `UPDATE ... WHERE status='OPEN' AND
assigned_partner_id IS NULL` (`ServiceRequestRepository#tryAssign`) — two
concurrent accepts on the same job produce exactly one winner and the loser
gets a 409. Before assignment, `NearbyJobResponse` deliberately omits customer
name/phone/exact address (only an approximate area/city and the partner's own
estimated earning); `AssignedJobResponse`, returned only for a partner's own
assigned/accepted jobs, includes the full address and customer contact.

## Money

`recordEarningForCompletedJob` is called exactly once, from the completion
confirmation transaction, and is guarded both in code (existence check) and at
the schema level (`UNIQUE` constraint on `earning_entry.service_request_id`)
— a completed job can never post two earning entries even under a retried
request. Reversals are modelled as a status flip on the original entry plus a
separate signed `EarningAdjustment` row (never a destructive delete), so the
ledger stays a complete audit trail. Payout requests in `dev_simulator` mode
complete synchronously and are clearly logged as simulated; the
`/api/v1/webhooks/payouts` endpoint exists for a real provider's async
callback and is idempotent on `provider_event_id` (duplicate delivery is a
no-op), but no real payout provider is wired up.

## Auth & security

Partners authenticate by **phone OTP only** (rate-limited per phone/hour,
resend cooldown, bounded verify attempts, codes stored only as a BCrypt hash,
never logged in plaintext outside the `dev_local` sender). Staff
(`ADMIN`/`REVIEWER`/`TRAINER`/`FINANCE`) authenticate with **username +
BCrypt password**, created only via `AdminBootstrapRunner` from environment
variables at startup (never via public registration, never from seed SQL) or
by an existing `ADMIN` — there is no self-service staff signup endpoint.
Sessions are HttpOnly cookies backed by **Spring Session JDBC**, so they
survive an application restart, with CSRF protection via the standard
cookie+header pair for SPAs (a small `CsrfCookieEagerLoadFilter`, the pattern
from Spring Security's own SPA docs, forces the XSRF-TOKEN cookie onto every
response instead of only once something happens to read the deferred token).

`ProdSafetyConfig` fails the application startup outright, under the `prod`
Spring profile only, if it detects a `dev_local` SMS provider, a non-HTTPS
session cookie, the default admin bootstrap password, or the default webhook
secret still in place.

## Provider adapters (SMS/WhatsApp, identity, payments, payouts)

Every external integration point ships a **working local/demo adapter** and,
for any other configured mode, **fails loudly** rather than silently no-oping
(`ProviderUnavailableException`, mapped to HTTP 503) — see
`OtpSmsSender`, `KitOrderService`/`MoneyService`'s `*_simulator` branches, and
`VerificationService`'s `DEV_SIMULATOR` provider. None of these are real
integrations; wiring an actual SMS/WhatsApp/identity/payment/payout provider
is the explicit extension point this architecture leaves open (one adapter
class per concern, selected by a `supplybase.providers.*.mode` property).

## Notifications

The outbox pattern (`notification_outbox` + `notification_delivery_attempt`)
is real and durable, but only a representative subset of touchpoints is wired
in this build (screening booking confirmation, kit order status changes, job
assigned/completed) rather than exhaustively every event named in the brief's
list (training reminders, verification updates, etc.) — the infrastructure to
add the rest is in place (`NotificationService#enqueue`), just not every call
site.

## What's genuinely tested vs. what isn't

See the README's "What's implemented vs. scaffolded" section and
`docs/MANUAL_QA.md` for the walkthroughs actually exercised. Automated tests
(`backend/src/test`) exist for the highest-risk concurrency/idempotency
behaviors (screening capacity, job acceptance, kit-order idempotency, OTP rate
limiting) but require a Docker engine to run (Testcontainers) and were written
and reviewed, not executed, in this build environment — run `./mvnw test`
yourself to confirm before relying on them.
