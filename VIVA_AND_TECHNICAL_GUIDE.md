# RoomSync — Simple Technical & Viva Preparation Guide

> **Easy-to-read guide for your project defense and viva voce.**  
> Everything is explained in plain, simple English with a complete step-by-step mathematical example.

---

## 1. What is RoomSync? (In 2 Sentences)

**RoomSync** is a desktop app for university students that does two things:
1. It checks your academic score (percentile or CGPA) and tells you **which hostels you are eligible for**.
2. It looks at your daily living habits and recommends your **top 3 most compatible roommates**.

> **Crucial Rule for Viva:** RoomSync is **recommendation-only**. It suggests good roommates; it **does NOT** book beds, assign rooms, or replace the hostel warden/office.

---

## 2. Technologies Used & Why We Used Them (Easy Explanations)

When the examiner asks: *"What technologies did you use and why?"*, here is what you say:

### 1. Java 17 (The Core Language)
* **What it does:** Runs the entire application and calculates roommate compatibility.
* **Why we used it:** Java is strongly typed, platform-independent (runs on Windows, Mac, Linux), and has built-in multithreading so the app doesn't freeze when syncing data.

### 2. Java Swing (The Desktop Interface)
* **What it does:** Builds the desktop screens (Login, Profile, Top 3 Matches, Match Details, Admin).
* **Why we used it:** It comes pre-installed inside Java! We didn't need to install external frameworks like React, Node.js, or Electron. We styled it with modern rounded cards and soft shadows.

### 3. SQLite Database (`roomsync.db` via JDBC)
* **What it does:** Stores all students, hostels, and roommate preferences locally.
* **Why we used it:** Unlike MySQL, SQLite is a **serverless database file**. You don't need to start a MySQL background server or enter database passwords. The app opens and works immediately on any computer.

### 4. Embedded HTTP Server (`jdk.httpserver`)
* **What it does:** Runs a tiny local web server on `http://localhost:8080`.
* **Why we used it:** When a student fills out the registration form in their web browser, the browser immediately sends the data to this local server, and it saves directly into SQLite in milliseconds without needing Spring Boot or Tomcat.

### 5. Google Forms & Google Sheets
* **What it does:** Lets students submit their lifestyle survey online from anywhere.
* **Why we used it:** Google Forms is free and easy for students. RoomSync automatically connects to the linked Google Sheet to pull new student submissions.

---

## 3. How the Academic Eligibility Check Works

The rule is very simple and strictly prevents mixing different scoring systems:
* **1st Year Students:** Evaluated **only by admission Percentile** (0 to 100).
* **2nd, 3rd & 4th Year Students:** Evaluated **only by CGPA** (0 to 10).
* **Why never mix?** A CGPA of 9.0 is completely different from a 90th percentile. Therefore:
  * **Aravali House:** Requires $\ge 85.0$ Percentile (1st Years only).
  * **Nilgiri Residency:** Requires $\ge 8.0$ CGPA (Senior Years).
  * **Shivalik Hall:** Requires $\ge 6.5$ CGPA (Senior Years).

If a student's score meets or exceeds the cutoff, that hostel shows up as **Eligible**.

---

## 4. How Roommate Compatibility is Calculated (Step-by-Step with Real Numbers)

This is the most important part of the viva. Here is the exact step-by-step logic.

### Step 1: Pre-Filtering (Who is allowed to be compared?)
Before calculating scores, RoomSync filters out anyone who cannot be a roommate:
1. **Same-Year Rule:** 1st years match ONLY with 1st years. 2nd years match ONLY with 2nd years. Cross-year matching is blocked.
2. **Shared Hostel Rule:** Both students must share at least one mutually eligible hostel.
3. **Self-Exclusion:** You cannot match with yourself.

---

### Step 2: The 8 Lifestyle Factors (Weights total 100%)

Each factor produces a score from **0 to 100**:

| Factor | Default Weight | How It Is Measured |
|---|---:|---|
| **Sleep Schedule** | **20%** | Bedtime (e.g., 23:00 vs 00:30) |
| **Cleanliness** | **15%** | Rating from 1 to 10 |
| **Noise Tolerance** | **15%** | Rating from 1 to 10 |
| **Study Habits** | **15%** | Study in room (`YES`/`SOMETIMES`/`NO`) + Study time |
| **Daily Schedule** | **10%** | Wake-up time (e.g., 07:00 vs 07:30) |
| **Room Environment** | **10%** | Atmosphere (`QUIET`/`BALANCED`/`LIVELY`) + Organization |
| **Guests & Social** | **10%** | Guest frequency (`NEVER`/`RARELY`/`SOMETIMES`/`OFTEN`) |
| **Food Habits** | **5%** | Diet (`VEG`/`EGG`/`NON_VEG`) + Sharing comfort |

