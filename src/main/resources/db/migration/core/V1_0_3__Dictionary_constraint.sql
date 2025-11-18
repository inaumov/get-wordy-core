ALTER TABLE cards
    ADD CONSTRAINT fk_cards_to_dictionary
        FOREIGN KEY (dictionary_id)
            REFERENCES dictionaries (id) ON DELETE CASCADE ON UPDATE CASCADE;
