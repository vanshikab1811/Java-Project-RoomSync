# RoomSync — Project Context

## Purpose of this document
This is the authoritative project context for **RoomSync**. It documents the application's architecture, scope boundaries, data workflows, algorithms, and technical implementations (including automated Google Form/Sheet synchronization and embedded server integration).

---

## 1. Project Overview
* **Project Name:** RoomSync
* **Project Title:** Intelligent Hostel Eligibility & Roommate Compatibility Recommendation System
* **Type:** Standalone Java desktop application with embedded SQLite database.

RoomSync helps university students discover which hostels they are **academically eligible** for and recommends their **top 3 compatible potential roommates** based on lifestyle habits and priorities.

### Critical Scope Boundary
RoomSync is **recommendation-only**.
* It does **NOT** allocate hostels, assign rooms, reserve beds, approve applications, or make final institutional decisions.
* It evaluates and displays eligible hostel options based on academic cutoffs.
* It calculates and displays the top 3 compatible potential roommates within the same academic year.
* It explains compatibility factors and highlights lifestyle conflicts.
* "Express interest" only records a mutual preference signal between students.
* Actual hostel and bed assignments are handled externally by university hostel administration.

---

## 2. Core Workflow

```
[New Student] ──> Fills Google Form (or Built-in Registration Web Form)
                          │
         ┌────────────────┴────────────────┐
         ▼                                 ▼
[Google Sheets]                   [Embedded Java Server]
 (Auto-sync via URL)              (Direct HTTP POST to localhost:8080)
         │                                 │
         └────────────────┬────────────────┘
                          ▼
            [RoomSync SQLite Database]
           (Validated & Stored in roomsync.db)
                          │
                          ▼
     [Student Signs In with University Roll Number]
                          │
         ┌────────────────┴────────────────┐
         ▼                                 ▼
[Academic Eligibility]            [Lifestyle Compatibility]
 (Percentile / CGPA)              (Weighted Algorithm & Must-Haves)
         │                                 │
         └────────────────┬────────────────┘
                          ▼
        [Top 3 Compatible Roommates & Eligible Hostels]
```

1. **Student Registration:** New or returning students fill out the standardized questionnaire via:
   * A **Live Google Form** (linked to Google Sheets), or
   * The **Built-in Registration Form** (`resources/registration_form.html`).
2. **Automated Synchronization & Data Ingestion:**
   * **Live Google Sheet Sync (`SyncService`):** RoomSync polls the linked Google Sheet in the background every 10 seconds and automatically syncs in real-time when an unregistered student attempts to log in.
   * **Direct Local Server Sync (`RegistrationServer`):** When using the web form, submissions post directly to RoomSync's embedded HTTP server (`http://localhost:8080/api/register`), inserting records instantly into SQLite.
   * **Admin Manual Import:** Administrators can also manually upload CSV exports or reset demo data via the password-protected Admin screen.
