package com.roomsync.logic;

import com.roomsync.model.Hostel;
import com.roomsync.model.Requirement;
import com.roomsync.model.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a Google Form CSV export into students. Invalid rows are reported, never thrown.
 * Parsing only: the caller saves the valid students, so this class has no database code.
 */
public class CsvImporter {

    /** Valid students plus one message per rejected row. */
    public static class Result {
        public List<Student> students = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    // Column headers exactly as in sample_responses.csv (matched case-insensitively).
    static final String NAME = "Full Name", ROLL = "Roll Number", EMAIL = "University Email", PROGRAM = "Program",
            YEAR = "Academic Year", SCORE = "Score", HOSTELS = "Hostel Preferences", OCCUPANCY = "Occupancy Preference",
            SLEEP = "Sleep Time", WAKE = "Wake Time", CLEAN = "Cleanliness", NOISE = "Noise Tolerance",
            STUDY_ROOM = "Study In Room", STUDY_TIME = "Study Time", ENV = "Room Environment", GUESTS = "Guest Frequency",
            FOOD = "Food Preference", SHARING = "Food Sharing", ORG = "Room Organization", CONSENT = "Consent",
            GENDER = "Gender";
    static final String[] REQUIRED_COLUMNS = {NAME, ROLL, EMAIL, PROGRAM, GENDER, YEAR, SCORE, CONSENT};
    static final List<String> OCCUPANCY_TYPES = List.of("SINGLE", "DOUBLE", "TRIPLE");

    public Result parse(Path file, List<Hostel> hostels, Set<String> existingRolls) {
        try {
            return parseLines(Files.readAllLines(file, StandardCharsets.UTF_8), hostels, existingRolls);
        } catch (IOException e) {
            Result result = new Result();
            result.errors.add("Could not read the file: " + file.getFileName());
            return result;
        }
    }

    public Result parseString(String csvContent, List<Hostel> hostels, Set<String> existingRolls) {
        if (csvContent == null || csvContent.isBlank()) {
            Result r = new Result();
            r.errors.add("The content is empty.");
            return r;
        }
        return parseLines(java.util.Arrays.asList(csvContent.split("\\r?\\n")), hostels, existingRolls);
    }

    public Result parseLines(List<String> lines, List<Hostel> hostels, Set<String> existingRolls) {
        Result result = new Result();
        if (lines.isEmpty()) {
            result.errors.add("The file is empty.");
            return result;
        }

        Map<String, Integer> columns = new HashMap<>();
        List<String> header = splitCsvLine(lines.get(0).replace("﻿", ""));   // drop a UTF-8 BOM
        for (int i = 0; i < header.size(); i++) columns.put(header.get(i).trim().toLowerCase(), i);
        for (String col : REQUIRED_COLUMNS) {
            if (!columns.containsKey(col.toLowerCase())) result.errors.add("Missing column: \"" + col + "\".");
        }
        if (!result.errors.isEmpty()) return result;

        Set<String> seenRolls = new HashSet<>(existingRolls);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            Row row = new Row(splitCsvLine(lines.get(i)), columns);
            List<String> problems = new ArrayList<>();
            Student s = readStudent(row, hostels, problems);
            if (s != null && !seenRolls.add(s.rollNo.toUpperCase())) {
                problems.add("roll number " + s.rollNo + " already exists");
            }
            if (problems.isEmpty()) result.students.add(s);
            else result.errors.add("Row " + (i + 1) + ": " + String.join("; ", problems) + ".");
        }
        return result;
    }

    private Student readStudent(Row row, List<Hostel> hostels, List<String> problems) {
        Student s = new Student();
        s.name = required(row, NAME, problems);
        s.rollNo = required(row, ROLL, problems);
        s.email = required(row, EMAIL, problems);
        s.program = required(row, PROGRAM, problems);
        String genderText = required(row, GENDER, problems);
        if (genderText != null) {
            String g = genderText.trim().toUpperCase();
            if (g.equals("MALE") || g.equals("FEMALE")) s.gender = g;
            else problems.add("gender must be Male or Female");
        }
        if (s.email != null && !s.email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) problems.add("email is not valid");

        String consent = row.get(CONSENT);
        if (consent == null || !(consent.toLowerCase().startsWith("yes") || consent.toLowerCase().startsWith("i agree"))) {
            problems.add("consent was not given");
        }

        String yearText = required(row, YEAR, problems);
        if (yearText != null) {
            String digits = yearText.replaceAll("\\D", "");     // "2nd Year" -> 2
            if (digits.length() == 1 && digits.charAt(0) >= '1' && digits.charAt(0) <= '4') {
                s.academicYear = digits.charAt(0) - '0';
            } else {
                problems.add("academic year must be 1 to 4");
            }
        }
        String scoreText = required(row, SCORE, problems);
        if (scoreText != null && s.academicYear > 0) {
            s.scoreType = EligibilityService.requiredScoreType(s.academicYear);
            double max = s.academicYear == 1 ? 100 : 10;
            try {
                s.score = Double.parseDouble(scoreText);
                if (s.score < 0 || s.score > max) {
                    problems.add(s.academicYear == 1 ? "percentile must be between 0 and 100" : "CGPA must be between 0 and 10");
                }
            } catch (NumberFormatException e) {
                problems.add("score is not a number");
            }
        }

        s.sleepTime = time(row, SLEEP, problems);
        s.wakeTime = time(row, WAKE, problems);
        s.cleanliness = scale(row, CLEAN, problems);
        s.noiseTolerance = scale(row, NOISE, problems);
        s.studyInRoom = choice(row, STUDY_ROOM, CompatibilityCalculator.STUDY_IN_ROOM, problems);
        s.studyTime = multiChoice(row, STUDY_TIME, CompatibilityCalculator.STUDY_TIME, problems);
        s.roomEnvironment = choice(row, ENV, CompatibilityCalculator.ENVIRONMENT, problems);
        s.guestFrequency = choice(row, GUESTS, CompatibilityCalculator.GUESTS, problems);
        s.foodPreference = choice(row, FOOD, CompatibilityCalculator.FOOD, problems);
        s.foodSharing = choice(row, SHARING, CompatibilityCalculator.SHARING, problems);
        s.roomOrganization = choice(row, ORG, CompatibilityCalculator.ORGANIZATION, problems);
        s.occupancyPref = multiChoice(row, OCCUPANCY, OCCUPANCY_TYPES, problems);
        s.hostelPrefIds = hostelIds(row.get(HOSTELS), hostels, problems);

        // One "Importance <Factor>" column per factor, e.g. "Importance Sleep Schedule".
        for (String factor : CompatibilityCalculator.FACTORS) {
            String label = CompatibilityCalculator.LABELS.get(factor);
            String level = importance(row.get("Importance " + label));
            if (level == null) level = importance(row.get("Importance [" + label + "]"));
            if (level != null) s.requirements.add(new Requirement(factor, level));
        }
        return s;
    }

