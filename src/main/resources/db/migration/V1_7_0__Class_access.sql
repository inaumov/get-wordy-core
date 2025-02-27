-- shared access to the class
CREATE TABLE class_access
(
    class_id  VARCHAR(255) NOT NULL,
    viewer_id VARCHAR(255) NOT NULL,
    is_active BOOLEAN,
    PRIMARY KEY (class_id, viewer_id),
    FOREIGN KEY (class_id) REFERENCES class_info (class_id) ON DELETE CASCADE
);
