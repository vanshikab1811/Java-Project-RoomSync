package com.roomsync.logic;

import com.roomsync.model.Hostel;
import com.roomsync.model.MatchResult;
import com.roomsync.model.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Applies the hard rules, scores every remaining candidate and keeps the top 3. Pure logic. */
public class MatchingService {

    /** The top matches plus, when there are none, a message explaining why. */
    public static class Outcome {
        public List<MatchResult> matches = new ArrayList<>();
        public List<MatchResult> excluded = new ArrayList<>();   // must-have violations, with reasons
        public String message;
    }

    private final CompatibilityCalculator calculator;

    public MatchingService(CompatibilityCalculator calculator) {
        this.calculator = calculator;
    }

    public Outcome findTopMatches(Student me, List<Student> everyone, List<Hostel> hostels) {
        Outcome out = new Outcome();
        List<Hostel> myHostels = EligibilityService.eligibleHostels(me, hostels);
        if (myHostels.isEmpty()) {
            out.message = "You are not academically eligible for any hostel yet, so there is no one to match you with.";
            return out;
        }
        if (calculator.factorsWithData(me) < calculator.minFactors()) {
            out.message = "There is not enough lifestyle information in your profile to compute compatibility.";
            return out;
        }

        List<MatchResult> valid = new ArrayList<>();
        for (Student other : everyone) {
            // Hard rules: a different student, in the same year, sharing at least one eligible hostel.
            if (other.id == me.id || other.rollNo != null && other.rollNo.equalsIgnoreCase(me.rollNo)) continue;
            if (other.academicYear != me.academicYear) continue;
            List<String> common = commonHostels(myHostels, EligibilityService.eligibleHostels(other, hostels));
            if (common.isEmpty()) continue;

            MatchResult r = calculator.compare(me, other);
            r.commonHostels = common;
            if (r.insufficientData) continue;
            if (!r.mustViolations.isEmpty()) {
                out.excluded.add(r);
                continue;
            }
            valid.add(r);
        }

        out.matches = rankTop3(valid);
        if (out.matches.isEmpty()) {
            out.message = "No compatible students in your year share an eligible hostel with you right now.";
        }
        return out;
    }

    /** Highest score first, name as a tie-breaker, at most 3. */
    public static List<MatchResult> rankTop3(List<MatchResult> results) {
        List<MatchResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingDouble((MatchResult r) -> r.score).reversed()
                .thenComparing(r -> r.candidate.name == null ? "" : r.candidate.name));
        return new ArrayList<>(sorted.subList(0, Math.min(3, sorted.size())));
    }

    private static List<String> commonHostels(List<Hostel> mine, List<Hostel> theirs) {
        List<String> names = new ArrayList<>();
        for (Hostel h : mine) {
            for (Hostel t : theirs) {
                if (h.id == t.id) names.add(h.name);
            }
        }
        return names;
    }
}
