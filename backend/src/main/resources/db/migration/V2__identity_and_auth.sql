-- Core identity: platform users (partners + staff) and phone-OTP auth challenges.

-- Partners authenticate by phone OTP only (username/password_hash stay NULL).
-- Staff (ADMIN/REVIEWER/TRAINER/FINANCE) authenticate with username + a BCrypt
-- password_hash; staff accounts are seeded/created by an existing ADMIN, never
-- through public registration.
CREATE TABLE app_user (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_e164          VARCHAR(16)  NOT NULL,
    name                VARCHAR(120) NULL,
    preferred_language  VARCHAR(8)   NOT NULL DEFAULT 'en',
    username            VARCHAR(60)  NULL,
    password_hash       VARCHAR(100) NULL,
    status              VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_app_user_phone UNIQUE (phone_e164),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A user can hold multiple roles (a partner is always PARTNER; staff get ADMIN/REVIEWER/TRAINER/FINANCE).
-- Never assignable through public registration -- only seeded or granted by an existing ADMIN via /api/v1/admin.
CREATE TABLE user_role (
    user_id     BIGINT UNSIGNED NOT NULL,
    role        VARCHAR(24) NOT NULL,
    granted_by  BIGINT UNSIGNED NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_granter FOREIGN KEY (granted_by) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_user_role_role CHECK (role IN ('PARTNER','ADMIN','REVIEWER','TRAINER','FINANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Phone OTP challenges. Codes are never stored in plaintext -- only a salted hash.
CREATE TABLE otp_challenge (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_e164            VARCHAR(16)  NOT NULL,
    purpose               VARCHAR(24)  NOT NULL,
    channel               VARCHAR(16)  NOT NULL,
    code_hash             VARCHAR(255) NOT NULL,
    attempts              INT UNSIGNED NOT NULL DEFAULT 0,
    max_attempts          INT UNSIGNED NOT NULL DEFAULT 5,
    expires_at            DATETIME(3)  NOT NULL,
    consumed_at           DATETIME(3)  NULL,
    resend_available_at   DATETIME(3)  NOT NULL,
    requester_ip          VARCHAR(64)  NULL,
    created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('LOGIN_OR_SIGNUP')),
    CONSTRAINT chk_otp_channel CHECK (channel IN ('SMS','WHATSAPP','DEV_LOCAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_otp_challenge_phone_created ON otp_challenge (phone_e164, created_at);
CREATE INDEX ix_otp_challenge_expires ON otp_challenge (expires_at);
