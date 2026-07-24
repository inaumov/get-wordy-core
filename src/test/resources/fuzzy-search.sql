SELECT setval('words_id_seq', 30);

INSERT INTO words(lemma, part_of_speech, transcription, meaning, level, created_at)
VALUES ('accommodation', 'noun', 'əˌkɒməˈdeɪʃən',
        'a place to live or stay', 'A1', now()),
       ('restaurant', 'noun', 'ˈrestərɒnt',
        'a place where meals are prepared and served', 'A1', now()),
       ('environment', 'noun', 'ɪnˈvaɪərənmənt',
        'the surroundings in which something exists', 'A1', now()),
       ('beautiful', 'adjective', 'ˈbjuːtɪfəl',
        'pleasing to the senses or mind', 'A1', now()),
       ('necessary', 'adjective', 'ˈnesəseri',
        'needed or required', 'A1', now()),
       ('separate', 'adjective', 'ˈsepərət',
        'existing or happening independently', 'A1', now());