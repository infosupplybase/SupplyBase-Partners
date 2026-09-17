-- Synthetic customer-side data (this platform has no real customer app yet):
-- admin-created customers/addresses/service requests drive a usable local job
-- lifecycle. `service_request` carries its own status + assignment pointer so
-- acceptance can be a single atomic conditional UPDATE.

CREATE TABLE customer (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    phone_e164  VARCHAR(16) NOT NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customer_address (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT UNSIGNED NOT NULL,
    line1       VARCHAR(300) NOT NULL,
    line2       VARCHAR(300) NULL,
    area_id     BIGINT UNSIGNED NOT NULL,
    lat         DECIMAL(9,6) NULL,
    lng         DECIMAL(9,6) NULL,
    CONSTRAINT fk_customer_address_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_address_area FOREIGN KEY (area_id) REFERENCES area(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_request (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id           BIGINT UNSIGNED NOT NULL,
    service_id            BIGINT UNSIGNED NOT NULL,
    address_id            BIGINT UNSIGNED NOT NULL,
    scheduled_at          DATETIME(3) NOT NULL,
    notes                 VARCHAR(1000) NULL,
    job_value_paise       BIGINT UNSIGNED NOT NULL,
    partner_earning_paise BIGINT UNSIGNED NOT NULL,
    status                VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    assigned_partner_id   BIGINT UNSIGNED NULL,
    created_by_admin_id   BIGINT UNSIGNED NOT NULL,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_request_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_request_service FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_request_address FOREIGN KEY (address_id) REFERENCES customer_address(id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_request_partner FOREIGN KEY (assigned_partner_id) REFERENCES partner(id) ON DELETE SET NULL,
    CONSTRAINT fk_service_request_admin FOREIGN KEY (created_by_admin_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_service_request_status CHECK (status IN (
        'OPEN','ASSIGNED','IN_PROGRESS','COMPLETION_SUBMITTED','COMPLETED','CANCELLED','DISPUTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_service_request_status ON service_request (status, scheduled_at);
CREATE INDEX ix_service_request_partner ON service_request (assigned_partner_id, status);
CREATE INDEX ix_service_request_service ON service_request (service_id);

CREATE TABLE job_status_history (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    service_request_id BIGINT UNSIGNED NOT NULL,
    from_status        VARCHAR(24) NULL,
    to_status          VARCHAR(24) NOT NULL,
    actor_user_id      BIGINT UNSIGNED NULL,
    reason             VARCHAR(500) NULL,
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_job_status_history_request FOREIGN KEY (service_request_id) REFERENCES service_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_status_history_actor FOREIGN KEY (actor_user_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_job_status_history_request ON job_status_history (service_request_id, created_at);

CREATE TABLE job_proof_media (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    service_request_id  BIGINT UNSIGNED NOT NULL,
    storage_key         VARCHAR(300) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    uploaded_by_partner_id BIGINT UNSIGNED NOT NULL,
    uploaded_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_job_proof_media_request FOREIGN KEY (service_request_id) REFERENCES service_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_proof_media_partner FOREIGN KEY (uploaded_by_partner_id) REFERENCES partner(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
