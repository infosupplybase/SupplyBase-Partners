-- Service catalog: categories/services, cities/areas, and configurable earning estimates.
-- Managed through the admin app; the provisional seed set lives in a separate dev-only
-- Flyway location (db/seed), never in this production schema migration.

CREATE TABLE category (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    slug           VARCHAR(140) NOT NULL,
    icon_url       VARCHAR(500) NULL,
    image_url      VARCHAR(500) NULL,
    display_order  INT NOT NULL DEFAULT 0,
    active         TINYINT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_category_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id    BIGINT UNSIGNED NOT NULL,
    name           VARCHAR(120) NOT NULL,
    slug           VARCHAR(140) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    active         TINYINT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_service_slug UNIQUE (slug),
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_service_category ON service (category_id);

-- Whether a starter kit is required before a partner offering this service can activate,
-- and the training course that must be passed. Both are admin-configurable per service.
-- required_course_id references training_course(id), added by V9 once that table exists.
CREATE TABLE service_requirement (
    service_id           BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    starter_kit_required  TINYINT(1) NOT NULL DEFAULT 1,
    required_course_id    BIGINT UNSIGNED NULL,
    updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_service_requirement_service FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE city (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    state       VARCHAR(120) NOT NULL,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_city_name_state UNIQUE (name, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE area (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    city_id     BIGINT UNSIGNED NOT NULL,
    name        VARCHAR(120) NOT NULL,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_area_city FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_area_city ON area (city_id);

-- Configurable "earning potential" estimate shown during onboarding, per category/city/hours.
-- Always rendered as a labelled demo estimate, never a guarantee.
CREATE TABLE category_city_estimate (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id             BIGINT UNSIGNED NOT NULL,
    city_id                 BIGINT UNSIGNED NOT NULL,
    hours_choice            VARCHAR(8) NOT NULL,
    estimated_monthly_paise BIGINT UNSIGNED NOT NULL,
    assumptions_text        VARCHAR(500) NOT NULL,
    active                  TINYINT(1) NOT NULL DEFAULT 1,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_estimate_scope UNIQUE (category_id, city_id, hours_choice),
    CONSTRAINT fk_estimate_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE,
    CONSTRAINT fk_estimate_city FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE CASCADE,
    CONSTRAINT chk_estimate_hours CHECK (hours_choice IN ('FOUR','SIX','EIGHT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Versioned terms/privacy/marketing-consent documents. Partners accept a specific version;
-- acceptance timestamps are permanent audit records.
CREATE TABLE policy_document (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type              VARCHAR(24) NOT NULL,
    version           VARCHAR(32) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    content_markdown  MEDIUMTEXT NOT NULL,
    is_current        TINYINT(1) NOT NULL DEFAULT 0,
    published_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_policy_type_version UNIQUE (type, version),
    CONSTRAINT chk_policy_type CHECK (type IN ('TERMS','PRIVACY','MARKETING_CONSENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_policy_document_current ON policy_document (type, is_current);
