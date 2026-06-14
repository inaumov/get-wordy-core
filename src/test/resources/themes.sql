TRUNCATE TABLE
    theme_has_words,
    theme
    RESTART IDENTITY;

INSERT INTO theme (owner_id, owner_type, name, status)
VALUES ('john-123', 'user', 'Animals', 'READY'),
       ('john-123', 'user', 'Food', 'READY'),
       ('mike-456', 'user', 'Sports', 'NEW'),
       ('mike-456', 'user', 'Pets', 'DRAFT');

UPDATE theme
SET candidate_words_draft = '[
  {
    "lemma": "dog",
    "partOfSpeech": "noun",
    "meaning": "a domesticated animal commonly kept as a pet",
    "level": "A1"
  },
  {
    "lemma": "cat",
    "partOfSpeech": "noun",
    "meaning": "a small domesticated animal often kept in homes",
    "level": "A1"
  },
  {
    "lemma": "hamster",
    "partOfSpeech": "noun",
    "meaning": "a small rodent often kept as a pet",
    "level": "A2"
  },
  {
    "lemma": "parrot",
    "partOfSpeech": "noun",
    "meaning": "a colorful bird that can imitate sounds and speech",
    "level": "A2"
  },
  {
    "lemma": "rabbit",
    "partOfSpeech": "noun",
    "meaning": "a small animal with long ears often kept as a pet",
    "level": "A1"
  }
]'::jsonb
WHERE theme_id = 4;

INSERT INTO theme_has_words(theme_id, word_id)
VALUES (1, 1),
       (1, 2),
       (2, 3);

INSERT INTO words(lemma, part_of_speech)
VALUES ('Airport', 'noun'),
       ('Passport', 'noun'),
       ('Boarding pass', 'noun');