---

### Step 3: Formulas Used for Individual Factor Scores

#### A. Time Comparison (Circular Midnight Difference)
* **The Problem:** In normal math, `23:00` (11 PM) and `01:00` (1 AM) are 22 hours apart. But on a clock, they are only **2 hours apart**!
* **The Solution:** We convert both times into minutes from midnight, find the difference $\Delta t$, and take the shortest path around the clock:
  $$\text{Circular Minutes} = \min(|t_1 - t_2|, 1440 - |t_1 - t_2|)$$
* **Score Formula:**
  If the difference is 0 minutes $\rightarrow$ Score = **100**.  
  If the difference is 4 hours (240 mins) or more $\rightarrow$ Score = **0**.  
  $$\text{Score} = 100 - \left(\frac{\text{Minutes Apart}}{240} \times 100\right)$$

#### B. 1-to-10 Scale Ratings (Cleanliness & Noise)
* Formula:
  $$\text{Score} = 100 - \left(\frac{|A - B|}{9} \times 100\right)$$
  *(Example: Rating 8 vs Rating 7 $\rightarrow$ Difference is 1 $\rightarrow 100 - (1/9 \times 100) = 88.9$)*

#### C. Multiple Choice Options (Choices like Quiet, Balanced, Lively)
* Identical choices (e.g. Quiet & Quiet) $\rightarrow$ **100 points**
* Neighboring/Adjacent choices (e.g. Quiet & Balanced) $\rightarrow$ **50 points**
* Opposite choices (e.g. Quiet & Lively) $\rightarrow$ **0 points**

---

### Step 4: Complete Worked Example with Real Numbers

Let's calculate the compatibility between **Aarav** and **Rohan**:

#### 1. Calculate each factor score (0 to 100):
1. **Sleep Schedule (Weight: 20):**
   * Aarav sleeps at `23:00`, Rohan sleeps at `00:00` (60 minutes apart).
   * $\text{Score} = 100 - (60 / 240 \times 100) = \mathbf{75.0}$
2. **Cleanliness (Weight: 15):**
   * Aarav rated `8`, Rohan rated `8` (difference = 0).
   * $\text{Score} = \mathbf{100.0}$
3. **Noise Tolerance (Weight: 15):**
   * Aarav rated `4`, Rohan rated `5` (difference = 1).
   * $\text{Score} = 100 - (1 / 9 \times 100) = \mathbf{88.9}$
4. **Study Habits (Weight: 15):**
   * Study in room: Aarav `YES`, Rohan `SOMETIMES` (adjacent = 50).
   * Study time: Aarav `NIGHT`, Rohan `NIGHT` (identical = 100).
   * Average $\text{Score} = (50 + 100) / 2 = \mathbf{75.0}$
5. **Daily Schedule (Weight: 10):**
   * Aarav wakes at `07:00`, Rohan wakes at `07:30` (30 mins apart).
   * $\text{Score} = 100 - (30 / 240 \times 100) = \mathbf{87.5}$
6. **Room Environment (Weight: 10):**
   * Atmosphere: Aarav `QUIET`, Rohan `BALANCED` (adjacent = 50).
   * Organization: Aarav `VERY_ORGANIZED`, Rohan `MODERATE` (adjacent = 50).
   * Average $\text{Score} = (50 + 50) / 2 = \mathbf{50.0}$
7. **Guests & Social (Weight: 10):**
   * Aarav `RARELY`, Rohan `RARELY` (identical = 100).
   * $\text{Score} = \mathbf{100.0}$
8. **Food Habits (Weight: 5):**
   * Diet: Aarav `VEG`, Rohan `EGG` (adjacent = 50).
   * Food sharing: Aarav `YES`, Rohan `YES` (identical = 100).
   * Average $\text{Score} = (50 + 100) / 2 = \mathbf{75.0}$

---

#### 2. Apply Importance Multipliers:
Each student can prioritize what matters to them:
* **Must Have:** Multiplier = **$\times 2.0$** (If score is $< 50$, candidate is rejected!)
* **Preferable:** Multiplier = **$\times 1.5$**
* **Not Important:** Multiplier = **$\times 0.25$**
* **Normal:** Multiplier = **$\times 1.0$**

Suppose Aarav has these priorities:
* Cleanliness: **Must Have** ($\times 2.0$)
* Daily Schedule: **Not Important** ($\times 0.25$)
* All other factors: **Normal** ($\times 1.0$)

