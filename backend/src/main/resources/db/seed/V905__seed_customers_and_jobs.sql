-- Synthetic customer-side data and demo job lifecycle records. A seed-only
-- "system" admin identity (no password_hash, so it can never log in) is used
-- purely as the audit "created by" reference for these admin-created requests;
-- the real, loginable admin account is created separately at startup by
-- AdminBootstrapRunner from environment variables, never from seed SQL.

INSERT INTO app_user (phone_e164, name, preferred_language, username, status)
VALUES ('+919800099999', 'Seed Data System', 'en', 'seed-system', 'ACTIVE');
SET @seed_admin = LAST_INSERT_ID();
INSERT INTO user_role (user_id, role) VALUES (@seed_admin, 'ADMIN');

INSERT INTO customer (name, phone_e164) VALUES
    ('Ananya Iyer', '+919811100001'),
    ('Rohan Verma', '+919811100002'),
    ('Meera Joshi', '+919811100003');

INSERT INTO customer_address (customer_id, line1, line2, area_id, lat, lng)
SELECT c.id, addr.line1, addr.line2, a.id, addr.lat, addr.lng
FROM customer c
JOIN (
    SELECT 'Ananya Iyer' AS cust, 'Flat 12B, Sea View Apartments' AS line1, 'Andheri West' AS line2, 'Andheri' AS area, 19.1197 AS lat, 72.8468 AS lng UNION ALL
    SELECT 'Rohan Verma', '304, Hill Crest', 'Bandra East', 'Bandra', 19.0596, 72.8402 UNION ALL
    SELECT 'Meera Joshi', '7, Shanti Niwas', 'Dadar West', 'Dadar', 19.0178, 72.8478
) addr ON addr.cust = c.name
JOIN area a ON a.name = addr.area;

-- Two OPEN cleaning jobs in Mumbai/Andheri and Bandra -- visible to active,
-- Mumbai-covering cleaning partners in "Around You".
INSERT INTO service_request (customer_id, service_id, address_id, scheduled_at, notes, job_value_paise, partner_earning_paise, status, created_by_admin_id)
SELECT cu.id, s.id, ca.id, DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 2 DAY), 'Deep kitchen cleaning, 2BHK.', 129900, 89900, 'OPEN', @seed_admin
FROM customer cu JOIN customer_address ca ON ca.customer_id = cu.id
JOIN service s ON s.slug = 'kitchen-cleaning'
WHERE cu.name = 'Ananya Iyer';

INSERT INTO service_request (customer_id, service_id, address_id, scheduled_at, notes, job_value_paise, partner_earning_paise, status, created_by_admin_id)
SELECT cu.id, s.id, ca.id, DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 3 DAY), 'Bathroom deep clean before a house party.', 99900, 69900, 'OPEN', @seed_admin
FROM customer cu JOIN customer_address ca ON ca.customer_id = cu.id
JOIN service s ON s.slug = 'bathroom-cleaning'
WHERE cu.name = 'Rohan Verma';

-- One job already assigned to and in progress for the active demo partner (#7, Kabir Mehta).
INSERT INTO service_request (customer_id, service_id, address_id, scheduled_at, notes, job_value_paise, partner_earning_paise, status, assigned_partner_id, created_by_admin_id)
SELECT cu.id, s.id, ca.id, DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 1 HOUR), 'Full kitchen deep clean.', 119900, 82900, 'IN_PROGRESS',
       p.id, @seed_admin
FROM customer cu JOIN customer_address ca ON ca.customer_id = cu.id
JOIN service s ON s.slug = 'kitchen-cleaning'
JOIN partner p JOIN app_user au ON au.id = p.user_id
WHERE cu.name = 'Meera Joshi' AND au.phone_e164 = '+919800000007';

SET @in_progress_request = LAST_INSERT_ID();
INSERT INTO job_status_history (service_request_id, from_status, to_status, actor_user_id, reason) VALUES
    (@in_progress_request, NULL, 'OPEN', @seed_admin, 'Seed data'),
    (@in_progress_request, 'OPEN', 'ASSIGNED', @seed_admin, 'Seed data: pre-assigned to demo partner'),
    (@in_progress_request, 'ASSIGNED', 'IN_PROGRESS', @seed_admin, 'Seed data: partner started job');

-- One already-completed job for the same partner, with its exactly-once earning entry,
-- to populate the Money tab with real demo history.
INSERT INTO service_request (customer_id, service_id, address_id, scheduled_at, notes, job_value_paise, partner_earning_paise, status, assigned_partner_id, created_by_admin_id)
SELECT cu.id, s.id, ca.id, DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 DAY), 'Weekly kitchen cleaning.', 109900, 75900, 'COMPLETED',
       p.id, @seed_admin
FROM customer cu JOIN customer_address ca ON ca.customer_id = cu.id
JOIN service s ON s.slug = 'kitchen-cleaning'
JOIN partner p JOIN app_user au ON au.id = p.user_id
WHERE cu.name = 'Ananya Iyer' AND au.phone_e164 = '+919800000007';

SET @completed_request = LAST_INSERT_ID();
INSERT INTO job_status_history (service_request_id, from_status, to_status, actor_user_id, reason) VALUES
    (@completed_request, NULL, 'OPEN', @seed_admin, 'Seed data'),
    (@completed_request, 'OPEN', 'ASSIGNED', @seed_admin, 'Seed data'),
    (@completed_request, 'ASSIGNED', 'IN_PROGRESS', @seed_admin, 'Seed data'),
    (@completed_request, 'IN_PROGRESS', 'COMPLETION_SUBMITTED', @seed_admin, 'Seed data'),
    (@completed_request, 'COMPLETION_SUBMITTED', 'COMPLETED', @seed_admin, 'Seed data: customer confirmed');

INSERT INTO earning_entry (partner_id, service_request_id, gross_paise, commission_paise, net_paise, status, earned_at, available_at)
SELECT p.id, @completed_request, 109900, 34000, 75900, 'EARNED', DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 5 DAY)
FROM partner p JOIN app_user au ON au.id = p.user_id WHERE au.phone_e164 = '+919800000007';
