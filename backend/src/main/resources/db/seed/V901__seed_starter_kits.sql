-- One demo starter kit per category. Prices are illustrative placeholders, not
-- actual SupplyBase fees.

INSERT INTO starter_kit (category_id, name, description, price_paise, fees_paise, terms_text, active)
SELECT id, CONCAT(name, ' Starter Kit'),
       CONCAT('Essential tools and uniform to begin taking ', name, ' jobs.'),
       149900, 4900,
       'Kit price is non-refundable once shipped, except for manufacturing defects reported within 7 days of delivery. Delivery typically takes 5-7 business days.',
       1
FROM category;

INSERT INTO starter_kit_item (kit_id, name, quantity)
SELECT id, 'Branded uniform (2 sets)', 1 FROM starter_kit;
INSERT INTO starter_kit_item (kit_id, name, quantity)
SELECT id, 'ID badge', 1 FROM starter_kit;
INSERT INTO starter_kit_item (kit_id, name, quantity)
SELECT id, 'Category tool kit', 1 FROM starter_kit;
