-- Partner profile: one row per app_user with role PARTNER. Onboarding stage and
-- activation status are backend-owned state machines -- never client-writable.

CREATE TABLE partner (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT UNSIGNED NOT NULL,
    primary_category_id    BIGINT UNSIGNED NULL,
    residence_city_id      BIGINT UNSIGNED NULL,
    photo_storage_key      VARCHAR(300) NULL,
    experience_years       TINYINT UNSIGNED NULL,
    bio                    VARCHAR(1000) NULL,
    working_hours_choice   VARCHAR(8) NULL,
    onboarding_stage       VARCHAR(32) NOT NULL DEFAULT 'BASIC_DETAILS',
    activation_status      VARCHAR(24) NOT NULL DEFAULT 'NOT_ACTIVE',
    suspended_reason       VARCHAR(500) NULL,
    activated_at           DATETIME(3) NULL,
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_partner_user UNIQUE (user_id),
    CONSTRAINT fk_partner_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_category FOREIGN KEY (primary_category_id) REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT fk_partner_residence_city FOREIGN KEY (residence_city_id) REFERENCES city(id) ON DELETE SET NULL,
    CONSTRAINT chk_partner_hours CHECK (working_hours_choice IS NULL OR working_hours_choice IN ('FOUR','SIX','EIGHT')),
    CONSTRAINT chk_partner_onboarding_stage CHECK (onboarding_stage IN (
        'BASIC_DETAILS','WORK_CATEGORY','CITY','TERMS_PRIVACY','EARNING_POTENTIAL',
        'WORKING_HOURS','PERMISSIONS','VERIFICATION','SCREENING','STARTER_KIT',
        'PROFILE','TRAINING','ACTIVATION_REVIEW','COMPLETE')),
    CONSTRAINT chk_partner_activation_status CHECK (activation_status IN ('NOT_ACTIVE','ACTIVE','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_partner_category ON partner (primary_category_id);
CREATE INDEX ix_partner_city ON partner (residence_city_id);
CREATE INDEX ix_partner_activation ON partner (activation_status);

CREATE TABLE partner_status_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id      BIGINT UNSIGNED NOT NULL,
    from_status     VARCHAR(24) NULL,
    to_status       VARCHAR(24) NOT NULL,
    reason          VARCHAR(500) NULL,
    changed_by_user_id BIGINT UNSIGNED NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_partner_status_history_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_partner_status_history_partner ON partner_status_history (partner_id, created_at);

CREATE TABLE partner_language (
    partner_id     BIGINT UNSIGNED NOT NULL,
    language_code  VARCHAR(8) NOT NULL,
    PRIMARY KEY (partner_id, language_code),
    CONSTRAINT fk_partner_language_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Approved skills beyond the single primary category chosen during onboarding.
CREATE TABLE partner_skill (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id   BIGINT UNSIGNED NOT NULL,
    service_id   BIGINT UNSIGNED NOT NULL,
    approved     TINYINT(1) NOT NULL DEFAULT 0,
    approved_by  BIGINT UNSIGNED NULL,
    approved_at  DATETIME(3) NULL,
    created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_partner_skill UNIQUE (partner_id, service_id),
    CONSTRAINT fk_partner_skill_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_skill_service FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_skill_approver FOREIGN KEY (approved_by) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE partner_availability_window (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id   BIGINT UNSIGNED NOT NULL,
    day_of_week  TINYINT UNSIGNED NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    CONSTRAINT fk_partner_availability_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_dow CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT chk_availability_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_partner_availability_partner ON partner_availability_window (partner_id);

-- Service-area coverage used for job matching. Distinct from residence city.
CREATE TABLE partner_coverage (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id  BIGINT UNSIGNED NOT NULL,
    area_id     BIGINT UNSIGNED NOT NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_partner_coverage UNIQUE (partner_id, area_id),
    CONSTRAINT fk_partner_coverage_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_coverage_area FOREIGN KEY (area_id) REFERENCES area(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_partner_coverage_area ON partner_coverage (area_id);

-- Bank details for payouts. Only a masked display value and syntactic validation live here;
-- a real deployment would delegate true account verification to a provider adapter.
CREATE TABLE bank_account (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id             BIGINT UNSIGNED NOT NULL,
    account_holder_name    VARCHAR(120) NOT NULL,
    account_number_masked  VARCHAR(40) NOT NULL,
    account_number_hash    VARCHAR(255) NOT NULL,
    ifsc                   VARCHAR(11) NOT NULL,
    bank_name              VARCHAR(120) NULL,
    verification_status    VARCHAR(24) NOT NULL DEFAULT 'UNVERIFIED',
    verified_at            DATETIME(3) NULL,
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_bank_account_partner UNIQUE (partner_id),
    CONSTRAINT fk_bank_account_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_verification_status CHECK (verification_status IN ('UNVERIFIED','FORMAT_VALID','PROVIDER_VERIFIED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Partner acceptance of a specific policy_document version.
CREATE TABLE partner_consent (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id          BIGINT UNSIGNED NOT NULL,
    policy_document_id  BIGINT UNSIGNED NOT NULL,
    accepted            TINYINT(1) NOT NULL DEFAULT 1,
    accepted_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ip_address          VARCHAR(64) NULL,
    CONSTRAINT uq_partner_consent UNIQUE (partner_id, policy_document_id),
    CONSTRAINT fk_partner_consent_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_consent_policy FOREIGN KEY (policy_document_id) REFERENCES policy_document(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
