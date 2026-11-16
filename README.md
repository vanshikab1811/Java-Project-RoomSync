# RoomSync

A small Java desktop app that shows which hostels a student is **academically eligible** for and recommends their **top 3 compatible potential roommates**.

RoomSync is recommendation only. It never allocates hostels, assigns rooms, reserves beds, books or approves anything, and it does not track availability. "Express interest" only records a preference signal.

## Prerequisites

- **JDK 17 or newer.** Nothing else is needed: no Maven, Gradle or database server.
- The only library, `lib/sqlite-jdbc-3.36.0.3.jar`, is already in the project. If it is ever missing, download it from
  `https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar` and place it in `lib/`.

## Build and run

Windows:

```bat
build.bat
run.bat
```

Mac and Linux:

```sh
sh build.sh
sh run.sh
```

Run the tests (prints PASS or FAIL per check) with `run.bat test` or `sh run.sh test`.

The raw commands, run from the project folder, are:

```sh
# Windows (cmd)
javac -encoding UTF-8 -d out -cp "lib/*" src\com\roomsync\*.java src\com\roomsync\model\*.java src\com\roomsync\db\*.java src\com\roomsync\logic\*.java src\com\roomsync\ui\*.java
java -cp "out;lib/*" com.roomsync.Main
java -cp "out;lib/*" com.roomsync.TestRunner

# Mac and Linux
javac -encoding UTF-8 -d out -cp "lib/*" $(find src -name "*.java")
java -cp "out:lib/*" com.roomsync.Main
java -cp "out:lib/*" com.roomsync.TestRunner
```

Always start the app from the project folder, because it reads `resources/` and creates `roomsync.db` in the working directory. The scripts do this for you.

If a `jdk` folder exists in the project (a portable JDK), the scripts use it; otherwise they use `java` and `javac` from your PATH.

## Using the app

1. **Sign in / Registration:**
   - Sign in with an existing roll number, or click **"New student? Fill registration form ↗"** to open the registration questionnaire in your browser.
   - Demo roll numbers:
     - `24BCE1001` (Year 1, percentile, with a must-have that excludes one candidate)
     - `23BCE1101` (Year 2, CGPA)
     - `21BCE1301` (Year 4, only two valid matches)
     - `24BCE1005` (below every cutoff, shows the "no eligible hostels" state)
     - `21BME1303` (too few lifestyle answers, shows the "insufficient data" state)
2. **Profile** shows your details, score type, verification status and eligible hostels with occupancy options.
3. **Matches** shows up to three cards. **View details** opens the score breakdown, compatible habits, conflicts and the optional **Express interest** button.
4. **Admin** (password `admin123`, set in `resources/config.properties`) can import a CSV, mark a score as verified, and reset the demo data.

## Registration Form & Automatic Database Sync

RoomSync supports two automated ways to register new students and update the database:

### Option A: Live Google Form + Automatic Sheet Sync
If you are using Google Forms (like the URL in `config.properties`):
1. In your Google Form, open the **Responses** tab and click **"Link to Sheets"** (or view linked sheet).
2. In that Google Sheet:
   - Click **File > Share > Share with others**.
   - Under General Access, change "Restricted" to **"Anyone with the link can view"**.
   - Copy the Google Sheet link.
3. Open `resources/config.properties` and paste your link:
   ```properties
   sheet.sync.url=https://docs.google.com/spreadsheets/d/YOUR_SHEET_ID/edit
   ```
4. **How the auto-sync works:**
   - When a student fills the Google Form and then enters their roll number on RoomSync's login screen, the app **automatically queries the Google Sheet**, imports the new student into SQLite, and logs them in immediately!
   - RoomSync also polls the sheet in the background every 10 seconds.
   - You can also click **"Sync Google Sheet"** inside the Admin panel.

