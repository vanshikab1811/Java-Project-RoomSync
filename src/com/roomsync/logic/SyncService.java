package com.roomsync.logic;

import com.roomsync.db.HostelDao;
import com.roomsync.db.StudentDao;
import com.roomsync.model.Hostel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Automatically synchronizes student responses from a published Google Sheet CSV URL into SQLite.
 */
public class SyncService {
    private final Properties config;
    private final StudentDao studentDao;
    private final HostelDao hostelDao;

    public SyncService(Properties config, StudentDao studentDao, HostelDao hostelDao) {
        this.config = config;
        this.studentDao = studentDao;
        this.hostelDao = hostelDao;
    }

    /**
     * Checks if a Google Sheet sync URL is configured and pulls new responses.
     * @return number of new students imported (0 if none or not configured).
     */
    public synchronized int syncFromSheet() {
        String urlStr = config.getProperty("sheet.sync.url", "").trim();
        urlStr = normalizeSheetUrl(urlStr);
        if (urlStr == null) return 0;

        try {
            int redirects = 0;
            HttpURLConnection conn;
            while (redirects < 5) {
                URI uri = URI.create(urlStr);
                conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                if (code >= 300 && code <= 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null) {
                        urlStr = loc;
                        redirects++;
                        continue;
                    }
                }
                if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                    }
                    List<Hostel> hostels = hostelDao.findAll();
                    Set<String> existing = studentDao.allRollNumbers();
                    CsvImporter.Result res = new CsvImporter().parseString(sb.toString(), hostels, existing);
                    if (!res.students.isEmpty()) {
                        studentDao.insertAll(res.students);
                        return res.students.size();
                    }
                }
                break;
            }
        } catch (Exception e) {
            System.err.println("Google Sheet sync notice: " + e.getMessage());
        }
        return 0;
    }

    public static String normalizeSheetUrl(String urlStr) {
        if (urlStr == null) return null;
        urlStr = urlStr.trim();
        if (urlStr.isEmpty() || urlStr.startsWith("#")) return null;
        if (urlStr.contains("export?format=csv") || urlStr.contains("output=csv")) {
            return urlStr;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)").matcher(urlStr);
        if (m.find()) {
            return "https://docs.google.com/spreadsheets/d/" + m.group(1) + "/export?format=csv";
        }
        return urlStr;
    }
}
