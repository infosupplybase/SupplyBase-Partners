-- Earnings ledger. All INR amounts are integer paise (never floating point).
-- earning_entry has a unique constraint on service_request_id so the
-- completion-triggered write can only ever happen once per job, even under
-- retried requests or duplicate event delivery.

CREATE TABLE earning_entry (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id          BIGINT UNSIGNED NOT NULL,
    service_request_id  BIGINT UNSIGNED NOT NULL,
    gross_paise         BIGINT NOT NULL,
    commission_paise    BIGINT NOT NULL,
    net_paise           BIGINT NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'EARNED',
    earned_at           DATETIME(3) NOT NULL,
    available_at        DATETIME(3) NOT NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_earning_entry_request UNIQUE (service_request_id),
    CONSTRAINT fk_earning_entry_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_earning_entry_request FOREIGN KEY (service_request_id) REFERENCES service_request(id) ON DELETE CASCADE,
    CONSTRAINT chk_earning_entry_status CHECK (status IN ('EARNED','REVERSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_earning_entry_partner ON earning_entry (partner_id, earned_at);

CREATE TABLE earning_adjustment (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id       BIGINT UNSIGNED NOT NULL,
    earning_entry_id BIGINT UNSIGNED NULL,
    type             VARCHAR(16) NOT NULL,
    amount_paise     BIGINT NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    created_by_user_id BIGINT UNSIGNED NOT NULL,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_earning_adjustment_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_earning_adjustment_entry FOREIGN KEY (earning_entry_id) REFERENCES earning_entry(id) ON DELETE SET NULL,
    CONSTRAINT fk_earning_adjustment_actor FOREIGN KEY (created_by_user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_earning_adjustment_type CHECK (type IN ('REVERSAL','BONUS','PENALTY','DEDUCTION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_earning_adjustment_partner ON earning_adjustment (partner_id, created_at);

CREATE TABLE payout_batch (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id BIGINT UNSIGNED NOT NULL,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_payout_batch_actor FOREIGN KEY (created_by_user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payout_batch_status CHECK (status IN ('DRAFT','PROCESSING','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payout (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id        BIGINT UNSIGNED NOT NULL,
    payout_batch_id   BIGINT UNSIGNED NULL,
    bank_account_id   BIGINT UNSIGNED NOT NULL,
    amount_paise      BIGINT UNSIGNED NOT NULL,
    status            VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    provider          VARCHAR(24) NOT NULL DEFAULT 'DEV_SIMULATOR',
    provider_reference VARCHAR(120) NULL,
    failure_reason    VARCHAR(500) NULL,
    requested_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at           DATETIME(3) NULL,
    CONSTRAINT fk_payout_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_payout_batch FOREIGN KEY (payout_batch_id) REFERENCES payout_batch(id) ON DELETE SET NULL,
    CONSTRAINT fk_payout_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payout_status CHECK (status IN ('REQUESTED','SCHEDULED','PROCESSING','PAID','FAILED','MANUAL_RECONCILED')),
    CONSTRAINT chk_payout_provider CHECK (provider IN ('DEV_SIMULATOR','MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_payout_partner ON payout (partner_id, status);

-- provider_event_id uniqueness makes duplicate payout webhook delivery a no-op.
CREATE TABLE payout_event (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payout_id         BIGINT UNSIGNED NOT NULL,
    event_type        VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(120) NULL,
    payload_json      JSON NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_payout_event_payout FOREIGN KEY (payout_id) REFERENCES payout(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE UNIQUE INDEX uq_payout_event_provider_id ON payout_event (provider_event_id);
