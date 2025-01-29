-- Update renamed status values
UPDATE cards
SET status = 'DEFERRED'
WHERE status = 'POSTPONED';

-- Drop the old constraint and add the new one
ALTER TABLE cards
    DROP CONSTRAINT cards_status_check;

ALTER TABLE cards
    ADD CONSTRAINT cards_status_check CHECK (status IN ('TO_LEARN', 'DEFERRED', 'LEARNT'));

-- Set the default value to 'TO_LEARN'
ALTER TABLE cards
    ALTER COLUMN status SET DEFAULT 'TO_LEARN';
