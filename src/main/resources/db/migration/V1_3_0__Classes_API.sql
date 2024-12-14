CREATE TABLE classes
(
    class_id   VARCHAR(50) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    format     VARCHAR(50),
    level      VARCHAR(50),
    material   TEXT,
    notes      TEXT,
    owner_id   VARCHAR(50)  NOT NULL,
    owner_type VARCHAR(50)  NOT NULL
);

CREATE TABLE word_sheet
(
    id          SERIAL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    name        VARCHAR(255) NOT NULL,
    class_id    VARCHAR(50)  NOT NULL,
    is_shared   BOOLEAN,
    PRIMARY KEY (id),
    FOREIGN KEY (class_id) REFERENCES classes (class_id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE wordsheet_to_words
(
    wordsheet_id INT NOT NULL,
    word_ref     INT NOT NULL,
    FOREIGN KEY (wordsheet_id) REFERENCES word_sheet (id) ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (word_ref) REFERENCES words (id) ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (wordsheet_id, word_ref)
);
