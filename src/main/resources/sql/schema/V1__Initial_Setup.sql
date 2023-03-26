CREATE TABLE dictionary (
  id   SERIAL,
  name VARCHAR(32) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE word (
  id            SERIAL,
  value         VARCHAR(255) NOT NULL UNIQUE,
  transcription VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE card (
  id            SERIAL,
  status        ENUM ('EDIT', 'POSTPONED', 'TO_LEARN', 'LEARNT')  NOT NULL DEFAULT 'EDIT',
  rating        SMALLINT DEFAULT '0'                              NOT NULL,
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP,
  dictionary_id BIGINT UNSIGNED                                   NOT NULL,
  word_id       BIGINT UNSIGNED                                   NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE definition (
  id               SERIAL,
  grammatical_unit ENUM ('NOUN', 'VERB', 'PHRASAL_VERB', 'ADJECTIVE', 'ADVERB', 'PHRASE', 'IDIOM', 'OTHER') NOT NULL,
  value            VARCHAR(255),
  card_id          BIGINT UNSIGNED                                                                          NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (card_id) REFERENCES card (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

CREATE TABLE meaning (
  id            SERIAL,
  translation   VARCHAR(1024)   NOT NULL,
  synonym       VARCHAR(1024),
  antonym       VARCHAR(1024),
  example       VARCHAR(4096),
  definition_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (definition_id) REFERENCES definition (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);