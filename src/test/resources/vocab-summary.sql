-- vocabularies table setup for testing (with 'name' as second column)
INSERT INTO vocabularies (vocab_id, name, owner_id, owner_type, is_shared, update_time)
VALUES
    (201, 'Colors', 'class-1', 'class', false, NOW() - INTERVAL '3 days'),
    (202, 'Shapes', 'class-1', 'class', true, NOW() - INTERVAL '2 days'),
    (203, 'Animals', 'class-1', 'class', false, NOW() - INTERVAL '1 day'),

    (204, 'Fruits', 'class-2', 'class', false, NOW() - INTERVAL '4 days'),
    (205, 'Vegetables', 'class-2', 'class', false, NOW() - INTERVAL '1 hour'),

    (206, 'Transport', 'class-3', 'class', true, NOW() - INTERVAL '5 days'),
    (207, 'Buildings', 'class-3', 'class', true, NOW() - INTERVAL '6 days'),

    (208, 'Greetings', 'user-1', 'user', false, NOW() - INTERVAL '12 hours'),
    (209, 'Food', 'user-1', 'user', false, NOW() - INTERVAL '6 hours'),

    (210, 'Clothes', 'user-2', 'user', false, NOW() - INTERVAL '2 days');
