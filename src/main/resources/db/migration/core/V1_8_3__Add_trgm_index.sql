CREATE INDEX IF NOT EXISTS idx_words_lemma_trgm
    ON words USING gin (lemma gin_trgm_ops);
