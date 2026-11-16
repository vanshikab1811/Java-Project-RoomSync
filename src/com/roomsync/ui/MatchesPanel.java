package com.roomsync.ui;

import com.roomsync.logic.CompatibilityCalculator;
import com.roomsync.logic.MatchingService;
import com.roomsync.model.MatchResult;
import com.roomsync.model.Student;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Screen 3: up to three match cards. Shows only privacy safe details about other students. */
public class MatchesPanel extends JPanel {
    private final MainFrame frame;

    public MatchesPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
    }

    void load(Student me, MatchingService.Outcome outcome) {
        JPanel content = Theme.column(
                Theme.title("Your top matches"),
                Theme.space(4),
                Theme.body("Students in Year " + me.academicYear
                        + " who share at least one eligible hostel with you, ranked by compatibility."),
                Theme.space(Theme.PAD));

        if (outcome.matches.isEmpty()) {
            Theme.add(content, emptyState(outcome.message));
        } else {
            JPanel grid = new JPanel(new GridLayout(1, 3, Theme.GAP, 0));
            grid.setOpaque(false);
            grid.setAlignmentX(LEFT_ALIGNMENT);
            for (int i = 0; i < 3; i++) {
                if (i < outcome.matches.size()) grid.add(matchCard(i + 1, outcome.matches.get(i)));
                else grid.add(placeholder());
            }
            Theme.add(content, grid);
            if (outcome.matches.size() < 3) {
                Theme.add(content, Theme.space(Theme.GAP));
                int n = outcome.matches.size();
                Theme.add(content, Theme.muted("Only " + n + (n == 1 ? " student" : " students")
                        + " met every rule: same year, a shared eligible hostel and no must-have conflicts."));
            }
        }
        if (!outcome.excluded.isEmpty()) {
            Theme.add(content, Theme.space(Theme.PAD));
            Theme.add(content, excludedCard(outcome.excluded));
        }

        removeAll();
        add(Theme.page(content, true), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private Theme.Card matchCard(int rank, MatchResult r) {
        Theme.Button details = new Theme.Button("View details", Theme.Button.PRIMARY);
        details.addActionListener(e -> frame.showDetail(r));
        return Theme.card(null,
                Theme.muted("Match " + rank),
                Theme.space(4),
                Theme.heading(r.candidate.displayName()),
                Theme.space(2),
                Theme.muted("Year " + r.candidate.academicYear),
                Theme.space(Theme.GAP),
                Theme.row(Theme.label(Math.round(r.score) + "%", Theme.BIG_NUMBER, Theme.TEXT),
                        Theme.muted(" compatible")),
                Theme.space(Theme.SMALL_GAP),
                new Theme.Bar(r.score, Theme.MATCH_TEXT_WIDTH),
                Theme.space(Theme.PAD - 4),
                Theme.label("Hostels in common", Theme.SMALL_BOLD, Theme.TEXT_MUTED),
                Theme.space(2),
                Theme.wrap(String.join(", ", r.commonHostels), Theme.BODY, Theme.TEXT, Theme.MATCH_TEXT_WIDTH),
                Theme.space(Theme.GAP),
                Theme.label("Shared traits", Theme.SMALL_BOLD, Theme.TEXT_MUTED),
                Theme.space(2),
                Theme.wrap(sharedTraits(r), Theme.BODY, Theme.TEXT, Theme.MATCH_TEXT_WIDTH),
                Theme.space(Theme.PAD - 4),
                details);
    }

    /** Short line naming the strongest matching factors. */
    static String sharedTraits(MatchResult r) {
        List<String> traits = new ArrayList<>();
        for (Map.Entry<String, Double> e : r.factorScores.entrySet()) {
            if (e.getValue() != null && e.getValue() >= 80 && traits.size() < 3) {
                traits.add(CompatibilityCalculator.LABELS.get(e.getKey()).toLowerCase());
            }
        }
        if (traits.isEmpty()) return "No especially strong overlaps. Open the details to compare habits.";
        String line = String.join(", ", traits);
        return "Similar " + line + ".";
    }

    private Theme.Card emptyState(String message) {
        return Theme.card("No matches to show",
                Theme.wrap(message == null ? "No valid candidates were found." : message,
                        Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH),
                Theme.space(Theme.GAP),
                Theme.wrap("Matches only include students in your year who share an eligible hostel with you, "
                        + "have answered enough lifestyle questions, and do not clash on a must-have.",
                        Theme.SMALL, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH));
    }

    private Theme.Card excludedCard(List<MatchResult> excluded) {
        Theme.Card card = Theme.card("Left out because of must-have requirements",
                Theme.muted(excluded.size() + (excluded.size() == 1 ? " student was" : " students were")
                        + " not shown. Reasons:"),
                Theme.space(Theme.SMALL_GAP));
        Set<String> reasons = new LinkedHashSet<>();
        for (MatchResult r : excluded) reasons.addAll(r.mustViolations);
        for (String reason : reasons) {
            Theme.add(card, Theme.wrap("•  " + reason, Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH));
            Theme.add(card, Theme.space(4));
        }
        return card;
    }

    private JPanel placeholder() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }
}
