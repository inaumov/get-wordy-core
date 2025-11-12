ALTER TABLE dictionaries
    RENAME TO vocabularies;
ALTER TABLE vocabularies
    RENAME COLUMN id TO vocab_id;
ALTER TABLE vocabularies
    ADD COLUMN is_shared BOOLEAN;
ALTER TABLE vocabularies
    ADD COLUMN create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE vocab_has_words
(
    vocab_id INT NOT NULL,
    word_ref INT NOT NULL,
    FOREIGN KEY (vocab_id) REFERENCES vocabularies (vocab_id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (word_ref) REFERENCES words (id) ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (vocab_id, word_ref)
);
