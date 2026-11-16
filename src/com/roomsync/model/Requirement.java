package com.roomsync.model;

/** How much a student cares about one compatibility factor (MUST, PREFERABLE, NOT_IMPORTANT). */
public class Requirement {
    public String factor;           // one of CompatibilityCalculator.FACTORS
    public String importanceLevel;  // MUST / PREFERABLE / NOT_IMPORTANT

    public Requirement(String factor, String importanceLevel) {
        this.factor = factor;
        this.importanceLevel = importanceLevel;
    }
}
