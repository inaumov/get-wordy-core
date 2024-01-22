CREATE TABLE dictionaries (
  id   SERIAL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE words (
  id            SERIAL,
  word          VARCHAR(255) NOT NULL,
  part_of_speech VARCHAR (40),
  transcription VARCHAR(255),
  meaning       VARCHAR(4000),
  PRIMARY KEY (id)
);

CREATE TABLE cards (
  id            SERIAL,
  status        ENUM ('EDIT', 'POSTPONED', 'TO_LEARN', 'LEARNT')  NOT NULL DEFAULT 'EDIT',
  rating        SMALLINT DEFAULT '0'                              NOT NULL,
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP,
  dictionary_id BIGINT UNSIGNED                                   NOT NULL,
  word_id       BIGINT UNSIGNED                                   NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE context (
  id               SERIAL,
  example          VARCHAR(255),
  word_id          BIGINT UNSIGNED                                                                          NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (word_id) REFERENCES words (id)
    ON delete CASCADE
    ON update CASCADE
);

CREATE TABLE collocations (
  id            SERIAL,
  example       VARCHAR(255),
  word_id       BIGINT UNSIGNED,
  PRIMARY KEY (id),
  FOREIGN KEY (word_id) REFERENCES words (id)
    ON delete CASCADE
    ON update CASCADE
);