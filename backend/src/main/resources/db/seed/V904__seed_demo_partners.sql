-- Synthetic partner personas spanning the onboarding lifecycle, per the build
-- brief's "seed usable synthetic cases" requirement. All phone numbers and bank
-- details are fictitious dev-only values (+9198xxxxxx00 range), never real identity data.

-- 1) Brand-new applicant: just past phone verification, nothing else chosen yet.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000001', 'Aarav Shah', 'en', 'ACTIVE');
SET @u1 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u1, 'PARTNER');
INSERT INTO partner (user_id, onboarding_stage, activation_status) VALUES (@u1, 'WORK_CATEGORY', 'NOT_ACTIVE');
SET @p1 = LAST_INSERT_ID();
INSERT INTO partner_status_history (partner_id, from_status, to_status, reason) VALUES (@p1, NULL, 'NOT_ACTIVE', 'Application created');

-- 2) Pending identity verification.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000002', 'Priya Nair', 'en', 'ACTIVE');
SET @u2 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u2, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status)
SELECT @u2, cat.id, ci.id, 'SIX', 'VERIFICATION', 'NOT_ACTIVE'
FROM category cat, city ci WHERE cat.slug = 'plumbing' AND ci.name = 'Thane';
SET @p2 = LAST_INSERT_ID();
INSERT INTO verification_request (partner_id, provider, status, submitted_at) VALUES (@p2, 'MANUAL_REVIEW', 'PENDING_REVIEW', CURRENT_TIMESTAMP(3));

-- 3) Rejected verification, awaiting resubmission.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000003', 'Sameer Khan', 'en', 'ACTIVE');
SET @u3 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u3, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status)
SELECT @u3, cat.id, ci.id, 'FOUR', 'VERIFICATION', 'NOT_ACTIVE'
FROM category cat, city ci WHERE cat.slug = 'electrical' AND ci.name = 'Kalyan';
SET @p3 = LAST_INSERT_ID();
INSERT INTO verification_request (partner_id, provider, status, reason, submitted_at, decided_at)
VALUES (@p3, 'MANUAL_REVIEW', 'REJECTED', 'Uploaded document image was blurry/unreadable; please re-upload a clear photo.', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));

