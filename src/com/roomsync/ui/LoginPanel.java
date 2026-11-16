package com.roomsync.ui;

import com.roomsync.model.Student;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;

/** Screen 1: sign in with a roll number. */
public class LoginPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField rollField = Theme.textField();
    private final Theme.Button continueButton = new Theme.Button("Continue", Theme.Button.PRIMARY);
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
                Theme.space(Theme.PAD),
                Theme.muted("Demo roll numbers: 24BCE1001, 23BCE1101, 22BCE1201, 21BCE1301"));

        continueButton.setEnabled(false);
        continueButton.addActionListener(e -> submit());
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

    private void changed() {
        continueButton.setEnabled(!rollField.getText().isBlank());
        message.setText(" ");
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
                message.setText("We couldn't find that roll number. Check it and try again.");
                return;
            }
            frame.signIn(s);
        } catch (Exception e) {
            frame.showError("Sign in failed.", e);
        }
    }
}
