INSERT INTO class_info (class_id, owner_id, owner_type, name, format, level, material, notes, is_repeatable)
VALUES ('class1', 'user123', 'user', 'Beginner English', 'Lecture', 'Beginner', 'Grammar Basics', 'Morning class', true),
       ('class2', 'user123', 'user', 'Intermediate English', 'Workshop', 'Intermediate', 'Speaking Practice','Interactive', true),
       ('class3', 'user123', 'user', 'Advanced English', 'Lecture', 'Advanced', 'Essay Writing', '', true),
       ('class4', 'user123', 'user', 'Business English', 'Workshop', 'Advanced', 'Corporate Communication','Evening class', true),
       ('class5', 'user123', 'user', 'English for Kids', 'Interactive', 'Beginner', 'Storybooks', 'Fun activities', true),
       ('class6', 'user123', 'user', 'English Literature', 'Lecture', 'Advanced', 'Classic Novels','Analysis and discussions', true),
       ('class7', 'user123', 'user', 'IELTS Preparation', 'Lecture', 'Advanced', 'IELTS Workbook', '', true),
       ('class8', 'user123', 'user', 'TOEFL Preparation', 'Lecture', 'Advanced', 'TOEFL Guide', '', true),
       ('class9', 'user123', 'user', 'Conversational English', 'Workshop', 'Intermediate', 'Dialogue Practice', '', true),
       ('class10', 'user123', 'user', 'Grammar for Beginners', 'Lecture', 'Beginner', 'Grammar Exercises', '', true),
       ('class11', 'user123', 'user', 'Pronunciation Practice', 'Workshop', 'Intermediate', 'Audio Lessons', '', true),
       ('class12', 'user123', 'user', 'English for Travel', 'Interactive', 'Intermediate', 'Travel Phrases', '', true);

INSERT INTO class_schedule (class_id, day_of_week, start_time, end_time)
VALUES
-- Beginner English
('class1', 'Mon', '09:00', '10:00'),
('class1', 'Wed', '09:00', '10:00'),

-- Intermediate English
('class2', 'Tue', '11:00', '12:00'),
('class2', 'Thu', '11:00', '12:00'),

-- Advanced English
('class3', 'Fri', '10:00', '11:30'),
('class3', 'Sat', '10:00', '11:30'),

-- Business English
('class4', 'Mon', '18:00', '19:30'),
('class4', 'Fri', '18:00', '19:30'),

-- English for Kids
('class5', 'Sat', '14:00', '15:00'),
('class5', 'Sun', '14:00', '15:00'),

-- English Literature
('class6', 'Wed', '16:00', '17:30'),

-- IELTS Preparation
('class7', 'Thu', '14:00', '15:30'),
('class7', 'Sat', '09:00', '10:30'),

-- TOEFL Preparation
('class8', 'Tue', '14:00', '15:30'),

-- Conversational English
('class9', 'Mon', '12:00', '13:00'),
('class9', 'Wed', '12:00', '13:00'),

-- Grammar for Beginners
('class10', 'Tue', '09:00', '10:00'),
('class10', 'Thu', '09:00', '10:00'),

-- Pronunciation Practice
('class11', 'Fri', '16:00', '17:00'),
('class11', 'Sun', '16:00', '17:00'),

-- English for Travel
('class12', 'Sat', '11:00', '12:00'),
('class12', 'Sun', '11:00', '12:00');

-- Insert access permissions for viewers
INSERT INTO class_access (class_id, viewer_id, is_active)
VALUES ('class1', 'viewer123', true),
       ('class2', 'viewer123', true),
       ('class3', 'viewer999', true);
