truncate table context;
truncate table collocations;
truncate table cards cascade;
truncate table words cascade;
truncate table vocabularies cascade;

insert into vocabularies (vocab_id, name) values (1, 'dictionary1');
insert into vocabularies (vocab_id, name) values (2, 'dictionary2');

insert into words (id, word, part_of_speech, transcription, meaning) values (1, 'example1', 'noun', 'ɪgˈzɑːmpl', 'a word in vocab 1');
insert into words (id, word, part_of_speech, transcription, meaning) values (2, 'example2', 'noun', 'ɪgˈzɑːmpl', 'other word in vocab 2');
insert into words (id, word, part_of_speech, transcription, meaning) values (3, 'example3', 'noun', 'ɪgˈzɑːmpl', 'not assigned word');

insert into cards (id, score, create_time, last_update_time, dictionary_id, word_id) values (1, 50, '2014-08-17 17:40:03', CURRENT_TIMESTAMP, 1, 1 );
insert into cards (id, score, create_time, last_update_time, dictionary_id, word_id, status) values (2, 50, '2014-08-17 17:40:04', CURRENT_TIMESTAMP, 2, 2, 'TO_LEARN');

insert into collocations (card_id, example, word_id) values (1, 'collocation1', 1);
insert into collocations (card_id, example, word_id) values (2, 'collocation2', 2);

insert into context (card_id, example, word_id, matched_words) values (1, 'Test sentence 1', 1, 'sentence 1');
insert into context (card_id, example, word_id) values (2, 'Test sentence 2', 2);
insert into context (card_id, example, word_id) values (1, 'Test sentence 3', 1);
insert into context (card_id, example, word_id, matched_words) values (1, 'Test sentence 4', 1, 'sentence 4');

update vocabularies set owner_id = 'john-123', owner_type = 'user' where vocab_id = 1;
update vocabularies set owner_id = 'class-42', owner_type = 'class' where vocab_id = 2;

-- updates on vocab API
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

SELECT setval('words_id_seq', (SELECT MAX(id) FROM words));
SELECT setval('dictionaries_id_seq', (SELECT MAX(vocab_id) FROM vocabularies));
