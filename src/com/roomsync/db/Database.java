package com.roomsync.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens SQLite connections and creates and seeds the database on first run. */
public class Database {
    private static final String[] TABLES = {"HOSTEL", "STUDENT", "REQUIREMENT", "INTEREST"};
    private static String dbFile = "roomsync.db";

    public static void setFile(String file) {
        dbFile = file;
    }

    /** New connection with foreign keys enforced (SQLite turns them off by default). */
    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** Creates missing tables, then seeds demo data if there are no hostels or students yet. */
    public static void init() throws SQLException, IOException {
        try (Connection c = connect()) {
            if (allTablesExist(c) && !hasGenderColumn(c)) dropAllTables(c);   // old database: rebuild with gender
            if (!allTablesExist(c)) runScript(c, Path.of("resources", "schema.sql"));
            if (isEmpty(c)) runScript(c, Path.of("resources", "seed.sql"));
        }
    }

    /** Deletes all rows and reloads seed.sql. */
    public static void resetDemoData() throws SQLException, IOException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM INTEREST");
                st.executeUpdate("DELETE FROM REQUIREMENT");
                st.executeUpdate("DELETE FROM STUDENT");
                st.executeUpdate("DELETE FROM HOSTEL");
                st.executeUpdate("DELETE FROM sqlite_sequence");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
            c.setAutoCommit(true);
            runScript(c, Path.of("resources", "seed.sql"));
        }
    }

    private static boolean allTablesExist(Connection c) throws SQLException {
        try (var ps = c.prepareStatement("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            for (String table : TABLES) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.getInt(1) == 0) return false;
                }
            }
        }
        return true;
    }

    private static boolean hasGenderColumn(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(STUDENT)")) {
            while (rs.next()) {
                if ("gender".equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    /** Used once when an older database without the gender column is found; demo data is reloaded afterwards. */
    private static void dropAllTables(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (String t : new String[] {"INTEREST", "REQUIREMENT", "STUDENT", "HOSTEL"}) st.executeUpdate("DROP TABLE IF EXISTS " + t);
        }
    }

    private static boolean isEmpty(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM HOSTEL) + (SELECT COUNT(*) FROM STUDENT)")) {
            return rs.getInt(1) == 0;
        }
    }

    /** Runs a .sql file in one transaction. Statements end with ';'. Lines starting with '--' are comments. */
    static void runScript(Connection c, Path file) throws SQLException, IOException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().startsWith("--")) sql.append(line).append('\n');
        }
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            for (String stmt : sql.toString().split(";")) {
                if (!stmt.isBlank()) st.execute(stmt);
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(auto);
        }
    }
}
