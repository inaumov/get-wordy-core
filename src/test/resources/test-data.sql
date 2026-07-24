TRUNCATE TABLE in_context RESTART IDENTITY;
TRUNCATE TABLE collocations RESTART IDENTITY;
TRUNCATE TABLE progress RESTART IDENTITY CASCADE;
TRUNCATE TABLE words RESTART IDENTITY CASCADE;
TRUNCATE TABLE vocabularies RESTART IDENTITY CASCADE;

insert into vocabularies (name)
values ('vocabulary1'),
       ('other vocabulary');

INSERT INTO words (lemma,
                   part_of_speech,
                   transcription,
                   meaning,
                   register,
                   domain,
                   level)
VALUES ('example',
        'noun',
        'ɪɡˈzɑːmpl',
        'a thing characteristic of its kind',
        null,
        null,
        'A1'),
       ('battery',
        'noun',
        '/ˈbætəri/',
        'A device that stores and supplies electrical energy.',
        'neutral',
        'technology',
        'A2'),
       ('tomato',
        'noun',
        '/təˈmeɪtəʊ/',
        'A round, usually red fruit that is commonly used as a vegetable in cooking.',
        'neutral',
        'food',
        'A1');

INSERT INTO vocab_has_words (vocab_id, word_id)
VALUES (1, 1),
       (2, 2);

insert into progress (vocab_id, word_id, status, score, create_time, last_update_time) values (1, 1, 'TO_LEARN', 3, '2014-08-17 17:40:03', CURRENT_TIMESTAMP);
insert into progress (vocab_id, word_id, status, score, create_time, last_update_time) values (2, 2, 'TO_LEARN', 95, '2014-08-17 17:40:04', CURRENT_TIMESTAMP);

insert into collocations (word_id, phrase) values (1, 'collocation1');
insert into collocations (word_id, phrase) values (2, 'collocation2');

insert into in_context (word_id, example, matched_words) values (1, 'Test sentence 1', 'sentence 1');
insert into in_context (word_id, example) values (2, 'Test sentence 2');
insert into in_context (word_id, example) values (1, 'Test sentence 3');
insert into in_context (word_id, example, matched_words) values (1, 'Test sentence 4', 'sentence 4');

update vocabularies set owner_id = 'john-123', owner_type = 'user' where vocab_id = 1;
update vocabularies set owner_id = 'class-42', owner_type = 'class' where vocab_id = 2;

-- own:
update progress set user_id = 'john-123' where vocab_id = 1;
-- shared:
update progress set user_id = 'john-123' where vocab_id = 2;

-- updates on vocab API
SELECT setval('vocabularies_vocab_id_seq', 100);

INSERT INTO vocabularies (owner_id, owner_type, name, is_shared)
VALUES ('class001', 'class', 'Vocabulary Basics', false),
       ('class001', 'class', 'Grammar 101', true),
       ('class002', 'class', 'Advanced Vocabulary', false);

-- words table
SELECT setval('words_id_seq', 9);

INSERT INTO words (lemma, part_of_speech)
VALUES ('run', 'verb'),
       ('jump', 'verb'),
       ('blue', 'adjective'),
       ('quickly', 'adverb'),
       ('cat', 'noun'),
       ('dog', 'noun'),
       ('swim', 'verb'),
       ('red', 'adjective'),
       ('walk', 'verb'),
       ('slowly', 'adverb'),
       ('happy', 'adjective');

-- vocab-has-words relation
INSERT INTO vocab_has_words (vocab_id, word_id)
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
