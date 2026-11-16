-- Demo data. Runs only when the database is empty (or on admin "Reset demo data").

INSERT INTO HOSTEL (hostel_id, hostel_name, score_type, cutoff, occupancy_options) VALUES
 (1, 'Aravali House',     'PERCENTILE', 85.0, 'DOUBLE,TRIPLE'),
 (2, 'Nilgiri Residency', 'CGPA',        8.0, 'SINGLE,DOUBLE'),
 (3, 'Shivalik Hall',     'CGPA',        6.5, 'DOUBLE,TRIPLE');

INSERT INTO STUDENT (student_id, name, roll_no, email, program, academic_year, score_type, score,
    verification_status, sleep_time, wake_time, cleanliness, noise_tolerance, study_in_room, study_time,
    room_environment, guest_frequency, food_preference, food_sharing, room_organization,
    hostel_pref_ids, occupancy_pref) VALUES
-- Year 1 (percentile)
 (1,  'Aarav Sharma',     '24BCE1001', 'aarav.sharma@uni.edu',     'B.Tech Computer Science',   1, 'PERCENTILE', 92.4, 'VERIFIED',      '23:00', '07:00', 8, 4, 'YES',       'NIGHT',     'QUIET',    'RARELY',    'VEG',     'YES',       'VERY_ORGANIZED', '1', 'DOUBLE'),
 (2,  'Ishaan Mehta',     '24BCE1002', 'ishaan.mehta@uni.edu',     'B.Tech Computer Science',   1, 'PERCENTILE', 88.1, 'SELF_REPORTED', '23:30', '07:15', 7, 4, 'YES',       'NIGHT',     'QUIET',    'RARELY',    'VEG',     'SOMETIMES', 'VERY_ORGANIZED', '1', 'DOUBLE'),
 (3,  'Kabir Nair',       '24BEC1003', 'kabir.nair@uni.edu',       'B.Tech Electronics',        1, 'PERCENTILE', 90.7, 'SELF_REPORTED', '01:30', '09:00', 4, 8, 'NO',        'EVENING',   'LIVELY',   'OFTEN',     'NON_VEG', 'YES',       'RELAXED',        '1', 'TRIPLE'),
 (4,  'Rohan Gupta',      '24BME1004', 'rohan.gupta@uni.edu',      'B.Tech Mechanical',         1, 'PERCENTILE', 86.0, 'SELF_REPORTED', '00:00', '07:30', 8, 5, 'SOMETIMES', 'NIGHT',     'BALANCED', 'RARELY',    'EGG',     'YES',       'MODERATE',       '1', 'DOUBLE'),
 (5,  'Vivaan Reddy',     '24BCE1005', 'vivaan.reddy@uni.edu',     'B.Tech Computer Science',   1, 'PERCENTILE', 79.5, 'SELF_REPORTED', '22:30', '06:00', 9, 3, 'YES',       'MORNING',   'QUIET',    'NEVER',     'VEG',     'NO',        'VERY_ORGANIZED', '1', 'DOUBLE'),
 (6,  'Arjun Patel',      '24BIT1006', 'arjun.patel@uni.edu',      'B.Tech Information Tech',   1, 'PERCENTILE', 95.2, 'SELF_REPORTED', '22:45', '06:30', 9, 3, 'YES',       'MORNING',   'QUIET',    'NEVER',     'VEG',     'YES',       'VERY_ORGANIZED', '1', 'DOUBLE'),
-- Year 2 (CGPA)
 (7,  'Ananya Iyer',      '23BCE1101', 'ananya.iyer@uni.edu',      'B.Tech Computer Science',   2, 'CGPA', 8.6, 'VERIFIED',      '23:00', '07:00', 8, 4, 'YES',       'NIGHT',     'QUIET',    'RARELY',    'VEG',     'YES',       'VERY_ORGANIZED', '2,3', 'DOUBLE'),
 (8,  'Diya Kapoor',      '23BCE1102', 'diya.kapoor@uni.edu',      'B.Tech Computer Science',   2, 'CGPA', 8.2, 'SELF_REPORTED', '23:15', '07:00', 8, 5, 'YES',       'NIGHT',     'QUIET',    'RARELY',    'VEG',     'YES',       'MODERATE',       '2,3', 'DOUBLE'),
 (9,  'Meera Joshi',      '23BEC1103', 'meera.joshi@uni.edu',      'B.Tech Electronics',        2, 'CGPA', 7.4, 'SELF_REPORTED', '23:30', '07:30', 7, 4, 'SOMETIMES', 'NIGHT',     'BALANCED', 'RARELY',    'EGG',     'YES',       'VERY_ORGANIZED', '3',   'DOUBLE'),
 (10, 'Riya Singh',       '23BME1104', 'riya.singh@uni.edu',       'B.Tech Mechanical',         2, 'CGPA', 6.9, 'SELF_REPORTED', '01:00', '09:30', 3, 9, 'NO',        'AFTERNOON', 'LIVELY',   'OFTEN',     'NON_VEG', 'YES',       'RELAXED',        '3',   'TRIPLE'),
 (11, 'Sneha Kulkarni',   '23BIT1105', 'sneha.kulkarni@uni.edu',   'B.Tech Information Tech',   2, 'CGPA', 9.1, 'SELF_REPORTED', '22:30', '06:00', 9, 2, 'YES',       'MORNING',   'QUIET',    'NEVER',     'VEG',     'NO',        'VERY_ORGANIZED', '2',   'SINGLE'),
 (12, 'Tanvi Desai',      '23BCE1106', 'tanvi.desai@uni.edu',      'B.Tech Computer Science',   2, 'CGPA', 7.8, 'SELF_REPORTED', '00:30', '08:00', 6, 6, 'SOMETIMES', 'EVENING',   'BALANCED', 'SOMETIMES', 'NON_VEG', 'SOMETIMES', 'MODERATE',       '3',   'DOUBLE'),
