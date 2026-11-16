package com.roomsync.model;

import java.util.ArrayList;
import java.util.List;

/** A hostel and its academic cutoff. Holds no availability or vacancy data by design. */
public class Hostel {
    public int id;
    public String name;
    public String scoreType;          // PERCENTILE or CGPA
    public double cutoff;
    public String occupancyOptions;   // e.g. "DOUBLE,TRIPLE"

    public Hostel() {
    }

    public Hostel(int id, String name, String scoreType, double cutoff, String occupancyOptions) {
        this.id = id;
        this.name = name;
        this.scoreType = scoreType;
        this.cutoff = cutoff;
        this.occupancyOptions = occupancyOptions;
    }

    public List<String> occupancyList() {
        List<String> list = new ArrayList<>();
        if (occupancyOptions == null) return list;
        for (String s : occupancyOptions.split(",")) {
            if (!s.isBlank()) list.add(s.trim());
        }
        return list;
    }
}
