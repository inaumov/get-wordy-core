-- Add word_id column to context table
ALTER TABLE context
    ADD COLUMN word_id BIGINT;

-- Update word_id values based on corresponding card_id values from cards table
UPDATE context
SET word_id = cards.word_id
FROM cards
WHERE context.card_id = cards.id;

-- Alter word_id column in context table to be NOT NULL
ALTER TABLE context
    ALTER COLUMN word_id SET NOT NULL;

-- Add foreign key constraint to word_id column in context table
ALTER TABLE context
    ADD CONSTRAINT fk_context_word_id
        FOREIGN KEY (word_id)
            REFERENCES words (id)
            ON DELETE CASCADE;
