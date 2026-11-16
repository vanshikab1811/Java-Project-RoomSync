package com.roomsync.ui;

import com.roomsync.logic.EligibilityService;
import com.roomsync.model.Hostel;
import com.roomsync.model.Student;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** Screen 2: the student's own details, score and the hostels they are academically eligible for. */
public class ProfilePanel extends JPanel {
    private final MainFrame frame;

    public ProfilePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
    }

    void load(Student s, List<Hostel> hostels) {
        List<Hostel> eligible = EligibilityService.eligibleHostels(s, hostels);
        String type = EligibilityService.scoreLabel(s.scoreType);
        String firstName = s.name.trim().split("\\s+")[0];

        JPanel content = Theme.column(
                Theme.title("Hi, " + firstName),
                Theme.space(4),
                Theme.body(s.program + "  ·  Year " + s.academicYear),
                Theme.space(Theme.PAD),
                detailsCard(s),
                Theme.space(Theme.GAP),
                scoreCard(s, type),
                Theme.space(Theme.GAP),
                hostelsCard(s, eligible, type),
                Theme.space(Theme.PAD));

        Theme.Button matches = new Theme.Button("See my top 3 matches", Theme.Button.PRIMARY);
        matches.addActionListener(e -> frame.showMatches());
        matches.setEnabled(!eligible.isEmpty());
        Theme.add(content, matches);
        if (eligible.isEmpty()) {
            Theme.add(content, Theme.space(Theme.SMALL_GAP));
            Theme.add(content, Theme.muted("Roommate matches need at least one eligible hostel."));
        }

        removeAll();
        add(Theme.page(content, true), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private Theme.Card detailsCard(Student s) {
        JPanel grid = new JPanel(new GridLayout(0, 2, Theme.PAD, Theme.GAP));
        grid.setOpaque(false);
        grid.add(field("Roll number", s.rollNo));
        grid.add(field("University email", s.email));
        grid.add(field("Program", s.program));
        grid.add(field("Academic year", "Year " + s.academicYear));
        grid.setAlignmentX(LEFT_ALIGNMENT);
        return Theme.card("Your details", grid);
    }

    private Theme.Card scoreCard(Student s, String type) {
        boolean verified = "VERIFIED".equals(s.verificationStatus);
        String basis = s.academicYear == 1 ? "First years are evaluated by percentile."
                : "Years 2 to 4 are evaluated by CGPA.";
        return Theme.card("Academic score",
                Theme.row(Theme.label(formatScore(s), Theme.BIG_NUMBER, Theme.TEXT),
                        Theme.body(type),
                        new Theme.Pill(verified ? "Verified" : "Self reported", verified ? Theme.ACCENT : Theme.WARM)),
                Theme.space(Theme.GAP),
                Theme.muted(basis + (verified ? " An admin has verified this score."
                        : " Self-reported scores are used as entered until an admin verifies them.")));
    }

    private Theme.Card hostelsCard(Student s, List<Hostel> eligible, String type) {
        Theme.Card card = Theme.card("Eligible hostels",
                Theme.muted("Based on your " + inSentence(type) + " only. Eligibility does not mean a room is available or assigned."),
                Theme.space(Theme.GAP));
        if (eligible.isEmpty()) {
            Theme.Card empty = new Theme.Card(Theme.CARD_ALT, false, Theme.PAD - 4);
            Theme.add(empty, Theme.label("No eligible hostels yet", Theme.BODY_BOLD, Theme.TEXT));
            Theme.add(empty, Theme.space(Theme.SMALL_GAP));
            Theme.add(empty, Theme.wrap("Your " + inSentence(type) + " of " + formatScore(s)
                    + " is below the cutoff of every hostel open to Year " + s.academicYear
                    + " students. If your score is out of date, submit the form again with the correct value.",
                    Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH));
            Theme.add(card, empty);
            return card;
        }
        for (Hostel h : eligible) {
            Theme.add(card, hostelRow(s, h, type));
            Theme.add(card, Theme.space(Theme.SMALL_GAP));
        }
        return card;
    }

    private Theme.Card hostelRow(Student s, Hostel h, String type) {
        Theme.Card row = new Theme.Card(Theme.CARD_ALT, false, Theme.GAP + 4);
        int rank = EligibilityService.preferenceRank(s, h);
        String meta = "Cutoff " + trim(h.cutoff) + " " + type + (rank > 0 ? "  ·  Your preference #" + rank : "");
        Theme.add(row, Theme.label(h.name, Theme.BODY_BOLD, Theme.TEXT));
        Theme.add(row, Theme.space(4));
        Theme.add(row, Theme.muted(meta));
        Theme.add(row, Theme.space(Theme.SMALL_GAP + 2));

        JPanel chips = Theme.row(Theme.muted("Occupancy options"));
        for (String option : h.occupancyList()) {
            boolean preferred = option.equals(s.occupancyPref);
            Theme.addToRow(chips, new Theme.Pill(EligibilityService.pretty(option), preferred ? Theme.ACCENT : Theme.SURFACE));
        }
        Theme.add(row, chips);
        Theme.add(row, Theme.space(Theme.SMALL_GAP));
        Theme.add(row, Theme.muted(EligibilityService.occupancyNote(s, h)));
        return row;
    }

    private static JPanel field(String key, String value) {
        return Theme.column(Theme.muted(key), Theme.space(2), Theme.body(value == null ? "-" : value));
    }

    private static String formatScore(Student s) {
        return "PERCENTILE".equals(s.scoreType) ? String.format("%.1f", s.score) : String.format("%.2f", s.score);
    }

    /** "Percentile" reads as "percentile" mid-sentence; "CGPA" stays as is. */
    private static String inSentence(String type) {
        return "CGPA".equals(type) ? type : type.toLowerCase();
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
    }
}
