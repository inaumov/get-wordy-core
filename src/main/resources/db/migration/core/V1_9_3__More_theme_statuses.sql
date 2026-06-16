ALTER TABLE theme
    DROP CONSTRAINT cards_status_check;

ALTER TABLE theme
    ADD CONSTRAINT cards_status_check
        CHECK (
            status IN (
                       'NEW',
                       'GENERATING',
                       'DRAFT',
                       'CONFIRMED',
                       'PROCESSING',
                       'READY',
                       'FAILED'
                )
            );