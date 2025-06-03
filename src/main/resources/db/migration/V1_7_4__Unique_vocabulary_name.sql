ALTER TABLE vocabularies
    ADD CONSTRAINT uniq_vocab_per_owner
        UNIQUE (owner_id, owner_type, name);
