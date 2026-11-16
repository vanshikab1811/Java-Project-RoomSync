package com.roomsync.ui;

import com.roomsync.db.Database;
import com.roomsync.logic.CsvImporter;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.Arrays;

/** Screen 5: password protected admin tools (import CSV, verify a score, reset demo data). */
public class AdminPanel extends JPanel {
    private static final Path SAMPLE_CSV = Path.of("resources", "sample_responses.csv");

    private final MainFrame frame;
    private final JPasswordField passwordField = Theme.passwordField();
    private final JLabel passwordMessage = Theme.label(" ", Theme.SMALL, Theme.TEXT);
    private final JTextField rollField = Theme.textField();
    private final JTextArea log = new JTextArea(8, 40);
    private final Theme.Button unlockButton = new Theme.Button("Unlock", Theme.Button.PRIMARY);
    private final Theme.Button verifyButton = new Theme.Button("Mark as verified", Theme.Button.PRIMARY);
    private boolean unlocked;

    public AdminPanel(MainFrame frame) {
        this.frame = frame;
        unlockButton.setEnabled(false);
        verifyButton.setEnabled(false);
        enableWhenFilled(passwordField, unlockButton);
        enableWhenFilled(rollField, verifyButton);
        unlockButton.addActionListener(e -> tryUnlock());
        passwordField.addActionListener(e -> tryUnlock());
        verifyButton.addActionListener(e -> verify());
        rollField.addActionListener(e -> { if (!rollField.getText().isBlank()) verify(); });
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(Theme.BODY);
        log.setForeground(Theme.TEXT);
        log.setBackground(Theme.CARD_ALT);
        log.setBorder(new javax.swing.border.EmptyBorder(Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP));
        log.setAlignmentX(LEFT_ALIGNMENT);
        log.setText("Results of admin actions appear here.");
    }

