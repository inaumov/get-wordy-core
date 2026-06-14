ALTER TABLE theme
    ADD COLUMN candidate_words_draft jsonb;

COMMENT ON COLUMN theme.candidate_words_draft IS
    'Raw AI-generated candidate words draft';