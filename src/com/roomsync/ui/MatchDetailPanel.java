package com.roomsync.ui;

import com.roomsync.logic.CompatibilityCalculator;
import com.roomsync.logic.EligibilityService;
import com.roomsync.model.MatchResult;
import com.roomsync.model.Student;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Screen 4: per factor breakdown, compatible habits, conflicts and the optional interest signal. */
public class MatchDetailPanel extends JPanel {
    private final MainFrame frame;
    private final CompatibilityCalculator calculator;

    public MatchDetailPanel(MainFrame frame) {
        this.frame = frame;
        this.calculator = new CompatibilityCalculator(frame.config);
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
    }

    void load(Student me, MatchResult r, boolean alreadyInterested) {
        Theme.Button back = new Theme.Button("Back to matches", Theme.Button.SECONDARY);
        back.addActionListener(e -> frame.showMatches());

        JPanel content = Theme.column(
                back,
                Theme.space(Theme.PAD),
                Theme.title(r.candidate.displayName()),
                Theme.space(4),
                Theme.body("Year " + r.candidate.academicYear + "  ·  " + Math.round(r.score) + "% compatible  ·  "
                        + "Hostels in common: " + String.join(", ", r.commonHostels)),
                Theme.space(Theme.GAP),
                new Theme.Bar(r.score, 400),
                Theme.space(Theme.PAD),
                breakdownCard(me, r),
                Theme.space(Theme.GAP),
                habitsCard(r),
                Theme.space(Theme.GAP),
                conflictsCard(r),
                Theme.space(Theme.GAP),
                interestCard(r, alreadyInterested));

        removeAll();
        add(Theme.page(content, true), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private Theme.Card breakdownCard(Student me, MatchResult r) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, Theme.PAD);
        c.anchor = GridBagConstraints.WEST;

        int y = 0;
        for (String f : CompatibilityCalculator.FACTORS) {
            Double score = r.factorScores.get(f);
            String importance = me.importanceOf(f);
            c.gridy = y++;

            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            grid.add(Theme.label(CompatibilityCalculator.LABELS.get(f), Theme.BODY_BOLD, Theme.TEXT), c);

            c.gridx = 1;
            String weight = "Weight " + Math.round(calculator.weightOf(f)) + "%";
            if (score != null) weight += "  (counts " + Math.round(r.weightShares.get(f)) + "%)";
            grid.add(Theme.muted(weight), c);

            c.gridx = 2;
            JComponent tag = importance == null ? Theme.muted("")
                    : new Theme.Pill(EligibilityService.pretty(importance), Theme.SURFACE);
            grid.add(tag, c);

            c.gridx = 3;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(score == null ? Theme.muted("Not enough data, so its weight is shared across the others")
                    : new Theme.Bar(score, 160), c);

            c.gridx = 4;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(6, 0, 6, 0);
            grid.add(Theme.label(score == null ? "-" : String.valueOf(Math.round(score)), Theme.BODY_BOLD, Theme.TEXT), c);
            c.insets = new Insets(6, 0, 6, Theme.PAD);
        }
        return Theme.card("Score breakdown",
                Theme.wrap("Each factor scores 0 to 100. The weight comes from the configuration; your importance "
                        + "settings raise or lower it, and \"counts\" shows its final share of the total.",
                        Theme.SMALL, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH),
                Theme.space(Theme.GAP),
                grid);
    }

    private Theme.Card habitsCard(MatchResult r) {
        Theme.Card card = Theme.card("Compatible habits");
        if (r.positives.isEmpty()) Theme.add(card, Theme.muted("No strongly matching habits found."));
        for (String p : r.positives) {
            Theme.add(card, Theme.wrap("•  " + p, Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH));
            Theme.add(card, Theme.space(4));
        }
        return card;
    }

    private Theme.Card conflictsCard(MatchResult r) {
        Theme.Card card = Theme.card("Possible conflicts");
        if (r.conflicts.isEmpty()) Theme.add(card, Theme.muted("No notable conflicts found."));
        for (MatchResult.Conflict conflict : r.conflicts) {
            Theme.Pill badge = new Theme.Pill(EligibilityService.pretty(conflict.severity),
                    Theme.severityColor(conflict.severity));
            Theme.add(card, Theme.row(badge, Theme.wrap(conflict.reason, Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH - 80)));
            Theme.add(card, Theme.space(Theme.SMALL_GAP));
        }
        return card;
    }

    private Theme.Card interestCard(MatchResult r, boolean alreadyInterested) {
        Theme.Button button = new Theme.Button(alreadyInterested ? "Interest noted" : "Express interest", Theme.Button.PRIMARY);
        button.setEnabled(!alreadyInterested);
        button.addActionListener(e -> {
            try {
                frame.recordInterest(r);
                button.setText("Interest noted");
                button.setEnabled(false);
            } catch (Exception ex) {
                frame.showError("Your interest could not be saved.", ex);
            }
        });
        String name = r.candidate.displayName();
        return Theme.card("Express interest (optional)",
                Theme.wrap("Let RoomSync note that you would like to room with " + name + (name.endsWith(".") ? "" : "."),
                        Theme.BODY, Theme.TEXT, Theme.WIDE_TEXT_WIDTH),
                Theme.space(Theme.SMALL_GAP),
                Theme.wrap("This is only a preference signal. It does not reserve, request, approve or allocate a hostel or room.",
                        Theme.SMALL, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH),
                Theme.space(Theme.GAP),
                button);
    }
}
