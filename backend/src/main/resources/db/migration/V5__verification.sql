-- Private identity document metadata (never the file bytes -- those sit in
-- authorization-gated object storage, addressed here only by storage_key) and
-- the verification workflow. A manually approved document is status APPROVED
-- via provider MANUAL_REVIEW; it must never be presented to partners as
-- "Aadhaar verified" unless provider = a real configured identity provider.

CREATE TABLE identity_document (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id         BIGINT UNSIGNED NOT NULL,
    doc_type           VARCHAR(24) NOT NULL,
    storage_key        VARCHAR(300) NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    size_bytes         BIGINT UNSIGNED NOT NULL,
    masked_reference   VARCHAR(64) NULL,
    uploaded_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_identity_document_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_identity_doc_type CHECK (doc_type IN ('AADHAAR','PAN','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_identity_document_partner ON identity_document (partner_id);

CREATE TABLE verification_request (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id     BIGINT UNSIGNED NOT NULL,
    provider       VARCHAR(24) NOT NULL DEFAULT 'MANUAL_REVIEW',
    status         VARCHAR(24) NOT NULL DEFAULT 'NOT_STARTED',
    reviewer_user_id BIGINT UNSIGNED NULL,
    reason         VARCHAR(500) NULL,
    submitted_at   DATETIME(3) NULL,
    decided_at     DATETIME(3) NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_verification_request_partner UNIQUE (partner_id),
    CONSTRAINT fk_verification_request_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_request_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_verification_provider CHECK (provider IN ('DEV_SIMULATOR','MANUAL_REVIEW')),
    CONSTRAINT chk_verification_status CHECK (status IN ('NOT_STARTED','SUBMITTED','PENDING_REVIEW','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Screening / skill sessions. capacity vs booked_count is enforced transactionally
-- in the service layer via a conditional UPDATE; `version` backs optimistic locking
-- for reschedule (release old slot, reserve new slot) as one atomic operation.
CREATE TABLE screening_slot (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id           BIGINT UNSIGNED NOT NULL,
    city_id               BIGINT UNSIGNED NOT NULL,
    mode                  VARCHAR(16) NOT NULL,
    venue_or_link         VARCHAR(500) NOT NULL,
    directions_text        VARCHAR(500) NULL,
    starts_at             DATETIME(3) NOT NULL,
    ends_at               DATETIME(3) NOT NULL,
    check_in_opens_at     DATETIME(3) NOT NULL,
    check_in_closes_at    DATETIME(3) NOT NULL,
    capacity              INT UNSIGNED NOT NULL,
    booked_count          INT UNSIGNED NOT NULL DEFAULT 0,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_screening_slot_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT,
    CONSTRAINT fk_screening_slot_city FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE RESTRICT,
    CONSTRAINT chk_screening_slot_mode CHECK (mode IN ('IN_PERSON','VIRTUAL')),
    CONSTRAINT chk_screening_slot_capacity CHECK (booked_count <= capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_screening_slot_scope ON screening_slot (category_id, city_id, starts_at);

CREATE TABLE screening_booking (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    slot_id            BIGINT UNSIGNED NOT NULL,
    partner_id         BIGINT UNSIGNED NOT NULL,
    status             VARCHAR(24) NOT NULL DEFAULT 'BOOKED',
    checked_in_at      DATETIME(3) NULL,
    outcome            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    outcome_reason     VARCHAR(500) NULL,
    decided_by_user_id BIGINT UNSIGNED NULL,
    decided_at         DATETIME(3) NULL,
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_screening_booking_slot FOREIGN KEY (slot_id) REFERENCES screening_slot(id) ON DELETE RESTRICT,
    CONSTRAINT fk_screening_booking_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_booking_decider FOREIGN KEY (decided_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_screening_booking_status CHECK (status IN ('BOOKED','RESCHEDULED','CANCELLED','CHECKED_IN','COMPLETED')),
    CONSTRAINT chk_screening_booking_outcome CHECK (outcome IN ('PENDING','PASSED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Only one active (non-cancelled/non-rescheduled-away) booking per partner at a time
-- is enforced in the service layer since MySQL cannot express a partial unique index;
-- history is preserved by inserting new rows on reschedule rather than mutating slot_id.
CREATE INDEX ix_screening_booking_partner ON screening_booking (partner_id, status);
CREATE INDEX ix_screening_booking_slot ON screening_booking (slot_id);

CREATE TABLE screening_booking_history (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id   BIGINT UNSIGNED NOT NULL,
    event        VARCHAR(32) NOT NULL,
    from_slot_id BIGINT UNSIGNED NULL,
    to_slot_id   BIGINT UNSIGNED NULL,
    created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_screening_history_booking FOREIGN KEY (booking_id) REFERENCES screening_booking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