-- 4) Verified, booked a screening session.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000004', 'Neha Kulkarni', 'en', 'ACTIVE');
SET @u4 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u4, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status)
SELECT @u4, cat.id, ci.id, 'EIGHT', 'SCREENING', 'NOT_ACTIVE'
FROM category cat, city ci WHERE cat.slug = 'cleaning' AND ci.name = 'Mumbai';
SET @p4 = LAST_INSERT_ID();
INSERT INTO verification_request (partner_id, provider, status, submitted_at, decided_at)
VALUES (@p4, 'MANUAL_REVIEW', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
INSERT INTO screening_booking (slot_id, partner_id, status)
SELECT ss.id, @p4, 'BOOKED'
FROM screening_slot ss JOIN category c ON c.id = ss.category_id JOIN city ci ON ci.id = ss.city_id
WHERE c.slug = 'cleaning' AND ci.name = 'Mumbai' ORDER BY ss.starts_at LIMIT 1;
UPDATE screening_slot ss JOIN category c ON c.id = ss.category_id JOIN city ci ON ci.id = ss.city_id
SET ss.booked_count = ss.booked_count + 1, ss.version = ss.version + 1
WHERE c.slug = 'cleaning' AND ci.name = 'Mumbai' ORDER BY ss.starts_at LIMIT 1;

-- 5) Screening passed, at the starter-kit stage (kit ordered, still processing).
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000005', 'Ravi Deshmukh', 'en', 'ACTIVE');
SET @u5 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u5, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status)
SELECT @u5, cat.id, ci.id, 'SIX', 'STARTER_KIT', 'NOT_ACTIVE'
FROM category cat, city ci WHERE cat.slug = 'painting' AND ci.name = 'Thane';
SET @p5 = LAST_INSERT_ID();
INSERT INTO verification_request (partner_id, provider, status, submitted_at, decided_at) VALUES (@p5, 'MANUAL_REVIEW', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
INSERT INTO kit_order (partner_id, kit_id, delivery_address_line, delivery_city_id, price_paise, fees_paise, total_paise, status, idempotency_key)
SELECT @p5, sk.id, '204, Sunrise Apartments, Thane', ci.id, sk.price_paise, sk.fees_paise, sk.price_paise + sk.fees_paise, 'PROCESSING', CONCAT('seed-kit-order-', @p5)
FROM starter_kit sk JOIN category cat ON cat.id = sk.category_id JOIN city ci ON ci.name = 'Thane'
WHERE cat.slug = 'painting';
SET @ko5 = LAST_INSERT_ID();
INSERT INTO kit_order_event (kit_order_id, event_type) VALUES (@ko5, 'CREATED'), (@ko5, 'PAYMENT_SUCCEEDED');

-- 6) Kit delivered, now in training.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000006', 'Fatima Sheikh', 'en', 'ACTIVE');
SET @u6 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u6, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status)
SELECT @u6, cat.id, ci.id, 'EIGHT', 'TRAINING', 'NOT_ACTIVE'
FROM category cat, city ci WHERE cat.slug = 'cleaning' AND ci.name = 'Mumbai';
SET @p6 = LAST_INSERT_ID();
INSERT INTO verification_request (partner_id, provider, status, submitted_at, decided_at) VALUES (@p6, 'MANUAL_REVIEW', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
INSERT INTO kit_order (partner_id, kit_id, delivery_address_line, delivery_city_id, price_paise, fees_paise, total_paise, status, idempotency_key)
SELECT @p6, sk.id, '12 Marine Drive, Mumbai', ci.id, sk.price_paise, sk.fees_paise, sk.price_paise + sk.fees_paise, 'DELIVERED', CONCAT('seed-kit-order-', @p6)
FROM starter_kit sk JOIN category cat ON cat.id = sk.category_id JOIN city ci ON ci.name = 'Mumbai'
WHERE cat.slug = 'cleaning';
INSERT INTO training_enrollment (partner_id, course_id, status, started_at)
SELECT @p6, id, 'IN_PROGRESS', CURRENT_TIMESTAMP(3) FROM training_course LIMIT 1;

-- 7) Fully active partner, ready to see and accept nearby jobs.
INSERT INTO app_user (phone_e164, name, preferred_language, status) VALUES ('+919800000007', 'Kabir Mehta', 'en', 'ACTIVE');
SET @u7 = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@u7, 'PARTNER');
INSERT INTO partner (user_id, primary_category_id, residence_city_id, working_hours_choice, onboarding_stage, activation_status, experience_years, bio, activated_at)
SELECT @u7, cat.id, ci.id, 'EIGHT', 'COMPLETE', 'ACTIVE', 3, 'Experienced cleaning professional serving South Mumbai.', CURRENT_TIMESTAMP(3)
FROM category cat, city ci WHERE cat.slug = 'cleaning' AND ci.name = 'Mumbai';
SET @p7 = LAST_INSERT_ID();
INSERT INTO partner_status_history (partner_id, from_status, to_status, reason) VALUES (@p7, 'NOT_ACTIVE', 'ACTIVE', 'All activation requirements met (seed data)');
INSERT INTO partner_language (partner_id, language_code) VALUES (@p7, 'en'), (@p7, 'hi');
INSERT INTO partner_skill (partner_id, service_id, approved, approved_at)
SELECT @p7, s.id, 1, CURRENT_TIMESTAMP(3) FROM service s WHERE s.slug = 'kitchen-cleaning';
INSERT INTO partner_coverage (partner_id, area_id)
SELECT @p7, a.id FROM area a JOIN city ci ON ci.id = a.city_id WHERE ci.name = 'Mumbai';
INSERT INTO verification_request (partner_id, provider, status, submitted_at, decided_at) VALUES (@p7, 'MANUAL_REVIEW', 'APPROVED', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
INSERT INTO kit_order (partner_id, kit_id, delivery_address_line, delivery_city_id, price_paise, fees_paise, total_paise, status, idempotency_key)
SELECT @p7, sk.id, '5 Marine Drive, Mumbai', ci.id, sk.price_paise, sk.fees_paise, sk.price_paise + sk.fees_paise, 'DELIVERED', CONCAT('seed-kit-order-', @p7)
FROM starter_kit sk JOIN category cat ON cat.id = sk.category_id JOIN city ci ON ci.name = 'Mumbai'
WHERE cat.slug = 'cleaning';
INSERT INTO training_enrollment (partner_id, course_id, status, started_at, completed_at)
SELECT @p7, id, 'COMPLETED', DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 4 DAY) FROM training_course LIMIT 1;
INSERT INTO bank_account (partner_id, account_holder_name, account_number_masked, account_number_hash, ifsc, bank_name, verification_status, verified_at)
VALUES (@p7, 'Kabir Mehta', 'XXXXXXXX1234', SHA2('seed-account-p7', 256), 'HDFC0000123', 'HDFC Bank', 'PROVIDER_VERIFIED', CURRENT_TIMESTAMP(3));

-- Accept the current TERMS/PRIVACY policy versions for every seeded partner so
-- they do not get blocked on that gate in the demo, except partner #1 (new applicant,
-- deliberately left before the Terms step to demonstrate the resume behaviour).
INSERT INTO partner_consent (partner_id, policy_document_id, accepted_at)
SELECT p.id, pd.id, CURRENT_TIMESTAMP(3)
FROM partner p JOIN policy_document pd ON pd.is_current = 1 AND pd.type IN ('TERMS','PRIVACY')
WHERE p.id IN (@p2, @p3, @p4, @p5, @p6, @p7);