### Option B: Built-in Interactive Web Form (`resources/registration_form.html`)
- If `form.url=resources/registration_form.html`, clicking **"New student? Fill registration form ↗"** opens the local web form.
- When the student clicks **"Register with RoomSync"**, it submits directly to RoomSync's embedded HTTP server (`localhost:8080/api/register`).
- The database is updated in real time, the login screen automatically fills their roll number, and clicking **Continue** signs in instantly.

## Resetting the demo data

Either:

- delete `roomsync.db` and start the app again (it is recreated and reseeded), or
- open **Admin** and choose **Reset demo data**.

## Importing the sample CSV

Open **Admin**, unlock it, and click **Import sample CSV** (or **Choose CSV file** for your own Google Form export). `resources/sample_responses.csv` has 8 rows: 5 import, and 3 are rejected on purpose so you can see the error report (a duplicate roll number, a CGPA of 11.2 with an invalid time, and a row without consent).

The CSV must use the same column headers as the sample. Answers can be written as people type them in a form ("Very organized", "Non-veg", "2nd Year", "Must have"); they are normalised on import. Each row is checked for required fields, unique roll number, score range (percentile 0 to 100 for Year 1, CGPA 0 to 10 for Years 2 to 4), HH:mm times and consent.

## How the scoring works

**Eligibility.** Year 1 students are compared by percentile and Years 2 to 4 by CGPA, never mixed. A student is eligible for a hostel when the hostel uses the same score type and their score is at or above its cutoff. Scores are labelled *Self reported* until an admin marks them *Verified*. Eligibility never means a room is available.

**Who can be a match.** A candidate must be a different student, in the same academic year, and share at least one eligible hostel.

**Factor scores (0 to 100).**

| Factor | Weight | Compared using |
| --- | --- | --- |
| Sleep schedule | 20% | bedtime |
| Cleanliness | 15% | 1 to 10 rating |
| Noise tolerance | 15% | 1 to 10 rating |
| Study habits | 15% | study in room, preferred study time |
| Daily schedule | 10% | wake-up time |
| Room environment | 10% | quiet/balanced/lively, room organisation |
| Guests and social | 10% | guest frequency |
| Food | 5% | food preference, food sharing |

- **Times** use the circular difference around midnight, so 23:00 and 01:00 are 2 hours apart, not 22. Same time scores 100, falling to 0 at 4 hours apart.
- **1 to 10 ratings** score `100 - |a - b| / 9 * 100`.
- **Choices** use a small similarity table: identical 100, neighbouring options 50, further apart 0.

**Total.** `sum(weight x importance multiplier x factor score) / sum(weight x importance multiplier)`, counting only factors both students answered. Missing answers are skipped and their weight is shared across the others; they are never treated as a conflict. At least 3 factors are needed to compute a score.

**Importance.** Each student can mark a factor *Must have* (x2.0), *Preferable* (x1.5) or *Not important* (x0.25). If a must-have factor (from either student) scores below 50, the candidate is excluded from the top 3 and the reason is shown.

**Conflicts.** Factors scoring under 70 are listed as conflicts: below 30 is High, below 50 Medium, otherwise Low. Factors scoring 80 or more are listed as compatible habits.

The top 3 valid candidates by score are shown. Weights, multipliers and thresholds live in `resources/config.properties`.

## Privacy

Other students are shown only by first name and last initial, with their year, compatibility and hostels in common. Their roll number, email and exact score are never shown, and conflict reasons describe differences ("about 2 hours apart") rather than their actual answers.

## Project layout

```
src/com/roomsync/
  Main.java, TestRunner.java
  model/  Student, Hostel, Requirement, MatchResult
  db/     Database, StudentDao, HostelDao
  logic/  EligibilityService, CompatibilityCalculator, MatchingService, CsvImporter
  ui/     Theme, MainFrame, LoginPanel, ProfilePanel, MatchesPanel, MatchDetailPanel, AdminPanel
lib/        sqlite-jdbc-3.36.0.3.jar
resources/  schema.sql, seed.sql, sample_responses.csv, config.properties
out/        compiled classes (not committed)
```
