CREATE TABLE theme
(
    theme_id    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    owner_id    VARCHAR(64)                 NOT NULL,
    owner_type  VARCHAR(32)                 NOT NULL,
    name        VARCHAR(255)                NOT NULL,
    notes       VARCHAR(4000),
    status      VARCHAR(20) DEFAULT 'DRAFT' NOT NULL,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE theme
    ADD CONSTRAINT cards_status_check CHECK (status IN ('DRAFT', 'GENERATING', 'READY', 'FAILED'));

CREATE UNIQUE INDEX uq_theme_owner_name
    ON theme (
              owner_id,
              owner_type,
              lower(name)
        );

CREATE TABLE theme_has_words
(
    theme_id INT NOT NULL,
    word_id  INT NOT NULL,
    FOREIGN KEY (theme_id) REFERENCES theme (theme_id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES words (id) ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (theme_id, word_id)
);
