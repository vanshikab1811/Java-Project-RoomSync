package com.roomsync.ui;

import com.roomsync.model.Student;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/** Screen 1: sign in with a roll number. */
public class LoginPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField rollField = Theme.textField();
    private final Theme.Button continueButton = new Theme.Button("Continue", Theme.Button.PRIMARY);
    private final Theme.Button registerButton = new Theme.Button("New student? Fill registration form \u2197", Theme.Button.SECONDARY);
    private final JLabel message = Theme.label(" ", Theme.SMALL, Theme.TEXT);

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());           // centres the card
        setBackground(Theme.BACKGROUND);

        Theme.Card card = Theme.card(null,
                Theme.title("Welcome to RoomSync"),
                Theme.space(Theme.SMALL_GAP),
                Theme.wrap("See the hostels you are academically eligible for and your top roommate suggestions.",
                        Theme.BODY, Theme.TEXT_MUTED, 340),
                Theme.space(Theme.PAD),
                Theme.label("Roll number", Theme.SMALL_BOLD, Theme.TEXT),
                Theme.space(Theme.SMALL_GAP),
                rollField,
                Theme.space(Theme.SMALL_GAP),
                message,
                Theme.space(Theme.GAP),
                continueButton,
                Theme.space(Theme.GAP),
                registerButton,
                Theme.space(Theme.PAD),
                Theme.muted("Demo roll numbers: 24BCE1001, 23BCE1101, 22BCE1201, 21BCE1301"));

        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> submit());
        registerButton.addActionListener(e -> openRegistrationForm());
        rollField.addActionListener(e -> submit());
        rollField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        add(card);
    }

    void reset() {
        rollField.setText("");
        message.setText(" ");
    }

    public void setRoll(String roll) {
        rollField.setText(roll);
        message.setText("Registration saved for " + roll + "! Click Continue to sign in.");
        continueButton.setEnabled(true);
    }

    private void changed() {
        continueButton.setEnabled(!rollField.getText().isBlank());
        message.setText(" ");
    }

    private void openRegistrationForm() {
        String url = frame.config.getProperty("form.url", "resources/registration_form.html").trim();
        if (url.isEmpty()) return;
        try {
            URI uri;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                uri = new URI(url);
            } else {
                // If it's the local form, route to the live embedded server so submitting connects automatically!
                int port = frame.getServerPort();
                uri = new URI("http://localhost:" + port + "/form");
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString()).start();
            }
        } catch (Exception ex) {
            // Fallback to local file if server URI fails
            try {
                Path p = Path.of(url);
                if (!p.isAbsolute()) p = Path.of("").toAbsolutePath().resolve(p);
                Desktop.getDesktop().browse(p.toUri());
            } catch (Exception ignored) {
                JOptionPane.showMessageDialog(this,
                        "Could not open browser automatically.\nPlease visit:\n" + url,
                        "RoomSync Registration", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void submit() {
        String roll = rollField.getText().trim();
        if (roll.isEmpty()) return;
        if (!roll.matches("[A-Za-z0-9]{4,20}")) {
            message.setText("Roll numbers contain only letters and digits.");
            return;
        }
        try {
            Student s = frame.studentDao.findByRoll(roll);
            if (s == null) {
                // Check if Google Sheet sync finds any new entries submitted via Google Form
                int synced = frame.syncService.syncFromSheet();
                if (synced > 0) {
                    s = frame.studentDao.findByRoll(roll);
                }
            }
            if (s == null) {
                message.setText("Roll number not found. Click the form link below to register.");
                return;
            }
            frame.signIn(s);
        } catch (Exception e) {
            frame.showError("Sign in failed.", e);
        }
    }
}
