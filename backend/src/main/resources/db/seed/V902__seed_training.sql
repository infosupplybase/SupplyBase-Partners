-- One shared demo training course (original text-based content) with a
-- three-question assessment, required for activation across all categories.

INSERT INTO training_course (category_id, title, description, required, active)
VALUES (NULL, 'SupplyBase Partner Fundamentals',
        'Core professionalism, safety, and customer-interaction standards required of every SupplyBase partner.',
        1, 1);

SET @course_id = LAST_INSERT_ID();

INSERT INTO training_module (course_id, title, content_type, content_body, display_order) VALUES
    (@course_id, 'Welcome and Code of Conduct', 'TEXT',
     'As a SupplyBase partner you represent our brand in a customer''s home. Always arrive on time, wear your uniform, and greet the customer courteously. Do not accept cash side-payments outside the app.', 1),
    (@course_id, 'Safety on the Job', 'TEXT',
     'Wear provided protective equipment for your category. Turn off relevant electrical/water supply before starting electrical or plumbing work. Never work at height without proper support.', 2),
    (@course_id, 'Handling Customer Issues', 'TEXT',
     'If a customer raises a concern, stay calm, document the issue with photos, and use the in-app support flow. Never argue with a customer on-site.', 3);

UPDATE service_requirement sr
JOIN service s ON s.id = sr.service_id
SET sr.required_course_id = @course_id;

INSERT INTO assessment (course_id, title, pass_score_percent, max_attempts)
VALUES (@course_id, 'Partner Fundamentals Check', 70, 3);

SET @assessment_id = LAST_INSERT_ID();

INSERT INTO assessment_question (assessment_id, question_text, display_order) VALUES
    (@assessment_id, 'Is it acceptable to accept a cash side-payment outside the app?', 1);
SET @q1 = LAST_INSERT_ID();
INSERT INTO assessment_option (question_id, option_text, is_correct) VALUES
    (@q1, 'No, all payments must go through the app', 1),
    (@q1, 'Yes, if the customer offers', 0);

INSERT INTO assessment_question (assessment_id, question_text, display_order) VALUES
    (@assessment_id, 'Before starting electrical work, you should:', 2);
SET @q2 = LAST_INSERT_ID();
INSERT INTO assessment_option (question_id, option_text, is_correct) VALUES
    (@q2, 'Turn off the relevant electrical supply first', 1),
    (@q2, 'Start immediately to save time', 0);

INSERT INTO assessment_question (assessment_id, question_text, display_order) VALUES
    (@assessment_id, 'If a customer raises a concern on-site, you should:', 3);
SET @q3 = LAST_INSERT_ID();
INSERT INTO assessment_option (question_id, option_text, is_correct) VALUES
    (@q3, 'Stay calm and use the in-app support flow', 1),
    (@q3, 'Argue with the customer', 0);
