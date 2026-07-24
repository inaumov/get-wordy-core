ALTER TABLE vocabularies
    ALTER COLUMN vocab_id DROP DEFAULT;

ALTER TABLE vocabularies
    RENAME CONSTRAINT dictionaries_pkey TO vocabularies_pkey;

DROP SEQUENCE dictionaries_id_seq;

ALTER TABLE vocabularies
    ALTER COLUMN vocab_id ADD GENERATED ALWAYS AS IDENTITY;
