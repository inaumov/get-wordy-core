-- predefined data for classes dao tests
INSERT INTO classes (class_id, name, format, level, material, notes, owner_id, owner_type)
VALUES ('class001', 'Math 101', 'online', 'beginner', 'book.pdf', 'Basic math concepts', 'owner123', 'user'),
       ('class002', 'Science 101', 'offline', 'beginner', 'book.pdf', 'Basic science concepts', 'owner123', 'user'),
       ('class003', 'History 101', 'hybrid', 'advanced', 'slides.pdf', 'Detailed history concepts', 'owner456', 'user');

-- predefined data for wordsheet dao tests
INSERT INTO word_sheet (id, class_id, name, is_shared)
VALUES (1, 'class001', 'Vocabulary Basics', false),
       (2, 'class001', 'Grammar 101', true),
       (3, 'class002', 'Advanced Vocabulary', false);

-- words table
INSERT INTO words (id, word, part_of_speech)
VALUES (10, 'run', 'verb'),
       (11, 'jump', 'verb'),
       (12, 'blue', 'adjective'),
       (13, 'quickly', 'adverb'),
       (14, 'cat', 'noun'),
       (15, 'dog', 'noun'),
       (16, 'swim', 'verb'),
       (17, 'red', 'adjective'),
       (18, 'walk', 'verb'),
       (19, 'slowly', 'adverb'),
       (20, 'happy', 'adjective');

-- wordsheet-to-words table
INSERT INTO wordsheet_to_words (wordsheet_id, word_ref)
VALUES (1, 10),
       (1, 11),
       (1, 12),
       (1, 13),
       (2, 14),
       (2, 15),
       (2, 16),
       (2, 17),
       (2, 18),
       (2, 19),
       (2, 20);