-- Year 3 (CGPA)
 (13, 'Aditya Rao',       '22BCE1201', 'aditya.rao@uni.edu',       'B.Tech Computer Science',   3, 'CGPA', 8.4, 'VERIFIED',      '00:00', '08:00', 7, 6, 'YES',       'NIGHT',     'BALANCED', 'SOMETIMES', 'NON_VEG', 'YES',       'MODERATE',       '2,3', 'DOUBLE'),
 (14, 'Nikhil Verma',     '22BEC1202', 'nikhil.verma@uni.edu',     'B.Tech Electronics',        3, 'CGPA', 7.9, 'SELF_REPORTED', '00:15', '08:00', 7, 5, 'YES',       'NIGHT',     'BALANCED', 'SOMETIMES', 'NON_VEG', 'YES',       'MODERATE',       '3,2', 'DOUBLE'),
 (15, 'Pranav Bhat',      '22BME1203', 'pranav.bhat@uni.edu',      'B.Tech Mechanical',         3, 'CGPA', 6.8, 'SELF_REPORTED', '23:45', '07:30', 6, 6, 'SOMETIMES', NULL,        'BALANCED', 'RARELY',    'EGG',     NULL,        'MODERATE',       '3',   'TRIPLE'),
 (16, 'Siddharth Menon',  '22BIT1204', 'siddharth.menon@uni.edu',  'B.Tech Information Tech',   3, 'CGPA', 8.9, 'SELF_REPORTED', '22:00', '05:30', 9, 2, 'YES',       'MORNING',   'QUIET',    'NEVER',     'VEG',     'NO',        'VERY_ORGANIZED', '2',   'SINGLE'),
 (17, 'Kunal Saxena',     '22BCE1205', 'kunal.saxena@uni.edu',     'B.Tech Computer Science',   3, 'CGPA', 6.0, 'SELF_REPORTED', '02:00', '10:00', 3, 9, 'NO',        'NIGHT',     'LIVELY',   'OFTEN',     'NON_VEG', 'YES',       'RELAXED',        '3',   'TRIPLE'),
-- Year 4 (CGPA)
 (18, 'Neha Pillai',      '21BCE1301', 'neha.pillai@uni.edu',      'B.Tech Computer Science',   4, 'CGPA', 8.7, 'VERIFIED',      '23:00', '07:00', 8, 4, 'YES',       'MORNING',   'QUIET',    'RARELY',    'VEG',     'YES',       'VERY_ORGANIZED', '2,3', 'DOUBLE'),
 (19, 'Pooja Agarwal',    '21BEC1302', 'pooja.agarwal@uni.edu',    'B.Tech Electronics',        4, 'CGPA', 7.2, 'SELF_REPORTED', '23:30', '07:00', 7, 5, 'YES',       'EVENING',   'BALANCED', 'RARELY',    'VEG',     'YES',       'MODERATE',       '3',   'DOUBLE'),
 (20, 'Lakshmi Krishnan', '21BME1303', 'lakshmi.krishnan@uni.edu', 'B.Tech Mechanical',         4, 'CGPA', 8.1, 'SELF_REPORTED', '23:15', NULL,    NULL, NULL, NULL,     NULL,        NULL,       NULL,        NULL,      NULL,        NULL,             '2',   'DOUBLE'),
 (21, 'Farhan Ali',       '21BIT1304', 'farhan.ali@uni.edu',       'B.Tech Information Tech',   4, 'CGPA', 6.7, 'SELF_REPORTED', '00:30', '08:30', 5, 7, 'SOMETIMES', 'NIGHT',     'LIVELY',   'SOMETIMES', 'NON_VEG', 'YES',       'RELAXED',        '3',   'TRIPLE');

INSERT INTO REQUIREMENT (student_id, factor, importance_level) VALUES
 (1,  'SLEEP',       'MUST'),
 (1,  'CLEANLINESS', 'PREFERABLE'),
 (7,  'CLEANLINESS', 'MUST'),
 (7,  'FOOD',        'NOT_IMPORTANT'),
 (8,  'SOCIAL',      'PREFERABLE'),
 (11, 'NOISE',       'MUST'),
 (13, 'NOISE',       'MUST'),
 (13, 'STUDY',       'PREFERABLE'),
 (18, 'SLEEP',       'PREFERABLE'),
 (18, 'SOCIAL',      'MUST');
