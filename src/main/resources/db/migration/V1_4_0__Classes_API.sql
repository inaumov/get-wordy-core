CREATE TABLE class_info
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

CREATE TABLE class_schedule
(
    class_id    VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(3)   NOT NULL, -- E.g., "Mon", "Tue"
    start_time  TIME,
    end_time    TIME,
    FOREIGN KEY (class_id) REFERENCES class_info (class_id) ON DELETE CASCADE
);
