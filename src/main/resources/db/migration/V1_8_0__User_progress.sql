ALTER TABLE cards
    DROP id;

ALTER TABLE cards
    RENAME TO progress;

ALTER TABLE progress
    ADD CONSTRAINT uniq_progress_per_user
        UNIQUE (user_id, vocab_id, word_id)
