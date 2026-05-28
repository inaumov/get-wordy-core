TRUNCATE TABLE
    theme_has_words,
    theme
    RESTART IDENTITY;

INSERT INTO theme (owner_id, owner_type, name)
VALUES ('john-123', 'user', 'Animals'),
       ('john-123', 'user', 'Food'),
       ('mike-456', 'user', 'Sports');

insert into theme_has_words(theme_id, word_id)
values (1, 1),
       (1, 2),
       (2, 3);

INSERT INTO words(lemma, part_of_speech)
VALUES ('Airport', 'noun'),
       ('Passport', 'noun'),
       ('Boarding pass', 'noun');
