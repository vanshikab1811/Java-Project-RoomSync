package com.roomsync.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compatibility of the current student with one candidate. */
public class MatchResult {
    public Student candidate;
    public double score;                                        // 0 to 100
    public boolean insufficientData;                            // too few shared factors to score
    public Map<String, Double> factorScores = new LinkedHashMap<>();   // null value = no data
    public Map<String, Double> weightShares = new LinkedHashMap<>();   // effective share in percent
    public List<String> positives = new ArrayList<>();
    public List<Conflict> conflicts = new ArrayList<>();
    public List<String> mustViolations = new ArrayList<>();
    public List<String> commonHostels = new ArrayList<>();

    public boolean isExcluded() {
        return insufficientData || !mustViolations.isEmpty();
    }

    /** A habit mismatch with a severity of LOW, MEDIUM or HIGH and a plain English reason. */
    public static class Conflict {
        public final String severity;
        public final String reason;

        public Conflict(String severity, String reason) {
            this.severity = severity;
            this.reason = reason;
        }
    }
}
