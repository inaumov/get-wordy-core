CREATE TABLE dictionaries
(
    id   SERIAL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE words
(
    id             SERIAL,
    word           VARCHAR(255) NOT NULL,
    part_of_speech VARCHAR(40),
    transcription  VARCHAR(255),
    meaning        VARCHAR(4000),
    PRIMARY KEY (id)
);

CREATE TABLE cards
(
    id            SERIAL,
    status        VARCHAR(20) DEFAULT 'EDIT' NOT NULL CHECK (status IN ('EDIT', 'POSTPONED', 'TO_LEARN', 'LEARNT')),
    score         SMALLINT    DEFAULT 0      NOT NULL,
    create_time   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP,
    dictionary_id BIGINT                     NOT NULL,
    word_id       BIGINT                     NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (word_id) REFERENCES words (id) ON UPDATE CASCADE
);

CREATE TABLE context
(
    id      SERIAL,
    example VARCHAR(255) NOT NULL,
    card_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE collocations
(
    id      SERIAL,
    example VARCHAR(255) NOT NULL,
    card_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE ON UPDATE CASCADE
);