# Entity-relationship diagram

Grouped by domain for readability (the full schema is ~50 tables across
`backend/src/main/resources/db/migration/V1..V10`). Every cross-domain
reference is a plain FK `Long` column, not a JPA object association — see
`docs/ASSUMPTIONS.md` for why packages are wired that way.

## Identity & auth

```mermaid
erDiagram
    app_user ||--o{ user_role : has
    app_user ||--o{ otp_challenge : "verifies via (by phone)"

    app_user {
        bigint id PK
        varchar phone_e164 UK
        varchar username UK "staff only"
        varchar password_hash "staff only"
        varchar status
    }
    user_role {
        bigint user_id FK
        varchar role
    }
    otp_challenge {
        bigint id PK
        varchar phone_e164
        varchar code_hash
        int attempts
        datetime expires_at
    }
```

## Catalog

```mermaid
erDiagram
    category ||--o{ service : contains
    service ||--|| service_requirement : configures
    city ||--o{ area : contains
    category ||--o{ category_city_estimate : "estimate for"
    city ||--o{ category_city_estimate : "estimate for"

    category { bigint id PK }
    service { bigint id PK; bigint category_id FK }
    service_requirement { bigint service_id PK,FK; boolean starter_kit_required; bigint required_course_id FK }
    city { bigint id PK }
    area { bigint id PK; bigint city_id FK }
    category_city_estimate { bigint id PK; varchar hours_choice; bigint estimated_monthly_paise }
    policy_document { bigint id PK; varchar type; varchar version; boolean is_current }
```

## Partner core

```mermaid
erDiagram
    app_user ||--|| partner : "is a"
    partner ||--o{ partner_status_history : logs
    partner ||--o{ partner_language : speaks
    partner ||--o{ partner_skill : "approved for"
    partner ||--o{ partner_availability_window : "available during"
    partner ||--o{ partner_coverage : covers
    partner ||--o| bank_account : "paid via"
    partner ||--o{ partner_consent : accepted
    partner_consent }o--|| policy_document : references
    partner_coverage }o--|| area : references

    partner {
        bigint id PK
        bigint user_id FK,UK
        bigint primary_category_id FK
        bigint residence_city_id FK
        varchar onboarding_stage
        varchar activation_status
        bigint version "optimistic lock"
    }
```

## Verification & screening

```mermaid
erDiagram
    partner ||--o{ identity_document : uploaded
    partner ||--|| verification_request : "reviewed via"
    screening_slot ||--o{ screening_booking : "booked as"
    partner ||--o{ screening_booking : books
    screening_booking ||--o{ screening_booking_history : logs

    verification_request { bigint id PK; bigint partner_id FK,UK; varchar provider; varchar status }
    screening_slot { bigint id PK; int capacity; int booked_count; bigint version }
    screening_booking { bigint id PK; bigint slot_id FK; bigint partner_id FK; varchar status; varchar outcome }
```

## Starter kit

```mermaid
erDiagram
    category ||--o{ starter_kit : "kit for"
    starter_kit ||--o{ starter_kit_item : contains
    starter_kit ||--o{ kit_order : ordered_as
    partner ||--o{ kit_order : places
    kit_order ||--o{ kit_order_event : logs

    kit_order { bigint id PK; bigint partner_id FK; bigint kit_id FK; varchar idempotency_key UK; varchar status }
```

## Training

```mermaid
erDiagram
    training_course ||--o{ training_module : contains
    training_course ||--o{ assessment : "tested by"
    assessment ||--o{ assessment_question : contains
    assessment_question ||--o{ assessment_option : offers
    partner ||--o{ training_enrollment : enrolls
    training_enrollment ||--o{ module_progress : tracks
    partner ||--o{ assessment_attempt : attempts
    assessment_attempt ||--o{ assessment_answer : records
```

## Jobs

```mermaid
erDiagram
    customer ||--o{ customer_address : has
    customer ||--o{ service_request : requests
    service ||--o{ service_request : "is for"
    customer_address ||--o{ service_request : "located at"
    partner ||--o{ service_request : "assigned to (0..1 active)"
    service_request ||--o{ job_status_history : logs
    service_request ||--o{ job_proof_media : "proven by"

    service_request {
        bigint id PK
        varchar status
        bigint assigned_partner_id FK
        bigint job_value_paise
        bigint partner_earning_paise
        bigint version "optimistic lock"
    }
```

## Money

```mermaid
erDiagram
    service_request ||--o| earning_entry : "earns (exactly once)"
    partner ||--o{ earning_entry : earns
    partner ||--o{ earning_adjustment : adjusted
    earning_entry ||--o{ earning_adjustment : "reversed by"
    partner ||--o{ payout : requests
    bank_account ||--o{ payout : "paid to"
    payout_batch ||--o{ payout : batches
    payout ||--o{ payout_event : logs

    earning_entry { bigint id PK; bigint service_request_id FK,UK; bigint net_paise; varchar status }
    payout { bigint id PK; bigint partner_id FK; bigint amount_paise; varchar status }
```

## Community, support, notifications, audit

```mermaid
erDiagram
    partner ||--o{ community_post : writes
    community_post ||--o{ community_comment : has
    partner ||--o{ community_comment : writes
    partner ||--o{ community_report : files
    partner ||--o{ support_ticket : opens
    support_ticket ||--o{ support_ticket_message : contains
    app_user ||--o{ notification_outbox : "notified via"
    notification_outbox ||--o{ notification_delivery_attempt : logs
    app_user ||--o{ audit_log : "acted (as actor)"
```