    // ---------- field helpers ----------

    private static String required(Row row, String col, List<String> problems) {
        String v = row.get(col);
        if (v == null) problems.add(col.toLowerCase() + " is required");
        return v;
    }

    /** Optional HH:mm time (H:mm accepted and padded). */
    private static String time(Row row, String col, List<String> problems) {
        String v = row.get(col);
        if (v == null) return null;
        if (!v.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
            problems.add(col.toLowerCase() + " must be a 24-hour time like 23:30");
            return null;
        }
        return v.length() == 4 ? "0" + v : v;
    }

    private static Integer scale(Row row, String col, List<String> problems) {
        String v = row.get(col);
        if (v == null) return null;
        try {
            int n = Integer.parseInt(v);
            if (n >= 1 && n <= 10) return n;
        } catch (NumberFormatException ignored) {
            // reported below
        }
        problems.add(col.toLowerCase() + " must be a whole number from 1 to 10");
        return null;
    }

    /** "Very organized" -> VERY_ORGANIZED, "Non-veg" -> NON_VEG. */
    private static String choice(Row row, String col, List<String> allowed, List<String> problems) {
        String v = row.get(col);
        if (v == null) return null;
        String code = v.trim().toUpperCase().replaceAll("[\\s-]+", "_");
        if (allowed.contains(code)) return code;
        problems.add(col.toLowerCase() + " \"" + v + "\" is not one of the form options");
        return null;
    }

    /** Several options in one cell ("Morning, Night" or "Morning; Night") become "MORNING,NIGHT" in a fixed order. */
    private static String multiChoice(Row row, String col, List<String> allowed, List<String> problems) {
        String v = row.get(col);
        if (v == null) return null;
        java.util.Set<String> picked = new java.util.HashSet<>();
        for (String part : v.split("[;,]")) {
            if (part.isBlank()) continue;
            String code = part.trim().toUpperCase().replaceAll("[\\s-]+", "_");
            if (allowed.contains(code)) picked.add(code);
            else problems.add(col.toLowerCase() + " \"" + part.trim() + "\" is not one of the form options");
        }
        if (picked.isEmpty()) return null;
        List<String> ordered = new ArrayList<>();
        for (String a : allowed) if (picked.contains(a)) ordered.add(a);
        return String.join(",", ordered);
    }

    private static String importance(String v) {
        if (v == null) return null;
        String t = v.toLowerCase();
        if (t.startsWith("must")) return "MUST";
        if (t.startsWith("pref")) return "PREFERABLE";
        if (t.startsWith("not")) return "NOT_IMPORTANT";
        return null;
    }

    /** Hostel names separated by ';' or ',' become ids in the same order. */
    private static String hostelIds(String v, List<Hostel> hostels, List<String> problems) {
        if (v == null) return null;
        List<String> ids = new ArrayList<>();
        for (String name : v.split("[;,]")) {
            if (name.isBlank()) continue;
            Hostel match = null;
            for (Hostel h : hostels) if (h.name.equalsIgnoreCase(name.trim())) match = h;
            if (match == null) problems.add("unknown hostel \"" + name.trim() + "\"");
            else ids.add(String.valueOf(match.id));
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    /** Splits one CSV line, honouring quotes and doubled quotes ("") as Google Sheets writes them. */
    static List<String> splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** One data row with lookup by header name. Blank cells read as null. */
    private static class Row {
        final List<String> cells;
        final Map<String, Integer> columns;

        Row(List<String> cells, Map<String, Integer> columns) {
            this.cells = cells;
            this.columns = columns;
        }

        String get(String col) {
            Integer i = columns.get(col.toLowerCase());
            if (i == null || i >= cells.size()) return null;
            String v = cells.get(i).trim();
            return v.isEmpty() ? null : v;
        }
    }
}
