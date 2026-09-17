-- Training courses/modules/assessments. Server-side progress only -- opening a
-- lesson page is never treated as "watched"; module_progress is written by
-- explicit progress-report calls, and assessment results are graded server-side.

CREATE TABLE training_course (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT UNSIGNED NULL,
    title        VARCHAR(200) NOT NULL,
    description  VARCHAR(1000) NULL,
    required     TINYINT(1) NOT NULL DEFAULT 1,
    active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_training_course_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE service_requirement
    ADD CONSTRAINT fk_service_requirement_course FOREIGN KEY (required_course_id) REFERENCES training_course(id) ON DELETE SET NULL;

CREATE TABLE training_module (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    course_id      BIGINT UNSIGNED NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content_type   VARCHAR(16) NOT NULL,
    content_body   MEDIUMTEXT NULL,
    content_url    VARCHAR(500) NULL,
    display_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_training_module_course FOREIGN KEY (course_id) REFERENCES training_course(id) ON DELETE CASCADE,
    CONSTRAINT chk_training_module_content_type CHECK (content_type IN ('TEXT','VIDEO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_training_module_course ON training_module (course_id, display_order);

CREATE TABLE training_enrollment (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id   BIGINT UNSIGNED NOT NULL,
    course_id    BIGINT UNSIGNED NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    started_at   DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    CONSTRAINT uq_training_enrollment UNIQUE (partner_id, course_id),
    CONSTRAINT fk_training_enrollment_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_enrollment_course FOREIGN KEY (course_id) REFERENCES training_course(id) ON DELETE CASCADE,
    CONSTRAINT chk_training_enrollment_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE module_progress (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    enrollment_id  BIGINT UNSIGNED NOT NULL,
    module_id      BIGINT UNSIGNED NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    completed_at   DATETIME(3) NULL,
    CONSTRAINT uq_module_progress UNIQUE (enrollment_id, module_id),
    CONSTRAINT fk_module_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES training_enrollment(id) ON DELETE CASCADE,
    CONSTRAINT fk_module_progress_module FOREIGN KEY (module_id) REFERENCES training_module(id) ON DELETE CASCADE,
    CONSTRAINT chk_module_progress_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    course_id          BIGINT UNSIGNED NOT NULL,
    title              VARCHAR(200) NOT NULL,
    pass_score_percent TINYINT UNSIGNED NOT NULL DEFAULT 70,
    max_attempts       INT UNSIGNED NOT NULL DEFAULT 3,
    CONSTRAINT fk_assessment_course FOREIGN KEY (course_id) REFERENCES training_course(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment_question (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    assessment_id  BIGINT UNSIGNED NOT NULL,
    question_text  VARCHAR(1000) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assessment_question_assessment FOREIGN KEY (assessment_id) REFERENCES assessment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment_option (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    question_id   BIGINT UNSIGNED NOT NULL,
    option_text   VARCHAR(500) NOT NULL,
    is_correct    TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_assessment_option_question FOREIGN KEY (question_id) REFERENCES assessment_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment_attempt (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    assessment_id   BIGINT UNSIGNED NOT NULL,
    partner_id      BIGINT UNSIGNED NOT NULL,
    attempt_number  INT UNSIGNED NOT NULL,
    score_percent   TINYINT UNSIGNED NOT NULL,
    passed          TINYINT(1) NOT NULL,
    submitted_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uq_assessment_attempt UNIQUE (assessment_id, partner_id, attempt_number),
    CONSTRAINT fk_assessment_attempt_assessment FOREIGN KEY (assessment_id) REFERENCES assessment(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_attempt_partner FOREIGN KEY (partner_id) REFERENCES partner(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment_answer (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    attempt_id         BIGINT UNSIGNED NOT NULL,
    question_id        BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NOT NULL,
    CONSTRAINT fk_assessment_answer_attempt FOREIGN KEY (attempt_id) REFERENCES assessment_attempt(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answer_question FOREIGN KEY (question_id) REFERENCES assessment_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answer_option FOREIGN KEY (selected_option_id) REFERENCES assessment_option(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
