package com.roomsync.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A student: identity, academic score and lifestyle answers in one place.
 * Fields are public to keep this mini project simple. A null lifestyle field means "not answered".
 */
public class Student {
    public int id;
    public String name;
    public String rollNo;
    public String email;
    public String program;
    public int academicYear;              // 1 to 4
    public String scoreType;              // PERCENTILE (year 1) or CGPA (years 2 to 4)
    public double score;
    public String verificationStatus = "SELF_REPORTED";

    public String sleepTime;              // HH:mm
    public String wakeTime;               // HH:mm
    public Integer cleanliness;           // 1 to 10
    public Integer noiseTolerance;        // 1 to 10
    public String studyInRoom;            // NO / SOMETIMES / YES
    public String studyTime;              // MORNING / AFTERNOON / EVENING / NIGHT
    public String roomEnvironment;        // QUIET / BALANCED / LIVELY
    public String guestFrequency;         // NEVER / RARELY / SOMETIMES / OFTEN
    public String foodPreference;         // VEG / EGG / NON_VEG
    public String foodSharing;            // NO / SOMETIMES / YES
    public String roomOrganization;       // RELAXED / MODERATE / VERY_ORGANIZED

    public String hostelPrefIds;          // hostel ids in preference order, e.g. "2,3"
    public String occupancyPref;          // SINGLE / DOUBLE / TRIPLE

    public List<Requirement> requirements = new ArrayList<>();

    /** Privacy safe name shown to other students: first name plus last initial. */
    public String displayName() {
        if (name == null || name.isBlank()) return "Student";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    /** Importance this student gave a factor, or null if they did not say. */
    public String importanceOf(String factor) {
        for (Requirement r : requirements) {
            if (r.factor.equals(factor)) return r.importanceLevel;
        }
        return null;
    }

    /** Preferred hostel ids in order. */
    public List<Integer> preferredHostelIds() {
        List<Integer> ids = new ArrayList<>();
        if (hostelPrefIds == null) return ids;
        for (String part : hostelPrefIds.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip malformed entries
            }
        }
        return ids;
    }
}
