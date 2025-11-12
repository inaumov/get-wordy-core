-- adjust words table
ALTER TABLE words
    RENAME COLUMN word TO lemma;

ALTER TABLE words
    ALTER COLUMN lemma TYPE TEXT,
    ALTER COLUMN meaning TYPE TEXT,
    ALTER COLUMN part_of_speech SET NOT NULL,
    ADD COLUMN register   TEXT, -- e.g. "formal", "slang"
    ADD COLUMN domain     TEXT, -- e.g. "medicine", "sports"
    ADD COLUMN created_at TIMESTAMP DEFAULT now();

-- rename word identification field
ALTER TABLE vocab_has_words
    RENAME COLUMN word_ref TO word_id;

-- adjust collocations: rename example → phrase, add word_sense_id
ALTER TABLE collocations
    RENAME COLUMN example TO phrase;
ALTER TABLE collocations
    ALTER COLUMN phrase TYPE TEXT;

CREATE OR REPLACE VIEW vocab_words_headlines AS
(
SELECT vhw.vocab_id,
       vhw.word_id                                                 AS word_id,
       w.lemma,
       w.part_of_speech,
       w.transcription,
       w.meaning,
       w.register,
       w.domain,
       array_remove(array_agg(DISTINCT in_context.example), NULL)  AS card_sentences,
       array_remove(array_agg(DISTINCT collocations.phrase), NULL) AS card_collocations
FROM vocab_has_words vhw
         JOIN words w ON vhw.word_id = w.id
         LEFT JOIN
     in_context ON w.id = in_context.word_id
         LEFT JOIN
     collocations ON w.id = collocations.word_id
GROUP BY vhw.vocab_id, vhw.word_id, w.lemma, w.part_of_speech, w.transcription, w.meaning, w.register, w.domain);
