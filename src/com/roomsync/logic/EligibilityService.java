package com.roomsync.logic;

import com.roomsync.model.Hostel;
import com.roomsync.model.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Academic eligibility only. First years are judged by percentile, years 2 to 4 by CGPA, never mixed.
 * Eligibility says nothing about availability: no hostel or bed is ever claimed to be free or assigned.
 */
public class EligibilityService {

    public static String requiredScoreType(int academicYear) {
        return academicYear == 1 ? "PERCENTILE" : "CGPA";
    }

    /** Hostels whose cutoff the student meets, the student's preferred hostels first. */
    public static List<Hostel> eligibleHostels(Student s, List<Hostel> hostels) {
        List<Hostel> result = new ArrayList<>();
        String type = requiredScoreType(s.academicYear);
        // A score of the wrong type (e.g. a CGPA for a first year) is never compared.
        if (!type.equals(s.scoreType)) return result;
        for (Hostel h : hostels) {
            if (type.equals(h.scoreType) && s.score >= h.cutoff) result.add(h);
        }
        List<Integer> prefs = s.preferredHostelIds();
        result.sort(Comparator.comparingInt(h -> prefs.contains(h.id) ? prefs.indexOf(h.id) : Integer.MAX_VALUE));
        return result;
    }

    /** 1-based preference rank of a hostel for this student, or 0 if not listed. */
    public static int preferenceRank(Student s, Hostel h) {
        return s.preferredHostelIds().indexOf(h.id) + 1;
    }

    /** Plain note about whether the student's occupancy preference is one of the hostel's room types. */
    public static String occupancyNote(Student s, Hostel h) {
        if (s.occupancyPref == null || s.occupancyPref.isBlank()) return "No occupancy preference given.";
        String pref = pretty(s.occupancyPref);
        return h.occupancyList().contains(s.occupancyPref)
                ? "Your preference (" + pref + ") is one of this hostel's room types."
                : "Your preference (" + pref + ") is not one of this hostel's room types.";
    }

    public static String scoreLabel(String scoreType) {
        return "PERCENTILE".equals(scoreType) ? "Percentile" : "CGPA";
    }

    /** "VERY_ORGANIZED" becomes "Very organized". */
    public static String pretty(String code) {
        if (code == null) return "";
        String s = code.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
