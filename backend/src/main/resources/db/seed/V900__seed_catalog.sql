-- DEV/DEMO SEED DATA -- never applied in production (see application-prod.yml,
-- which points spring.flyway.locations only at classpath:db/migration).
-- Provisional catalog: seven categories per the SupplyBase Partners build brief,
-- Kitchen Cleaning included under Cleaning to exercise the reference flow.

INSERT INTO category (name, slug, display_order, active) VALUES
    ('Electrical', 'electrical', 1, 1),
    ('Plumbing', 'plumbing', 2, 1),
    ('Painting', 'painting', 3, 1),
    ('POP Ceiling & Design', 'pop-ceiling-design', 4, 1),
    ('Waterproofing', 'waterproofing', 5, 1),
    ('Interior Design', 'interior-design', 6, 1),
    ('Cleaning', 'cleaning', 7, 1);

INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'General Electrical Repair', 'general-electrical-repair', 1, 1 FROM category WHERE slug = 'electrical';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'General Plumbing Repair', 'general-plumbing-repair', 1, 1 FROM category WHERE slug = 'plumbing';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'Interior Wall Painting', 'interior-wall-painting', 1, 1 FROM category WHERE slug = 'painting';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'POP False Ceiling', 'pop-false-ceiling', 1, 1 FROM category WHERE slug = 'pop-ceiling-design';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'Terrace Waterproofing', 'terrace-waterproofing', 1, 1 FROM category WHERE slug = 'waterproofing';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'Modular Kitchen Design', 'modular-kitchen-design', 1, 1 FROM category WHERE slug = 'interior-design';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'Kitchen Cleaning', 'kitchen-cleaning', 1, 1 FROM category WHERE slug = 'cleaning';
INSERT INTO service (category_id, name, slug, display_order, active)
SELECT id, 'Bathroom Cleaning', 'bathroom-cleaning', 2, 1 FROM category WHERE slug = 'cleaning';

INSERT INTO service_requirement (service_id, starter_kit_required)
SELECT id, 1 FROM service;

INSERT INTO city (name, state, active) VALUES
    ('Mumbai', 'Maharashtra', 1),
    ('Thane', 'Maharashtra', 1),
    ('Kalyan', 'Maharashtra', 1);

INSERT INTO area (city_id, name, active)
SELECT id, area_name, 1 FROM city
JOIN (
    SELECT 'Mumbai' AS city_name, 'Andheri' AS area_name UNION ALL
    SELECT 'Mumbai', 'Bandra' UNION ALL
    SELECT 'Mumbai', 'Dadar' UNION ALL
    SELECT 'Thane', 'Ghodbunder Road' UNION ALL
    SELECT 'Thane', 'Naupada' UNION ALL
    SELECT 'Kalyan', 'Kalyan West' UNION ALL
    SELECT 'Kalyan', 'Kalyan East'
) areas ON areas.city_name = city.name;

-- Demo earning-potential estimates. Explicitly labelled as illustrative in the API
-- response (see EstimateResponse.demo=true) -- never presented as a guarantee.
INSERT INTO category_city_estimate (category_id, city_id, hours_choice, estimated_monthly_paise, assumptions_text, active)
SELECT c.id, ci.id, hc.hours_choice, hc.amount_paise, hc.assumption, 1
FROM category c
JOIN city ci
JOIN (
    SELECT 'FOUR' AS hours_choice, 1680000 AS amount_paise, 'Assumes ~4 hrs/day, 24 working days/month, average demand in this city.' AS assumption UNION ALL
    SELECT 'SIX', 2520000, 'Assumes ~6 hrs/day, 24 working days/month, average demand in this city.' UNION ALL
    SELECT 'EIGHT', 3360000, 'Assumes ~8 hrs/day, 24 working days/month, average demand in this city. Matches the reference "Up to Rs 33,600 per month" example.'
) hc ON 1=1
WHERE c.slug = 'cleaning' AND ci.name = 'Mumbai';

INSERT INTO policy_document (type, version, title, content_markdown, is_current)
VALUES (
    'TERMS', 'draft-2026-09', 'SupplyBase Partners Terms of Service',
    '# SupplyBase Partners Terms of Service (DRAFT)\n\nThis is placeholder draft content pending business/legal review. It is provided so the onboarding flow is testable end to end and must not be treated as finalized legal text.\n\n1. Eligibility and conduct\n2. Job acceptance and cancellation\n3. Payments and payouts\n4. Data and privacy\n5. Termination',
    1
);

INSERT INTO policy_document (type, version, title, content_markdown, is_current)
VALUES (
    'PRIVACY', 'draft-2026-09', 'SupplyBase Partners Privacy Policy',
    '# SupplyBase Partners Privacy Policy (DRAFT)\n\nPlaceholder draft content pending business/legal review; not finalized. Describes categories of data collected during onboarding (identity, location, bank details) and retention intent.',
    1
);

INSERT INTO policy_document (type, version, title, content_markdown, is_current)
VALUES (
    'MARKETING_CONSENT', 'draft-2026-09', 'Marketing Communications Consent',
    '# Marketing Communications Consent (DRAFT)\n\nOptional. Kept separate from Terms/Privacy acceptance as required by the build brief.',
    1
);
