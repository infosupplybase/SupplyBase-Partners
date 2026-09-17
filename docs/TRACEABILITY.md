# Reference-to-implementation traceability

Maps the PDF brief's sections to what actually exists in this repo. "PDF section"
quotes the brief's own lettering/numbering; nothing here claims a pixel-perfect
recreation of an unseen screenshot — see `docs/ASSUMPTIONS.md` for the design
decisions made where the PDF only described behavior, not layout.

| PDF section | Frontend | Backend |
|---|---|---|
| A. Discover/join | `frontend/src/pages/Welcome.jsx` | — |
| B. Basic details + sign-in (OTP) | `pages/OtpAuth.jsx`, `pages/onboarding/BasicDetails.jsx` | `identity/AuthController`, `OtpService`, `OtpSmsSender` |
| C. Work category | `pages/onboarding/WorkCategory.jsx` | `catalog/CatalogController` (categories), `partner/PartnersMeController#category` |
| D. City | `pages/onboarding/City.jsx` | `catalog/CatalogController` (cities/areas), `PartnersMeController#city` |
| E. Terms and privacy | `pages/onboarding/TermsPrivacy.jsx` | `catalog/PolicyDocument`, `PartnersMeController#consents` |
| F. Earning potential | `pages/onboarding/EarningPotential.jsx` | `catalog/CategoryCityEstimate`, `CatalogController#estimate` |
| G. Working hours | `pages/onboarding/WorkingHours.jsx` | `PartnersMeController#workingHours` |
| H. Permissions | `pages/onboarding/Permissions.jsx` (browser geolocation/camera APIs) | `PartnersMeController#permissionsAck` (no persisted permission state — see ASSUMPTIONS) |
| I. Verification | `pages/dashboard/VerificationPage.jsx` | `verification/*` (submit, admin decide, document download) |
| J. Screening/skill session | `pages/dashboard/ScreeningPage.jsx` | `screening/*` (slots, booking, reschedule, check-in, admin outcome) |
| K. Progress stages (Session \| Starter Kit \| Profile \| Training) | `pages/dashboard/ProgressTab.jsx` (tiles) | `PartnerService` onboarding-stage state machine |
| L. Starter kit | `pages/dashboard/StarterKitPage.jsx` | `kit/*` (quote, book, idempotent payment sim, admin fulfillment) |
| M. Profile | `pages/dashboard/ProfilePage.jsx` | `PartnersMeController` (profile, coverage, bank account) |
| N. Training and activation | `pages/dashboard/TrainingPage.jsx` | `training/*`, `partner/ActivationService` |
| §4 Dashboard (Progress \| Money \| Around You \| Community) | `components/AppShell.jsx` + 4 tab pages | — |
| §4 Around You / jobs | `pages/dashboard/AroundYouTab.jsx`, `JobDetailPage.jsx` | `jobs/*` (matching, atomic accept, lifecycle) |
| §4 Money | `pages/dashboard/MoneyTab.jsx` | `money/*` (ledger, summary, payouts) |
| §4 Community | `pages/dashboard/CommunityTab.jsx` | `community/*` |
| §5 Admin application | `pages/admin/*` (login, dashboard, partners, verification, support, community, audit logs) | `admin/*` + each domain's `Admin*Controller` |
| §6 API contract | — | `docs/openapi.yaml` |
| §7 Auth/integrations | — | `config/SecurityConfig`, `identity/OtpSmsSender`, `verification/VerificationService`, `kit/KitOrderService`, `money/MoneyService`, `webhooks/*` |

## Explicitly not implemented (see ASSUMPTIONS.md for why)

- A real customer-facing app (out of scope — this is the partner platform;
  `admin/jobs/*` creates synthetic requests instead).
- Real SMS/WhatsApp, identity-verification, payment, or payout provider
  integrations (working `dev_local`/`dev_simulator` adapters + documented
  extension points instead).
- A live map view on Around You (list view only; an honest "map not available"
  note is shown rather than a broken map).
- Dedicated admin screens for catalog/training/starter-kit authoring and
  screening-slot creation (the REST endpoints exist and were exercised
  directly; no frontend page wraps them yet).
