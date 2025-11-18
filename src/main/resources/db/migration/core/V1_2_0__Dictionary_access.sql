ALTER TABLE dictionaries
    ADD COLUMN owner_id   VARCHAR(50),
    ADD COLUMN owner_type VARCHAR(50);
-- composite key index
CREATE INDEX idx_owner ON dictionaries (owner_id, owner_type);
