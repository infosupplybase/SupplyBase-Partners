-- Community (SupplyBase's implementation of the reference's vaguely described
-- "Cult" section), support tickets, the notification outbox, and audit logs.

CREATE TABLE announcement (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    body               MEDIUMTEXT NOT NULL,
    published_by_user_id BIGINT UNSIGNED NOT NULL,
    published_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    active             TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_announcement_publisher FOREIGN KEY (published_by_user_id) REFERENCES app_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE community_post (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id  BIGINT UNSIGNED NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_community_post_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_community_post_status CHECK (status IN ('VISIBLE','HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_community_post_status ON community_post (status, created_at);

CREATE TABLE community_comment (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT UNSIGNED NOT NULL,
    partner_id  BIGINT UNSIGNED NOT NULL,
    body        VARCHAR(1000) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'VISIBLE',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_community_comment_post FOREIGN KEY (post_id) REFERENCES community_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comment_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_community_comment_status CHECK (status IN ('VISIBLE','HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_community_comment_post ON community_comment (post_id, created_at);

CREATE TABLE community_report (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    target_type         VARCHAR(16) NOT NULL,
    target_id           BIGINT UNSIGNED NOT NULL,
    reported_by_partner_id BIGINT UNSIGNED NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_community_report_reporter FOREIGN KEY (reported_by_partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT chk_community_report_target_type CHECK (target_type IN ('POST','COMMENT')),
    CONSTRAINT chk_community_report_status CHECK (status IN ('OPEN','REVIEWED','DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE support_ticket (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id      BIGINT UNSIGNED NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    description     VARCHAR(2000) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    assigned_to_user_id BIGINT UNSIGNED NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_support_ticket_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_ticket_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_support_ticket_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_support_ticket_partner ON support_ticket (partner_id, status);

CREATE TABLE support_ticket_message (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ticket_id      BIGINT UNSIGNED NOT NULL,
    sender_user_id BIGINT UNSIGNED NOT NULL,
    body           VARCHAR(2000) NOT NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_support_ticket_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_ticket_message_sender FOREIGN KEY (sender_user_id) REFERENCES app_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Outbox pattern for SMS/WhatsApp/push/email/in-app delivery, with retry/backoff
-- tracked via attempts. Local mode records previews without sending anything real.
CREATE TABLE notification_outbox (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT UNSIGNED NULL,
    channel       VARCHAR(16) NOT NULL,
    template_key  VARCHAR(80) NOT NULL,
    language      VARCHAR(8) NOT NULL DEFAULT 'en',
    payload_json  JSON NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
    attempts      INT UNSIGNED NOT NULL DEFAULT 0,
    last_attempt_at DATETIME(3) NULL,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_notification_outbox_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_notification_outbox_channel CHECK (channel IN ('SMS','WHATSAPP','PUSH','EMAIL','INAPP')),
    CONSTRAINT chk_notification_outbox_status CHECK (status IN ('QUEUED','SENT','DELIVERED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_notification_outbox_user ON notification_outbox (user_id, created_at);
CREATE INDEX ix_notification_outbox_status ON notification_outbox (status);

CREATE TABLE notification_delivery_attempt (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    outbox_id           BIGINT UNSIGNED NOT NULL,
    attempt_number      INT UNSIGNED NOT NULL,
    provider             VARCHAR(24) NOT NULL DEFAULT 'DEV_LOCAL',
    status                VARCHAR(16) NOT NULL,
    provider_message_id  VARCHAR(120) NULL,
    error_message         VARCHAR(500) NULL,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_notification_attempt_outbox FOREIGN KEY (outbox_id) REFERENCES notification_outbox(id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_attempt_status CHECK (status IN ('SUCCESS','FAILURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_read_state (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    outbox_id   BIGINT UNSIGNED NOT NULL,
    read_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_notification_read_state UNIQUE (user_id, outbox_id),
    CONSTRAINT fk_notification_read_state_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_read_state_outbox FOREIGN KEY (outbox_id) REFERENCES notification_outbox(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Who did what sensitive action, when, and why. Before/after values are stored
-- as JSON with sensitive fields masked by the application before insert.
CREATE TABLE audit_log (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_user_id   BIGINT UNSIGNED NULL,
    action          VARCHAR(80) NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       VARCHAR(80) NOT NULL,
    before_json     JSON NULL,
    after_json      JSON NULL,
    reason          VARCHAR(500) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_audit_log_entity ON audit_log (entity_type, entity_id, created_at);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_user_id, created_at);
