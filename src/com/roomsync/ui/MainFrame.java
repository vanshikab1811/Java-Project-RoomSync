package com.roomsync.ui;

import com.roomsync.db.HostelDao;
import com.roomsync.db.StudentDao;
import com.roomsync.logic.CompatibilityCalculator;
import com.roomsync.logic.MatchingService;
import com.roomsync.logic.RegistrationServer;
import com.roomsync.logic.SyncService;
import com.roomsync.model.MatchResult;
import com.roomsync.model.Student;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

/** The window: a slim top bar for navigation and a CardLayout holding the five screens. */
public class MainFrame extends JFrame {
    final Properties config;
    final StudentDao studentDao = new StudentDao();
    final HostelDao hostelDao = new HostelDao();
    final MatchingService matching;
    public final SyncService syncService;
    private final RegistrationServer server;

    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final Theme.Button navProfile = new Theme.Button("Profile", Theme.Button.NAV);
    private final Theme.Button navMatches = new Theme.Button("Matches", Theme.Button.NAV);
    private final Theme.Button navAdmin = new Theme.Button("Admin", Theme.Button.NAV);
    private final Theme.Button navSession = new Theme.Button("Log out", Theme.Button.NAV);

    private final LoginPanel loginPanel;
    private final ProfilePanel profilePanel;
    private final MatchesPanel matchesPanel;
    private final MatchDetailPanel detailPanel;
    private final AdminPanel adminPanel;

    private Student current;

    public MainFrame(Properties config) {
        super("RoomSync");
        this.config = config;
        this.matching = new MatchingService(new CompatibilityCalculator(config));
        this.syncService = new SyncService(config, studentDao, hostelDao);

        int port = 8080;
        try {
            port = Integer.parseInt(config.getProperty("server.port", "8080"));
        } catch (NumberFormatException ignored) {}
        this.server = new RegistrationServer(this, studentDao, hostelDao);
        this.server.start(port);

        loginPanel = new LoginPanel(this);
        profilePanel = new ProfilePanel(this);
        matchesPanel = new MatchesPanel(this);
        detailPanel = new MatchDetailPanel(this);
        adminPanel = new AdminPanel(this);
        deck.add(loginPanel, "login");
        deck.add(profilePanel, "profile");
        deck.add(matchesPanel, "matches");
        deck.add(detailPanel, "detail");
        deck.add(adminPanel, "admin");

        navProfile.addActionListener(e -> showProfile());
        navMatches.addActionListener(e -> showMatches());
        navAdmin.addActionListener(e -> showAdmin());
        navSession.addActionListener(e -> showLogin());

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                new EmptyBorder(Theme.GAP, Theme.PAGE_PAD_X - 4, Theme.GAP, Theme.PAGE_PAD_X - 4)));
        bar.add(Theme.row(Theme.label("RoomSync", Theme.BRAND, Theme.TEXT),
                Theme.muted("  Hostel eligibility and roommate recommendations")), BorderLayout.WEST);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        nav.setOpaque(false);
        nav.add(navProfile);
        nav.add(navMatches);
        nav.add(navAdmin);
        nav.add(navSession);
        bar.add(nav, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(bar, BorderLayout.NORTH);
        add(deck, BorderLayout.CENTER);
        getContentPane().setBackground(Theme.BACKGROUND);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                server.stop();
            }
        });
        setSize(Theme.WINDOW_WIDTH, Theme.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(920, 640));
        setLocationRelativeTo(null);
        showLogin();

        // Background polling for Google Sheet sync every 10 seconds if configured
        Thread syncThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000);
                    int count = syncService.syncFromSheet();
                    if (count > 0) {
                        javax.swing.SwingUtilities.invokeLater(() -> dataChanged());
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "GoogleSheetSyncThread");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    public int getServerPort() {
        return server.getPort();
    }

    public void setLoginRoll(String roll) {
        showLogin();
        loginPanel.setRoll(roll);
    }

    Student currentStudent() {
        return current;
    }

    void signIn(Student s) {
        current = s;
        showProfile();
    }

    void showLogin() {
        current = null;
        loginPanel.reset();
        show("login");
    }

    void showProfile() {
        if (current == null) {
            showLogin();
            return;
        }
        try {
            profilePanel.load(current, hostelDao.findAll());
            show("profile");
        } catch (Exception e) {
            showError("Your profile could not be loaded.", e);
        }
    }

    void showMatches() {
        if (current == null) {
            showLogin();
            return;
        }
        try {
            MatchingService.Outcome outcome = matching.findTopMatches(current, studentDao.findAll(), hostelDao.findAll());
            matchesPanel.load(current, outcome);
            show("matches");
        } catch (Exception e) {
            showError("Matches could not be loaded.", e);
        }
    }

    void showDetail(MatchResult result) {
        try {
            detailPanel.load(current, result, studentDao.hasInterest(current.id, result.candidate.id));
            show("detail");
        } catch (Exception e) {
            showError("This match could not be opened.", e);
        }
    }

    void showAdmin() {
        adminPanel.onShow();
        show("admin");
    }

    /** After admin changes, refresh the signed in student (or sign out if they no longer exist). */
    public void dataChanged() {
        if (current == null) return;
        try {
            current = studentDao.findByRoll(current.rollNo);
        } catch (Exception e) {
            current = null;
        }
        updateNav(null);
    }

    void recordInterest(MatchResult result) throws Exception {
        studentDao.recordInterest(current.id, result.candidate.id);
    }

    /** Friendly message for the user; technical details only go to the console. */
    void showError(String message, Exception e) {
        System.err.println(message + " " + e);
        JOptionPane.showMessageDialog(this, message + "\nPlease try again. If it keeps happening, restart RoomSync.",
                "Something went wrong", JOptionPane.WARNING_MESSAGE);
    }

    private void show(String name) {
        cards.show(deck, name);
        updateNav(name);
    }

    private void updateNav(String screen) {
        boolean signedIn = current != null;
        navProfile.setVisible(signedIn);
        navMatches.setVisible(signedIn);
        navSession.setText(signedIn ? "Log out" : "Sign in");
        navSession.setVisible(signedIn || "admin".equals(screen));
        if (screen != null) {
            navProfile.setSelected("profile".equals(screen));
            navMatches.setSelected("matches".equals(screen) || "detail".equals(screen));
            navAdmin.setSelected("admin".equals(screen));
        }
        repaint();
    }
}
