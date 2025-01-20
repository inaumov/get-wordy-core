-- Remove card id columns from context and collocation tables
ALTER TABLE context
    DROP COLUMN card_id;

ALTER TABLE collocations
    DROP COLUMN card_id;

ALTER TABLE context
    RENAME TO in_context;
