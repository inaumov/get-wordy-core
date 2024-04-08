-- Add word_id column to collocations table
ALTER TABLE collocations
    ADD COLUMN word_id BIGINT;

-- Update word_id values based on corresponding card_id values from cards table
UPDATE collocations
SET word_id = cards.word_id
FROM cards
WHERE collocations.card_id = cards.id;

-- Alter word_id column in collocations table to be NOT NULL
ALTER TABLE collocations
    ALTER COLUMN word_id SET NOT NULL;

-- Add foreign key constraint to word_id column in collocations table
ALTER TABLE collocations
    ADD CONSTRAINT fk_collocations_word_id
        FOREIGN KEY (word_id)
            REFERENCES words (id)
            ON DELETE CASCADE;