Let's calculate the effective weights:
* Sleep: $20 \times 1.0 = 20.0$
* Cleanliness: $15 \times 2.0 = 30.0$
* Noise: $15 \times 1.0 = 15.0$
* Study: $15 \times 1.0 = 15.0$
* Daily Schedule: $10 \times 0.25 = 2.5$
* Environment: $10 \times 1.0 = 10.0$
* Guests: $10 \times 1.0 = 10.0$
* Food: $5 \times 1.0 = 5.0$
* **Total Weight Sum:** $20 + 30 + 15 + 15 + 2.5 + 10 + 10 + 5 = \mathbf{107.5}$

---

#### 3. Calculate Final Weighted Score:
$$\text{Weighted Points} = \sum (\text{Effective Weight} \times \text{Factor Score})$$

* Sleep: $20.0 \times 75.0 = 1500.0$
* Cleanliness: $30.0 \times 100.0 = 3000.0$
* Noise: $15.0 \times 88.9 = 1333.5$
* Study: $15.0 \times 75.0 = 1125.0$
* Daily Schedule: $2.5 \times 87.5 = 218.75$
* Environment: $10.0 \times 50.0 = 500.0$
* Guests: $10.0 \times 100.0 = 1000.0$
* Food: $5.0 \times 75.0 = 375.0$
* **Total Points:** $9052.25$

$$\text{Final Compatibility Score} = \frac{9052.25}{107.5} = \mathbf{84.2\%}$$

---

### Step 5: Must-Have Checks, Positives, and Conflicts

1. **Must-Have Check:**
   Aarav marked Cleanliness as *Must Have*. The score was $100.0$ (which is $\ge 50$), so Rohan is **kept as a valid candidate**.
   *(If the score had been $< 50$, Rohan would be immediately eliminated from Aarav's top 3!)*
2. **Compatible Habits ($\ge 80\%$):**
   * Cleanliness (100%) &rarr; Listed as a shared strength!
   * Guests & Social (100%) &rarr; Listed as a shared strength!
   * Noise Tolerance (88.9%) &rarr; Listed as a shared strength!
   * Daily Schedule (87.5%) &rarr; Listed as a shared strength!
3. **Conflicts ($< 70\%$):**
   * Room Environment scored 50% &rarr; Listed as a **Low Conflict** (*"room environment preferences partly match"*).

---

### Step 6: Picking the Top 3
RoomSync runs this calculation for every eligible student in the same year, sorts them from highest score to lowest score, and displays **only the top 3**.

---

## 5. How the Automated Database Sync Works

When asked: *"How does the database update when a user submits a Google Form?"*

### For Google Forms:
1. The student submits the Google Form online.
2. The response automatically appears in the linked Google Sheet.
3. In `resources/config.properties`, we provide the Google Sheet link in `sheet.sync.url`.
4. RoomSync reads that sheet in two smart ways:
   * **Background Sync:** A background thread silently checks the sheet every 10 seconds.
   * **On-Demand Login Sync:** If a new student immediately enters their roll number on the login screen, RoomSync fetches the Google Sheet on the spot, saves the new student to SQLite, and logs them in!

### For the Local Web Form:
1. Clicking *"New student? Fill registration form ↗"* opens `resources/registration_form.html`.
2. Clicking *"Register with RoomSync"* sends an HTTP POST request to RoomSync's embedded local server on `http://localhost:8080/api/register`.
3. RoomSync saves the user directly into SQLite, updates the login box with their roll number, and lets them sign in immediately.

---

## 6. Top Viva Questions & Crisp Answers

### Q1: Why use an embedded SQLite database instead of MySQL?
> **Answer:** *"SQLite runs directly inside the application process as a single file (`roomsync.db`). It does not require installing or running a separate MySQL server service. For a desktop application, SQLite is lighter, faster, zero-maintenance, and fully ACID-compliant."*

### Q2: Why is the project recommendation-only? Why not assign rooms?
> **Answer:** *"Real room allocation involves administrative approvals, fee receipts, physical room inventory, and warden oversight. RoomSync is designed as a decision-support system to help students find compatible roommates and verify eligibility before applying."*

### Q3: How do you prevent SQL Injection?
> **Answer:** *"We use `PreparedStatement` with parameterized placeholders (`?`) in all database queries. User input is never concatenated directly into SQL strings."*

### Q4: How is student privacy protected?
> **Answer:** *"Other students can only see first names and last initials (e.g. 'Aarav S.'). University roll numbers, emails, exact CGPA/percentile scores, and private accommodation requests are never shown to other students."*

### Q5: What happens if a student leaves some optional form questions blank?
> **Answer:** *"In `CompatibilityCalculator`, missing factors are skipped and never counted as a conflict. The missing weight is automatically redistributed proportionally across the factors both students actually answered."*

### Q6: How do you handle concurrency between the UI and background sync?
> **Answer:** *"Network requests and HTTP servers run on background worker threads so the desktop window never freezes. When new data arrives, we update the Swing UI using `SwingUtilities.invokeLater()` on the Event Dispatch Thread (EDT)."*
