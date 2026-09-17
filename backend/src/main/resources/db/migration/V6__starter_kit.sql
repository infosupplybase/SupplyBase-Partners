-- Starter kits: category-specific required items, sold to the partner after
-- screening passes. Price/order snapshots are frozen at order time so later
-- catalog price changes never rewrite historical orders.

CREATE TABLE starter_kit (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT UNSIGNED NOT NULL,
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(1000) NULL,
    price_paise  BIGINT UNSIGNED NOT NULL,
    fees_paise   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    terms_text   VARCHAR(2000) NOT NULL,
    active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_starter_kit_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_starter_kit_category ON starter_kit (category_id);

CREATE TABLE starter_kit_item (
    id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kit_id    BIGINT UNSIGNED NOT NULL,
    name      VARCHAR(150) NOT NULL,
    quantity  INT UNSIGNED NOT NULL DEFAULT 1,
    CONSTRAINT fk_starter_kit_item_kit FOREIGN KEY (kit_id) REFERENCES starter_kit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE kit_order (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id             BIGINT UNSIGNED NOT NULL,
    kit_id                 BIGINT UNSIGNED NOT NULL,
    delivery_address_line  VARCHAR(500) NOT NULL,
    delivery_city_id       BIGINT UNSIGNED NOT NULL,
    price_paise            BIGINT UNSIGNED NOT NULL,
    fees_paise             BIGINT UNSIGNED NOT NULL,
    total_paise            BIGINT UNSIGNED NOT NULL,
    status                 VARCHAR(24) NOT NULL DEFAULT 'QUOTE',
    payment_provider       VARCHAR(24) NOT NULL DEFAULT 'DEV_SIMULATOR',
    idempotency_key        VARCHAR(80) NOT NULL,
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_kit_order_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_kit_order_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_kit_order_kit FOREIGN KEY (kit_id) REFERENCES starter_kit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_kit_order_delivery_city FOREIGN KEY (delivery_city_id) REFERENCES city(id) ON DELETE RESTRICT,
    CONSTRAINT chk_kit_order_status CHECK (status IN (
        'QUOTE','PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED','PAYMENT_FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_kit_order_partner ON kit_order (partner_id, status);

-- Every payment/fulfilment transition, keyed by provider_event_id for idempotent
-- webhook handling -- a duplicate delivery of the same event is a no-op.
CREATE TABLE kit_order_event (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kit_order_id      BIGINT UNSIGNED NOT NULL,
    event_type        VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(120) NULL,
    payload_json      JSON NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_kit_order_event_order FOREIGN KEY (kit_order_id) REFERENCES kit_order(id) ON DELETE CASCADE,
    CONSTRAINT chk_kit_order_event_type CHECK (event_type IN (
        'CREATED','PAYMENT_INITIATED','PAYMENT_SUCCEEDED','PAYMENT_FAILED','SHIPPED','DELIVERED','CANCELLED','REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE UNIQUE INDEX uq_kit_order_event_provider_id ON kit_order_event (provider_event_id);
CREATE INDEX ix_kit_order_event_order ON kit_order_event (kit_order_id, created_at);
