truncate table context;
truncate table collocations;
truncate table cards cascade;
truncate table words cascade;
truncate table dictionaries cascade;

insert into dictionaries (id, name) values (1, 'dictionary1');
insert into dictionaries (id, name) values (2, 'dictionary2');

insert into words (id, word, part_of_speech, transcription, meaning) values (1, 'example1', 'noun', 'ɪgˈzɑːmpl', 'a word in dic 1');
insert into words (id, word, part_of_speech, transcription, meaning) values (2, 'example2', 'noun', 'ɪgˈzɑːmpl', 'other word in dic 2');
insert into words (id, word, part_of_speech, transcription, meaning) values (3, 'example3', 'noun', 'ɪgˈzɑːmpl', 'a word without dictionary');

insert into cards (id, score, create_time, last_update_time, dictionary_id, word_id) values (1, 50, '2014-08-17 17:40:03', CURRENT_TIMESTAMP, 1, 1 );
insert into cards (id, score, create_time, last_update_time, dictionary_id, word_id, status) values (2, 50, '2014-08-17 17:40:04', CURRENT_TIMESTAMP, 2, 2, 'TO_LEARN');

insert into collocations (id, example, card_id) values (1, 'collocation1', 1);
insert into collocations (id, example, card_id) values (2, 'collocation2', 2);

insert into context (id, example, card_id) values (1, 'context1', 1);
insert into context (id, example, card_id) values (2, 'context2', 2);