    void onShow() {
        removeAll();
        add(Theme.page(unlocked ? toolsView() : lockedView(), false), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel lockedView() {
        passwordField.setText("");
        passwordMessage.setText(" ");
        return Theme.column(
                Theme.title("Admin"),
                Theme.space(4),
                Theme.body("Enter the admin password to manage demo data."),
                Theme.space(Theme.PAD),
                Theme.card(null,
                        Theme.label("Password", Theme.SMALL_BOLD, Theme.TEXT),
                        Theme.space(Theme.SMALL_GAP),
                        passwordField,
                        Theme.space(Theme.SMALL_GAP),
                        passwordMessage,
                        Theme.space(Theme.GAP),
                        unlockButton));
    }

    private void tryUnlock() {
        char[] typed = passwordField.getPassword();
        char[] expected = frame.config.getProperty("admin.password", "admin123").toCharArray();
        boolean ok = Arrays.equals(typed, expected);
        Arrays.fill(typed, ' ');
        if (!ok) {
            passwordMessage.setText("That password is not correct.");
            return;
        }
        unlocked = true;
        onShow();
    }

    private JPanel toolsView() {
        Theme.Button importSample = new Theme.Button("Import sample CSV", Theme.Button.PRIMARY);
        Theme.Button importFile = new Theme.Button("Choose CSV file", Theme.Button.SECONDARY);
        Theme.Button syncSheet = new Theme.Button("Sync Google Sheet", Theme.Button.SECONDARY);
        importSample.addActionListener(e -> importCsv(SAMPLE_CSV));
        importFile.addActionListener(e -> chooseCsv());
        syncSheet.addActionListener(e -> syncSheet());

        Theme.Button reset = new Theme.Button("Reset demo data", Theme.Button.SECONDARY);
        reset.addActionListener(e -> reset());

        Theme.Button lock = new Theme.Button("Lock admin", Theme.Button.SECONDARY);
        lock.addActionListener(e -> {
            unlocked = false;
            onShow();
        });

        return Theme.column(
                Theme.title("Admin"),
                Theme.space(4),
                Theme.body("Manage demo data. These tools never allocate hostels or rooms."),
                Theme.space(Theme.PAD),
                Theme.card("Import form responses",
                        Theme.wrap("Import a Google Form export saved as CSV. Rows with problems are skipped and listed "
                                + "in the results below.", Theme.BODY, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH),
                        Theme.space(Theme.GAP),
                        Theme.row(importSample, importFile, syncSheet)),
                Theme.space(Theme.GAP),
                Theme.card("Verify a score",
                        Theme.wrap("Marks a student's self-reported score as verified after checking their records.",
                                Theme.BODY, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH),
                        Theme.space(Theme.GAP),
                        Theme.label("Roll number", Theme.SMALL_BOLD, Theme.TEXT),
                        Theme.space(Theme.SMALL_GAP),
                        rollField,
                        Theme.space(Theme.GAP),
                        verifyButton),
                Theme.space(Theme.GAP),
                Theme.card("Reset demo data",
                        Theme.wrap("Deletes every student, imported response and interest signal, then reloads the "
                                + "original demo data.", Theme.BODY, Theme.TEXT_MUTED, Theme.WIDE_TEXT_WIDTH),
                        Theme.space(Theme.GAP),
                        reset),
                Theme.space(Theme.GAP),
                Theme.card("Results", log),
                Theme.space(Theme.PAD),
                lock);
    }

    private void chooseCsv() {
        JFileChooser chooser = new JFileChooser(Path.of("resources").toAbsolutePath().toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) importCsv(chooser.getSelectedFile().toPath());
    }

    private void importCsv(Path file) {
        try {
            CsvImporter.Result result = new CsvImporter().parse(file, frame.hostelDao.findAll(),
                    frame.studentDao.allRollNumbers());
            frame.studentDao.insertAll(result.students);
            StringBuilder text = new StringBuilder("Imported " + result.students.size()
                    + (result.students.size() == 1 ? " student" : " students") + " from " + file.getFileName() + ".");
            if (!result.errors.isEmpty()) {
                text.append("\n").append(result.errors.size()).append(" row(s) skipped:");
                for (String err : result.errors) text.append("\n  ").append(err);
            }
            log.setText(text.toString());
            frame.dataChanged();
        } catch (Exception e) {
            frame.showError("The CSV could not be imported.", e);
        }
    }

    private void syncSheet() {
        int count = frame.syncService.syncFromSheet();
        if (count > 0) {
            log.setText("Sync successful! Imported " + count + " new student(s) from linked Google Sheet.");
            frame.dataChanged();
        } else {
            String url = frame.config.getProperty("sheet.sync.url", "").trim();
            if (url.isEmpty()) {
                log.setText("No Google Sheet sync URL configured.\nSet sheet.sync.url in resources/config.properties to auto-sync with Google Forms.");
            } else {
                log.setText("Checked Google Sheet: no new student entries found.");
            }
        }
    }

    private void verify() {
        String roll = rollField.getText().trim();
        try {
            if (frame.studentDao.markVerified(roll)) {
                log.setText("The score for " + roll.toUpperCase() + " is now marked as verified.");
                rollField.setText("");
                frame.dataChanged();
            } else {
                log.setText("No student has the roll number " + roll + ".");
            }
        } catch (Exception e) {
            frame.showError("The score could not be verified.", e);
        }
    }

    private void reset() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Reset all demo data? Imported students and interest signals will be removed.",
                "Reset demo data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            Database.resetDemoData();
            log.setText("Demo data has been reset to the original 21 students and 3 hostels.");
            frame.dataChanged();
        } catch (Exception e) {
            frame.showError("Demo data could not be reset.", e);
        }
    }

    /** Keeps a button disabled until its field has text. */
    private static void enableWhenFilled(JTextComponent field, Theme.Button button) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() { button.setEnabled(field.getDocument().getLength() > 0); }
        });
    }
}
