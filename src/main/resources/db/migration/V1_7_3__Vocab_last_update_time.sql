ALTER TABLE vocabularies
    ADD COLUMN update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE vocabularies
SET update_time = create_time;

ALTER TABLE vocabularies
    ALTER COLUMN create_time SET NOT NULL,
    ALTER COLUMN update_time SET NOT NULL;
