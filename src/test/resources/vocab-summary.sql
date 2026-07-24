-- vocabularies table setup for testing (with 'name' as second column)
SELECT setval('vocabularies_vocab_id_seq', 200);

INSERT INTO vocabularies (name, owner_id, owner_type, is_shared, update_time)
VALUES
    ('Colors', 'class-1', 'class', false, NOW() - INTERVAL '3 days'),
    ('Shapes', 'class-1', 'class', true, NOW() - INTERVAL '2 days'),
    ('Animals', 'class-1', 'class', false, NOW() - INTERVAL '1 day'),

    ('Fruits', 'class-2', 'class', false, NOW() - INTERVAL '4 days'),
    ('Vegetables', 'class-2', 'class', false, NOW() - INTERVAL '1 hour'),

    ('Transport', 'class-3', 'class', true, NOW() - INTERVAL '5 days'),
    ('Buildings', 'class-3', 'class', true, NOW() - INTERVAL '6 days'),

    ('Greetings', 'user-1', 'user', false, NOW() - INTERVAL '12 hours'),
    ('Food', 'user-1', 'user', false, NOW() - INTERVAL '6 hours'),

    ('Clothes', 'user-2', 'user', false, NOW() - INTERVAL '2 days');
