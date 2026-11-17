-- RoomSync schema (SQLite). Recommendation only: no allocation, booking or approval tables.

CREATE TABLE IF NOT EXISTS HOSTEL (
    hostel_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    hostel_name       TEXT NOT NULL UNIQUE,
    score_type        TEXT NOT NULL CHECK (score_type IN ('PERCENTILE', 'CGPA')),
    cutoff            REAL NOT NULL,
    occupancy_options TEXT NOT NULL,
    CHECK ((score_type = 'PERCENTILE' AND cutoff BETWEEN 0 AND 100)
        OR (score_type = 'CGPA' AND cutoff BETWEEN 0 AND 10))
);

CREATE TABLE IF NOT EXISTS STUDENT (
    student_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name                TEXT NOT NULL,
    roll_no             TEXT NOT NULL UNIQUE,
    email               TEXT NOT NULL,
    program             TEXT NOT NULL,
    academic_year       INTEGER NOT NULL CHECK (academic_year BETWEEN 1 AND 4),
    score_type          TEXT NOT NULL CHECK (score_type IN ('PERCENTILE', 'CGPA')),
    score               REAL NOT NULL,
    verification_status TEXT NOT NULL DEFAULT 'SELF_REPORTED'
                        CHECK (verification_status IN ('SELF_REPORTED', 'VERIFIED')),
    sleep_time          TEXT,
    wake_time           TEXT,
    cleanliness         INTEGER CHECK (cleanliness BETWEEN 1 AND 10),
    noise_tolerance     INTEGER CHECK (noise_tolerance BETWEEN 1 AND 10),
    study_in_room       TEXT,
    study_time          TEXT,   -- one or more of MORNING, AFTERNOON, EVENING, NIGHT, comma separated
    room_environment    TEXT,
    guest_frequency     TEXT,
    food_preference     TEXT,
    food_sharing        TEXT,
    room_organization   TEXT,
    hostel_pref_ids     TEXT,
    occupancy_pref      TEXT,   -- one or more of SINGLE, DOUBLE, TRIPLE, comma separated
    gender              TEXT NOT NULL DEFAULT 'MALE' CHECK (gender IN ('MALE', 'FEMALE')),
    -- First years are scored by percentile, years 2 to 4 by CGPA. Never mixed.
    CHECK ((academic_year = 1 AND score_type = 'PERCENTILE')
        OR (academic_year > 1 AND score_type = 'CGPA')),
    CHECK ((score_type = 'PERCENTILE' AND score BETWEEN 0 AND 100)
        OR (score_type = 'CGPA' AND score BETWEEN 0 AND 10))
);

CREATE TABLE IF NOT EXISTS REQUIREMENT (
    student_id       INTEGER NOT NULL REFERENCES STUDENT(student_id) ON DELETE CASCADE,
    factor           TEXT NOT NULL,
    importance_level TEXT NOT NULL CHECK (importance_level IN ('MUST', 'PREFERABLE', 'NOT_IMPORTANT')),
    PRIMARY KEY (student_id, factor)
);

-- Only records that a student is interested in a candidate. It is not an allocation.
CREATE TABLE IF NOT EXISTS INTEREST (
    student_id   INTEGER NOT NULL REFERENCES STUDENT(student_id) ON DELETE CASCADE,
    candidate_id INTEGER NOT NULL REFERENCES STUDENT(student_id) ON DELETE CASCADE,
    created_at   TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (student_id, candidate_id),
    CHECK (student_id <> candidate_id)
);
