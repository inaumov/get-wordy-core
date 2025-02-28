ALTER TABLE class_info
    ADD COLUMN is_repeatable BOOLEAN DEFAULT FALSE,
    ADD COLUMN end_date      DATE;
