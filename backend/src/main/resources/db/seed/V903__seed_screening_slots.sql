-- Upcoming screening slots, computed relative to install time so they always
-- look "upcoming" to whoever runs this locally, for every seeded category/city pair.

INSERT INTO screening_slot (category_id, city_id, mode, venue_or_link, directions_text, starts_at, ends_at, check_in_opens_at, check_in_closes_at, capacity, booked_count)
SELECT
    c.id, ci.id,
    IF(ci.name = 'Mumbai', 'IN_PERSON', 'VIRTUAL'),
    IF(ci.name = 'Mumbai', CONCAT('SupplyBase Partner Center, ', ci.name), 'https://meet.supplybase.example/screening'),
    IF(ci.name = 'Mumbai', 'Near the main railway station; ask for the SupplyBase desk.', NULL),
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 3 DAY) + INTERVAL 10 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 3 DAY) + INTERVAL 11 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 3 DAY) + INTERVAL 9 HOUR + INTERVAL 45 MINUTE,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 3 DAY) + INTERVAL 10 HOUR + INTERVAL 15 MINUTE,
    15, 0
FROM category c JOIN city ci;

INSERT INTO screening_slot (category_id, city_id, mode, venue_or_link, directions_text, starts_at, ends_at, check_in_opens_at, check_in_closes_at, capacity, booked_count)
SELECT
    c.id, ci.id,
    IF(ci.name = 'Mumbai', 'IN_PERSON', 'VIRTUAL'),
    IF(ci.name = 'Mumbai', CONCAT('SupplyBase Partner Center, ', ci.name), 'https://meet.supplybase.example/screening'),
    IF(ci.name = 'Mumbai', 'Near the main railway station; ask for the SupplyBase desk.', NULL),
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 7 DAY) + INTERVAL 15 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 7 DAY) + INTERVAL 16 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 7 DAY) + INTERVAL 14 HOUR + INTERVAL 45 MINUTE,
    DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 7 DAY) + INTERVAL 15 HOUR + INTERVAL 15 MINUTE,
    15, 0
FROM category c JOIN city ci;