3. **Data Validation & Normalization:** [`CsvImporter`](file:///E:/Java_project/Java-Project-RoomSync/src/com/roomsync/logic/CsvImporter.java) validates required fields, unique roll numbers, academic score ranges, 24-hour time formats (`HH:mm`), and mandatory consent.
4. **Academic Eligibility Evaluation:**
   * **First-year students:** Judged strictly by admission **percentile** (0–100).
   * **Second, third, and fourth-year students:** Judged strictly by **CGPA** (0–10).
   * Percentile and CGPA are never cross-compared.
   * Scores are labeled *Self reported* until verified by an administrator.
5. **Candidate Pool Generation:**
   * **Hard constraint:** Only candidates in the **exact same academic year** are eligible for matching.
   * Candidates must share at least one mutually eligible hostel.
6. **Compatibility & Conflict Scoring:** Evaluated across 8 weighted lifestyle factors with personalized importance multipliers (*Must have*, *Preferable*, *Not important*). Candidates violating hard *Must have* requirements are excluded.
7. **Top 3 Display:** Only the top 3 roommate recommendations are shown with compatibility percentages, shared habits, potential conflicts, and an optional "Express interest" signal.

---

## 3. Academic Eligibility & Hostel Rules

* **Hostel Rules:** Each hostel specifies an academic score type (`PERCENTILE` or `CGPA`), a minimum cutoff threshold, and available occupancy types (`SINGLE`, `DOUBLE`, `TRIPLE`).
* **Evaluation:** A student is academically eligible for a hostel if and only if their score meets or exceeds the hostel's cutoff for their academic year tier.
* Eligibility indicates qualification to reside in the hostel; it does **not** guarantee room or bed availability.

---

## 4. Roommate Compatibility Algorithm

Compatibility scoring is transparent, explainable, and deterministic (no black-box AI/ML):

### Factor Weights (Default 100% Total)
| Factor | Weight | Comparison Method |
|---|---:|---|
| **Sleep schedule** | 20% | Circular time difference around midnight (0–4 hours) |
| **Cleanliness** | 15% | Linear difference on 1–10 rating scale |
| **Noise tolerance** | 15% | Linear difference on 1–10 rating scale |
| **Study habits** | 15% | Study in room (`YES`, `SOMETIMES`, `NO`) & preferred time |
| **Daily schedule** | 10% | Circular time difference for wake-up time |
| **Room environment** | 10% | Atmosphere preference (`QUIET`, `BALANCED`, `LIVELY`) |
| **Social / Guest habits** | 10% | Guest frequency (`NEVER`, `RARELY`, `SOMETIMES`, `OFTEN`) |
| **Food habits** | 5% | Diet type (`VEG`, `EGG`, `NON_VEG`) & food sharing |

### Importance Multipliers
Each factor can be weighted by student preference:
* **Must have (x2.0):** If either student designates a factor as *Must have* and their pair score falls below 50, the candidate is **strictly excluded** from recommendations with the violation reason recorded.
* **Preferable (x1.5):** Increases the relative weight of the factor.
* **Not important (x0.25):** Reduces the factor's impact on the overall score.

### Missing Data & Conflicts
* Missing optional answers are skipped without penalty; weights are dynamically redistributed across answered factors.
* A minimum of 3 mutually answered factors is required to compute a score.
* Factors scoring below 70 are categorized as **Conflicts** (High: <30, Medium: <50, Low: <70).
* Factors scoring 80 or above are highlighted as **Compatible Habits**.

---

## 5. Google Form & Registration Integration

### Google Form Architecture (5 Sections)
1. **Section 1 — Personal & Academic Details:** Full Name, Roll Number (unique ID), University Email, Program, Academic Year (1st–4th), Score (Percentile for Year 1; CGPA for Years 2–4), Application Type.
2. **Section 2 — Hostel Preferences:** Hostel choices, Occupancy preference, Preferred floor, Special accommodations, Willingness to consider other eligible hostels.
3. **Section 3 — Lifestyle Preferences:** Sleep time (`HH:mm`), Wake time (`HH:mm`), Cleanliness (1–10), Noise tolerance (1–10), Study habits, Room environment, Guest frequency, Food preference & sharing, Room organization.
4. **Section 4 — Important Requirements:** Importance grid (Must have / Preferable / Not important for all 8 factors), Intolerable habits checkboxes, Guest policy.
5. **Section 5 — Consent & Submit:** Accuracy confirmation, consent for matching, recommendation-only acknowledgment.

### Implemented Automation Utilities
* **1-Click Google Form Generator (`resources/create_google_form.gs`):** Google Apps Script that automatically creates the complete 5-section form and linked Google Sheet in Google Drive.
* **Live Google Sheet Sync (`com.roomsync.logic.SyncService`):** Reads the public/shared CSV export of the linked Google Sheet (`sheet.sync.url` in `config.properties`), supporting auto-redirects and background polling.
* **Standalone Interactive Web Form (`resources/registration_form.html`):** Client-side replica with direct HTTP submission to RoomSync and CSV backup download.
* **Embedded Local HTTP Server (`com.roomsync.logic.RegistrationServer`):** Built-in `jdk.httpserver` on `http://localhost:8080` receiving instant form submissions without external dependencies.

---

## 6. Technical Stack & Project Layout

* **Language:** Java 17+ (Core Java, standard libraries only).
* **GUI Toolkit:** Java Swing (custom-designed flat modern card theme with soft drop shadows).
* **Database:** Embedded SQLite via JDBC driver (`lib/sqlite-jdbc-3.36.0.3.jar`).
* **HTTP / Networking:** `com.sun.net.httpserver.HttpServer` (standard JDK module `jdk.httpserver`).
* **Build System:** Plain `javac` via `build.bat` (Windows) and `build.sh` (Mac/Linux). Zero Maven/Gradle dependency.

### Directory Structure
```
Java-Project-RoomSync/
├── lib/
│   └── sqlite-jdbc-3.36.0.3.jar
├── resources/
│   ├── config.properties          # Scoring weights, passwords, form & sheet URLs
│   ├── schema.sql                 # SQLite schema
│   ├── seed.sql                   # Initial demo seed data (360 students, 3 hostels)
│   ├── sample_responses.csv       # Test CSV import data
│   ├── registration_form.html     # Standalone registration web form
│   └── create_google_form.gs      # 1-click Google Apps Script form generator
├── src/com/roomsync/
│   ├── Main.java                  # Application entry point & config loader
│   ├── TestRunner.java            # Automated test suite (25 unit tests)
│   ├── model/                     # Student, Hostel, Requirement, MatchResult
│   ├── db/                        # Database, StudentDao, HostelDao
│   ├── logic/
│   │   ├── CompatibilityCalculator.java
│   │   ├── EligibilityService.java
│   │   ├── MatchingService.java
│   │   ├── CsvImporter.java       # Resilient CSV parser & validator
│   │   ├── SyncService.java       # Automated Google Sheet synchronizer
│   │   └── RegistrationServer.java# Embedded HTTP registration receiver
│   └── ui/                        # Theme, MainFrame, LoginPanel, ProfilePanel,
│                                  # MatchesPanel, MatchDetailPanel, AdminPanel
├── build.bat / build.sh           # Build scripts
├── run.bat / run.sh               # Execution scripts
├── README.md                      # User & developer manual
└── PROJECT_CONTEXT.md             # Authoritative project specification (this document)
```

---

## 7. Security, Privacy & Non-Functional Requirements

1. **Student Privacy:** Match results display only first names and last initials (e.g. "Aditi B."). Sensitive data (roll numbers, emails, exact scores, contact info) are never displayed to other students.
2. **Explainable Scoring:** The algorithm provides a detailed score breakdown showing why two students matched and explains differences without revealing private individual answers.
3. **Robust Input Handling:** Time values handle midnight crossovers seamlessly; numbers, scales, and score ranges are strictly bound; Google Sheet grid headers with or without brackets (`Importance [Factor]`) are handled transparently.

