-- predefined data for classes dao tests
INSERT INTO classes (class_id, name, format, level, material, notes, owner_id, owner_type)
VALUES ('class001', 'Math 101', 'online', 'beginner', 'book.pdf', 'Basic math concepts', 'owner123', 'user'),
       ('class002', 'Science 101', 'offline', 'beginner', 'book.pdf', 'Basic science concepts', 'owner123', 'user'),
       ('class003', 'History 101', 'hybrid', 'advanced', 'slides.pdf', 'Detailed history concepts', 'owner456', 'user');

-- predefined data for vocabularies dao tests
INSERT INTO vocabularies (vocab_id, owner_id, owner_type, name, is_shared)
VALUES (101, 'class001', 'class', 'Vocabulary Basics', false),
       (102, 'class001', 'class', 'Grammar 101', true),
       (103, 'class002', 'class', 'Advanced Vocabulary', false);

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

-- vocab-has-words relation
INSERT INTO vocab_has_words (vocab_id, word_ref)
VALUES (101, 10),
       (101, 11),
       (101, 12),
       (101, 13),
       (102, 14),
       (102, 15),
       (102, 16),
       (102, 17),
       (102, 18),
       (102, 19),
       (102, 20);