package com.roomsync.logic;

import com.roomsync.model.MatchResult;
import com.roomsync.model.Student;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Transparent weighted compatibility scoring plus conflict detection for one pair of students.
 * Pure logic: no database or UI code here.
 */
public class CompatibilityCalculator {

    public static final String[] FACTORS =
            {"SLEEP", "CLEANLINESS", "NOISE", "STUDY", "SCHEDULE", "ENVIRONMENT", "SOCIAL", "FOOD"};
    private static final double[] DEFAULT_WEIGHTS = {20, 15, 15, 15, 10, 10, 10, 5};

    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("SLEEP", "Sleep schedule");
        LABELS.put("CLEANLINESS", "Cleanliness");
        LABELS.put("NOISE", "Noise tolerance");
        LABELS.put("STUDY", "Study habits");
        LABELS.put("SCHEDULE", "Daily schedule");
        LABELS.put("ENVIRONMENT", "Room environment");
        LABELS.put("SOCIAL", "Guests and social");
        LABELS.put("FOOD", "Food");
    }

    /** Plural phrases used in sentences such as "study habits match closely". */
    private static final Map<String, String> HABIT_NAMES = Map.of(
            "STUDY", "study habits",
            "ENVIRONMENT", "room environment preferences",
            "SOCIAL", "guest and social habits",
            "FOOD", "food habits");

    // Ordered answer scales. Neighbours on a scale are "adjacent" (50), the ends are "opposite" (0).
    public static final List<String> STUDY_IN_ROOM = List.of("NO", "SOMETIMES", "YES");
    public static final List<String> STUDY_TIME = List.of("MORNING", "AFTERNOON", "EVENING", "NIGHT");
    public static final List<String> ENVIRONMENT = List.of("QUIET", "BALANCED", "LIVELY");
    public static final List<String> ORGANIZATION = List.of("RELAXED", "MODERATE", "VERY_ORGANIZED");
    public static final List<String> GUESTS = List.of("NEVER", "RARELY", "SOMETIMES", "OFTEN");
    public static final List<String> FOOD = List.of("VEG", "EGG", "NON_VEG");
    public static final List<String> SHARING = List.of("NO", "SOMETIMES", "YES");

    /** Times this many minutes apart (or more) score 0. */
    private static final double MAX_TIME_GAP_MINUTES = 240;

    private final Map<String, Double> weights = new LinkedHashMap<>();
    private final Map<String, Double> multipliers = new LinkedHashMap<>();
    private final double mustMinimum;
    private final int minFactors;

    public CompatibilityCalculator(Properties config) {
        for (int i = 0; i < FACTORS.length; i++) {
            weights.put(FACTORS[i], number(config, "weight." + FACTORS[i], DEFAULT_WEIGHTS[i]));
        }
        multipliers.put("MUST", number(config, "multiplier.MUST", 2.0));
        multipliers.put("PREFERABLE", number(config, "multiplier.PREFERABLE", 1.5));
        multipliers.put("NOT_IMPORTANT", number(config, "multiplier.NOT_IMPORTANT", 0.25));
        mustMinimum = number(config, "must.minimum.score", 50);
        minFactors = (int) number(config, "min.factors", 3);
    }

    public double weightOf(String factor) {
        return weights.get(factor);
    }

    public int minFactors() {
        return minFactors;
    }

    // ---------- comparison primitives ----------

    /** Minutes between two HH:mm times going the short way round the clock (23:00 vs 01:00 = 120). */
    public static int circularMinutes(String a, String b) {
        int diff = Math.abs(toMinutes(a) - toMinutes(b));
        return Math.min(diff, 24 * 60 - diff);
    }

    static int toMinutes(String hhmm) {
        String[] p = hhmm.trim().split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    static Double timeScore(String a, String b) {
        if (a == null || b == null) return null;
        return Math.max(0, 100 - circularMinutes(a, b) / MAX_TIME_GAP_MINUTES * 100);
    }

    /** 1 to 10 scale: 100 minus the normalised absolute difference. */
    static Double scaleScore(Integer a, Integer b) {
        if (a == null || b == null) return null;
        return 100 - Math.abs(a - b) / 9.0 * 100;
    }

    /** Identical 100, adjacent 50, further apart 0. Unknown answers count as missing. */
    static Double categoryScore(String a, String b, List<String> scale) {
        if (a == null || b == null) return null;
        int i = scale.indexOf(a), j = scale.indexOf(b);
        if (i < 0 || j < 0) return null;
        int d = Math.abs(i - j);
        return d == 0 ? 100.0 : d == 1 ? 50.0 : 0.0;
    }

    /** Average of the parts that have data, or null if none do. */
    static Double average(Double... parts) {
        double sum = 0;
        int n = 0;
        for (Double p : parts) {
            if (p != null) {
                sum += p;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    /** 0 to 100 score for one factor, or null when either student has no data for it. */
    public Double factorScore(String factor, Student a, Student b) {
        switch (factor) {
            case "SLEEP": return timeScore(a.sleepTime, b.sleepTime);
            case "CLEANLINESS": return scaleScore(a.cleanliness, b.cleanliness);
            case "NOISE": return scaleScore(a.noiseTolerance, b.noiseTolerance);
            case "STUDY": return average(categoryScore(a.studyInRoom, b.studyInRoom, STUDY_IN_ROOM),
                    categoryScore(a.studyTime, b.studyTime, STUDY_TIME));
            case "SCHEDULE": return timeScore(a.wakeTime, b.wakeTime);
            case "ENVIRONMENT": return average(categoryScore(a.roomEnvironment, b.roomEnvironment, ENVIRONMENT),
                    categoryScore(a.roomOrganization, b.roomOrganization, ORGANIZATION));
            case "SOCIAL": return categoryScore(a.guestFrequency, b.guestFrequency, GUESTS);
            case "FOOD": return average(categoryScore(a.foodPreference, b.foodPreference, FOOD),
                    categoryScore(a.foodSharing, b.foodSharing, SHARING));
            default: return null;
        }
    }

    /** How many factors a student answered enough of to be compared (compares them with themself). */
    public int factorsWithData(Student s) {
        int n = 0;
        for (String f : FACTORS) if (factorScore(f, s, s) != null) n++;
        return n;
    }

    // ---------- full comparison ----------

    /**
     * Scores {@code other} from the point of view of {@code me}.
     * Total = sum(weight x multiplier x score) / sum(weight x multiplier) over factors with data on both sides,
     * so the weight of a missing factor is shared out across the rest instead of counting as a conflict.
     */
    public MatchResult compare(Student me, Student other) {
        MatchResult r = new MatchResult();
        r.candidate = other;
        double weighted = 0, totalWeight = 0;
        int withData = 0;

        for (String f : FACTORS) {
            Double score = factorScore(f, me, other);
            r.factorScores.put(f, score);
            if (score == null) continue;
            withData++;
            double w = weights.get(f) * multiplier(me.importanceOf(f));
            weighted += w * score;
            totalWeight += w;
            describe(f, score, me, other, r);
        }

        for (String f : FACTORS) {
            Double score = r.factorScores.get(f);
            double share = (score == null || totalWeight == 0) ? 0
                    : weights.get(f) * multiplier(me.importanceOf(f)) / totalWeight * 100;
            r.weightShares.put(f, share);
        }

        r.insufficientData = withData < minFactors || totalWeight == 0;
        r.score = r.insufficientData ? 0 : weighted / totalWeight;
        return r;
    }

    private double multiplier(String importance) {
        return importance == null ? 1.0 : multipliers.getOrDefault(importance, 1.0);
    }

    /** Adds positive points, conflicts and must-have violations for one factor. */
    private void describe(String f, double score, Student me, Student other, MatchResult r) {
        String label = LABELS.get(f);
        String detail = detail(f, score, me, other);

        if (score >= 80) r.positives.add(label + ": " + detail + ".");
        else if (score < 70) {
            String severity = score < 30 ? "HIGH" : score < 50 ? "MEDIUM" : "LOW";
            r.conflicts.add(new MatchResult.Conflict(severity, label + ": " + detail + "."));
        }

        // Must-have rules from either side exclude the candidate when the factor scores too low.
        if (score < mustMinimum) {
            if ("MUST".equals(me.importanceOf(f))) {
                r.mustViolations.add("Your must-have on " + label.toLowerCase() + " is not met: " + detail + ".");
            }
            if ("MUST".equals(other.importanceOf(f))) {
                r.mustViolations.add("Their must-have on " + label.toLowerCase() + " is not met by your answers.");
            }
        }
    }

    /** Short plain English description. Relative only, so it never reveals the other student's exact answers. */
    private String detail(String f, double score, Student a, Student b) {
        switch (f) {
            case "SLEEP": return gap("bedtimes", circularMinutes(a.sleepTime, b.sleepTime));
            case "SCHEDULE": return gap("wake-up times", circularMinutes(a.wakeTime, b.wakeTime));
            case "CLEANLINESS": return pointsApart("cleanliness ratings", a.cleanliness, b.cleanliness);
            case "NOISE": return pointsApart("noise tolerance ratings", a.noiseTolerance, b.noiseTolerance);
            default:
                String what = HABIT_NAMES.get(f);
                if (score >= 80) return what + " match closely";
                if (score >= 50) return what + " partly match";
                return what + " are quite different";
        }
    }

    private static String gap(String what, int minutes) {
        if (minutes <= 30) return what + " are within 30 minutes";
        double hours = Math.round(minutes / 30.0) / 2.0;   // nearest half hour
        String h = hours == Math.floor(hours) ? String.valueOf((int) hours) : String.valueOf(hours);
        return what + " are about " + h + (hours == 1 ? " hour" : " hours") + " apart";
    }

    private static String pointsApart(String what, int a, int b) {
        int d = Math.abs(a - b);
        if (d == 0) return what + " are the same";
        return what + " differ by " + d + (d == 1 ? " point" : " points") + " out of 10";
    }

    private static double number(Properties p, String key, double fallback) {
        try {
            String v = p.getProperty(key);
            return v == null ? fallback : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
