CREATE TABLE classes
(
    class_id   VARCHAR(50) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    format     VARCHAR(50),
    level      VARCHAR(50),
    material   TEXT,
    notes      TEXT,
    owner_id   VARCHAR(50)  NOT NULL,
    owner_type VARCHAR(50)  NOT NULL,
    time_slot ???? array
);
