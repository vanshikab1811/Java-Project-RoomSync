package com.roomsync.logic;

import com.roomsync.db.HostelDao;
import com.roomsync.db.StudentDao;
import com.roomsync.model.Hostel;
import com.roomsync.model.Student;
import com.roomsync.ui.MainFrame;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Built-in HTTP server listening on localhost.
 * Allows the browser registration form to automatically post responses directly into SQLite!
 */
public class RegistrationServer {
    private final MainFrame frame;
    private final StudentDao studentDao;
    private final HostelDao hostelDao;
    private HttpServer server;
    private int port = 8080;

    public RegistrationServer(MainFrame frame, StudentDao studentDao, HostelDao hostelDao) {
        this.frame = frame;
        this.studentDao = studentDao;
        this.hostelDao = hostelDao;
    }

    public void start(int preferredPort) {
        this.port = preferredPort;
        for (int p = preferredPort; p < preferredPort + 10; p++) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                this.port = p;
                break;
            } catch (IOException ignored) {}
        }
        if (server == null) {
            System.err.println("Could not bind RegistrationServer to any port around " + preferredPort);
            return;
        }

        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/form", new FormHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("RoomSync registration server active on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);

            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            String body;
            try (InputStream in = ex.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            try {
                List<Hostel> hostels = hostelDao.findAll();
                Set<String> existing = studentDao.allRollNumbers();
                CsvImporter.Result result = new CsvImporter().parseString(body, hostels, existing);

                if (result.students.isEmpty()) {
                    String err = result.errors.isEmpty() ? "No valid student data found." : String.join("; ", result.errors);
                    send(ex, 400, "{\"success\": false, \"error\": \"" + escape(err) + "\"}");
                    return;
                }

                Student s = result.students.get(0);
                studentDao.insertAll(result.students);

                // Update UI on Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    frame.dataChanged();
                    frame.setLoginRoll(s.rollNo);
                });

                send(ex, 200, "{\"success\": true, \"message\": \"Student registered\", \"rollNo\": \"" 
                        + escape(s.rollNo) + "\", \"name\": \"" + escape(s.name) + "\"}");
            } catch (Exception e) {
                send(ex, 500, "{\"success\": false, \"error\": \"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            int count = 0;
            try { count = studentDao.allRollNumbers().size(); } catch (Exception ignored) {}
            send(ex, 200, "{\"status\": \"running\", \"app\": \"RoomSync\", \"studentCount\": " + count + "}");
        }
    }

    private class FormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            Path file = Path.of("resources", "registration_form.html");
            if (Files.exists(file)) {
                byte[] bytes = Files.readAllBytes(file);
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
            } else {
                send(ex, 404, "Form not found.");
            }
        }
    }

    private static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept");
    }

    private static void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}

