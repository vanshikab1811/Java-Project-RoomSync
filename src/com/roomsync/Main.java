package com.roomsync;

import com.roomsync.db.Database;
import com.roomsync.logic.CompatibilityCalculator;
import com.roomsync.ui.MainFrame;
import com.roomsync.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Entry point: loads config, prepares the database and opens the window. Run from the project folder. */
public class Main {

    public static void main(String[] args) {
        Properties config = loadConfig();
        Database.setFile(config.getProperty("db.file", "roomsync.db"));
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                Database.init();
            } catch (Exception e) {
                System.err.println("Database start-up failed: " + e);
                JOptionPane.showMessageDialog(null,
                        "RoomSync could not open its database.\n\n"
                                + "Start it from the project folder with run.bat or run.sh so it can find\n"
                                + "the resources and lib folders.",
                        "RoomSync", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new MainFrame(config).setVisible(true);
        });
    }

    /** Reads resources/config.properties. Missing values fall back to the built-in defaults. */
    public static Properties loadConfig() {
        Properties p = new Properties();
        Path file = Path.of("resources", "config.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + file + ", using defaults.");
            }
        }
        double total = 0;
        CompatibilityCalculator calc = new CompatibilityCalculator(p);
        for (String f : CompatibilityCalculator.FACTORS) total += calc.weightOf(f);
        if (Math.abs(total - 100) > 0.001) {
            System.err.println("Warning: scoring weights add up to " + total + "%, not 100%. Scores are normalised anyway.");
        }
        return p;
    }
}
