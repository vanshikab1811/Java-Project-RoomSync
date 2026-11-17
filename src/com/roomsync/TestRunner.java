package com.roomsync;

import com.roomsync.logic.CompatibilityCalculator;
import com.roomsync.logic.CsvImporter;
import com.roomsync.logic.EligibilityService;
import com.roomsync.logic.MatchingService;
import com.roomsync.model.Hostel;
import com.roomsync.model.MatchResult;
import com.roomsync.model.Requirement;
import com.roomsync.model.Student;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Plain test runner (no JUnit). Prints PASS or FAIL per check and exits with 1 if anything failed. */
public class TestRunner {
    private static int failures = 0;
    private static final CompatibilityCalculator CALC = new CompatibilityCalculator(new Properties());
    private static final MatchingService MATCHING = new MatchingService(CALC);
    private static final List<Hostel> HOSTELS = List.of(
            new Hostel(1, "Aravali House", "PERCENTILE", 85, "DOUBLE,TRIPLE"),
            new Hostel(2, "Nilgiri Residency", "CGPA", 8.0, "SINGLE,DOUBLE"),
            new Hostel(3, "Shivalik Hall", "CGPA", 6.5, "DOUBLE,TRIPLE"));

    public static void main(String[] args) {
        circularTimeDifference();
        eligibilityNeverMixesScoreTypes();
        sameYearFiltering();
        mustHaveExclusion();
        missingDataHandling();
        topThreeRanking();
        csvValidation();
        sameGenderOnly();
        multiChoiceAnswers();
        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " test(s) failed.");
        if (failures > 0) System.exit(1);
    }

    static void circularTimeDifference() {
        check("circular time: 23:00 vs 01:00 is 120 min", CompatibilityCalculator.circularMinutes("23:00", "01:00") == 120);
        check("circular time: 01:00 vs 23:00 is 120 min", CompatibilityCalculator.circularMinutes("01:00", "23:00") == 120);
        check("circular time: 00:00 vs 12:00 is 720 min", CompatibilityCalculator.circularMinutes("00:00", "12:00") == 720);
        check("circular time: same time is 0 min", CompatibilityCalculator.circularMinutes("22:15", "22:15") == 0);
    }

    static void eligibilityNeverMixesScoreTypes() {
        Student first = student(1, "Asha Rao", 1);
        first.scoreType = "PERCENTILE";
        first.score = 90;
        List<Hostel> h = EligibilityService.eligibleHostels(first, HOSTELS);
        check("eligibility: first year judged by percentile only", h.size() == 1 && h.get(0).id == 1);

        Student wrongType = student(2, "Bina Das", 1);
        wrongType.scoreType = "CGPA";
        wrongType.score = 9.5;
        check("eligibility: CGPA for a first year is never compared",
                EligibilityService.eligibleHostels(wrongType, HOSTELS).isEmpty());
    }

    static void sameYearFiltering() {
        Student me = student(1, "Asha Rao", 2);
        Student sameYear = student(2, "Bina Das", 2);
        Student otherYear = student(3, "Chitra Iyer", 3);   // identical habits but a different year
        MatchingService.Outcome out = MATCHING.findTopMatches(me, List.of(me, sameYear, otherYear), HOSTELS);
        check("same year: only the same-year candidate is returned",
                out.matches.size() == 1 && out.matches.get(0).candidate.id == 2);
        check("same year: the student is never matched with themself",
                out.matches.stream().noneMatch(r -> r.candidate.id == me.id));
    }

    static void mustHaveExclusion() {
        Student me = student(1, "Asha Rao", 2);
        me.requirements.add(new Requirement("CLEANLINESS", "MUST"));
        Student messy = student(2, "Bina Das", 2);
        messy.cleanliness = 1;                              // me.cleanliness is 9, far apart
        Student tidy = student(3, "Chitra Iyer", 2);
        MatchingService.Outcome out = MATCHING.findTopMatches(me, List.of(me, messy, tidy), HOSTELS);
        check("must have: violating candidate is excluded from the top 3",
                out.matches.stream().noneMatch(r -> r.candidate.id == 2));
        check("must have: the reason is recorded",
                out.excluded.size() == 1 && out.excluded.get(0).mustViolations.get(0).contains("cleanliness"));
        check("must have: a compatible candidate is still shown",
                out.matches.size() == 1 && out.matches.get(0).candidate.id == 3);
    }

    static void missingDataHandling() {
        Student me = student(1, "Asha Rao", 2);
        Student partial = student(2, "Bina Das", 2);
        partial.wakeTime = null;
        partial.studyInRoom = null;
        partial.studyTime = null;
        partial.guestFrequency = null;
        MatchResult r = CALC.compare(me, partial);
        check("missing data: missing factors are skipped", r.factorScores.get("SOCIAL") == null);
        check("missing data: identical answered factors still score 100", Math.abs(r.score - 100) < 0.001);
        check("missing data: never treated as a conflict", r.conflicts.isEmpty() && r.mustViolations.isEmpty());
        double shares = r.weightShares.values().stream().mapToDouble(Double::doubleValue).sum();
        check("missing data: weights are redistributed to total 100%", Math.abs(shares - 100) < 0.001);
    }

    static void topThreeRanking() {
        Student me = student(1, "Asha Rao", 2);
        List<Student> everyone = new ArrayList<>(List.of(me));
        String[] names = {"Bina Das", "Chitra Iyer", "Devi Nair", "Esha Paul", "Farah Khan"};
        for (int i = 0; i < names.length; i++) {
            Student s = student(i + 2, names[i], 2);
            s.sleepTime = String.format("%02d:00", (22 + i) % 24);   // each one sleeps an hour later
            everyone.add(s);
        }
        List<MatchResult> top = MATCHING.findTopMatches(me, everyone, HOSTELS).matches;
        check("top 3: at most three matches", top.size() == 3);
        check("top 3: sorted by score, highest first",
                top.get(0).score >= top.get(1).score && top.get(1).score >= top.get(2).score);
        check("top 3: closest habits ranked first", top.get(0).candidate.id == 2);
        check("top 3: fewer candidates gives fewer matches",
                MatchingService.rankTop3(new ArrayList<>(List.of(CALC.compare(me, everyone.get(1))))).size() == 1);
    }

    static void csvValidation() {
        CsvImporter.Result r = new CsvImporter().parse(Path.of("resources", "sample_responses.csv"), HOSTELS, Set.of("23BCE1101"));
        String errors = String.join(" ", r.errors);
        check("csv: 5 valid rows imported from the sample", r.students.size() == 5);
        check("csv: 3 bad rows reported, not thrown", r.errors.size() == 3);
        check("csv: duplicate roll number caught", errors.contains("already exists"));
        check("csv: CGPA range and time format caught", errors.contains("CGPA must be") && errors.contains("24-hour time"));
        check("csv: missing consent caught", errors.contains("consent"));
        check("csv: H:mm time normalised to HH:mm",
                r.students.stream().anyMatch(s -> "00:10".equals(s.sleepTime)));
    }

    static void sameGenderOnly() {
        Student me = student(1, "Asha Rao", 2);
        me.gender = "FEMALE";
        Student sameGender = student(2, "Bina Das", 2);
        sameGender.gender = "FEMALE";
        Student otherGender = student(3, "Chetan Iyer", 2);   // identical habits but a different gender
        MatchingService.Outcome out = MATCHING.findTopMatches(me, List.of(me, sameGender, otherGender), HOSTELS);
        check("gender: only same-gender candidates are returned",
                out.matches.size() == 1 && out.matches.get(0).candidate.id == 2);
    }

    static void multiChoiceAnswers() {
        Student a = student(1, "Asha Rao", 2);
        Student b = student(2, "Bina Das", 2);
        a.studyTime = "MORNING,NIGHT";
        b.studyTime = "NIGHT,EVENING";   // 1 shared out of 3 different options
        Double score = CALC.factorScore("STUDY", a, b);
        check("multi choice: study time scored by overlap (1 of 3 shared)", score != null && Math.abs(score - (100 + 100.0 / 3) / 2) < 0.001);
        b.studyTime = "NIGHT,MORNING";
        check("multi choice: same study times score 100", Math.abs(CALC.factorScore("STUDY", a, b) - 100) < 0.001);
        a.occupancyPref = "DOUBLE,TRIPLE";
        check("multi choice: occupancy list is split", a.occupancyPrefs().size() == 2);
        check("multi choice: occupancy note uses any preference",
                EligibilityService.occupancyNote(a, HOSTELS.get(0)).contains("include"));
    }

    /** A year-2+ student with a full, identical set of answers that tests then tweak. */
    private static Student student(int id, String name, int year) {
        Student s = new Student();
        s.id = id;
        s.name = name;
        s.rollNo = "T" + id;
        s.gender = "MALE";
        s.academicYear = year;
        s.scoreType = EligibilityService.requiredScoreType(year);
        s.score = year == 1 ? 90 : 8.5;
        s.sleepTime = "22:00";
        s.wakeTime = "06:30";
        s.cleanliness = 9;
        s.noiseTolerance = 3;
        s.studyInRoom = "YES";
        s.studyTime = "MORNING";
        s.roomEnvironment = "QUIET";
        s.guestFrequency = "RARELY";
        s.foodPreference = "VEG";
        s.foodSharing = "YES";
        s.roomOrganization = "VERY_ORGANIZED";
        return s;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }
}
