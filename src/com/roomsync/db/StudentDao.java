package com.roomsync.db;

import com.roomsync.model.Requirement;
import com.roomsync.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads and writes students, their requirements and interest signals. */
public class StudentDao {
    private static final String COLUMNS = "name, roll_no, email, program, academic_year, score_type, score, "
            + "verification_status, sleep_time, wake_time, cleanliness, noise_tolerance, study_in_room, study_time, "
            + "room_environment, guest_frequency, food_preference, food_sharing, room_organization, "
            + "hostel_pref_ids, occupancy_pref, gender";

    public Student findByRoll(String rollNo) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT student_id, " + COLUMNS + " FROM STUDENT WHERE roll_no = ? COLLATE NOCASE")) {
            ps.setString(1, rollNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Student s = map(rs);
                loadRequirements(c, List.of(s));
                return s;
            }
        }
    }

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT student_id, " + COLUMNS + " FROM STUDENT ORDER BY student_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            loadRequirements(c, list);
        }
        return list;
    }

    public Set<String> allRollNumbers() throws SQLException {
        Set<String> rolls = new HashSet<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT roll_no FROM STUDENT");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rolls.add(rs.getString(1).toUpperCase());
        }
        return rolls;
    }

    /** Inserts students and their requirements in one transaction. */
    public void insertAll(List<Student> students) throws SQLException {
        String placeholders = "?,".repeat(22);
        String sql = "INSERT INTO STUDENT (" + COLUMNS + ") VALUES (" + placeholders.substring(0, placeholders.length() - 1) + ")";
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql);
                 PreparedStatement req = c.prepareStatement(
                         "INSERT INTO REQUIREMENT (student_id, factor, importance_level) VALUES (?, ?, ?)");
                 Statement idQuery = c.createStatement()) {
                for (Student s : students) {
                    bind(ps, s);
                    ps.executeUpdate();
                    try (ResultSet rs = idQuery.executeQuery("SELECT last_insert_rowid()")) {
                        s.id = rs.getInt(1);
                    }
                    for (Requirement r : s.requirements) {
                        req.setInt(1, s.id);
                        req.setString(2, r.factor);
                        req.setString(3, r.importanceLevel);
                        req.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Marks a student's score as VERIFIED. Returns false if the roll number does not exist. */
    public boolean markVerified(String rollNo) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE STUDENT SET verification_status = 'VERIFIED' WHERE roll_no = ? COLLATE NOCASE")) {
            ps.setString(1, rollNo.trim());
            return ps.executeUpdate() > 0;
        }
    }

    /** Records a preference signal only. This never allocates or reserves anything. */
    public void recordInterest(int studentId, int candidateId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO INTEREST (student_id, candidate_id) VALUES (?, ?)")) {
            ps.setInt(1, studentId);
            ps.setInt(2, candidateId);
            ps.executeUpdate();
        }
    }

    public boolean hasInterest(int studentId, int candidateId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM INTEREST WHERE student_id = ? AND candidate_id = ?")) {
            ps.setInt(1, studentId);
            ps.setInt(2, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void loadRequirements(Connection c, List<Student> students) throws SQLException {
        Map<Integer, Student> byId = new LinkedHashMap<>();
        for (Student s : students) byId.put(s.id, s);
        try (PreparedStatement ps = c.prepareStatement("SELECT student_id, factor, importance_level FROM REQUIREMENT");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Student s = byId.get(rs.getInt(1));
                if (s != null) s.requirements.add(new Requirement(rs.getString(2), rs.getString(3)));
            }
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.id = rs.getInt("student_id");
        s.name = rs.getString("name");
        s.rollNo = rs.getString("roll_no");
        s.email = rs.getString("email");
        s.program = rs.getString("program");
        s.academicYear = rs.getInt("academic_year");
        s.scoreType = rs.getString("score_type");
        s.score = rs.getDouble("score");
        s.verificationStatus = rs.getString("verification_status");
        s.sleepTime = rs.getString("sleep_time");
        s.wakeTime = rs.getString("wake_time");
        s.cleanliness = intOrNull(rs, "cleanliness");
        s.noiseTolerance = intOrNull(rs, "noise_tolerance");
        s.studyInRoom = rs.getString("study_in_room");
        s.studyTime = rs.getString("study_time");
        s.roomEnvironment = rs.getString("room_environment");
        s.guestFrequency = rs.getString("guest_frequency");
        s.foodPreference = rs.getString("food_preference");
        s.foodSharing = rs.getString("food_sharing");
        s.roomOrganization = rs.getString("room_organization");
        s.hostelPrefIds = rs.getString("hostel_pref_ids");
        s.occupancyPref = rs.getString("occupancy_pref");
        s.gender = rs.getString("gender");
        return s;
    }

    private void bind(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.name);
        ps.setString(2, s.rollNo);
        ps.setString(3, s.email);
        ps.setString(4, s.program);
        ps.setInt(5, s.academicYear);
        ps.setString(6, s.scoreType);
        ps.setDouble(7, s.score);
        ps.setString(8, s.verificationStatus);
        ps.setString(9, s.sleepTime);
        ps.setString(10, s.wakeTime);
        setInt(ps, 11, s.cleanliness);
        setInt(ps, 12, s.noiseTolerance);
        ps.setString(13, s.studyInRoom);
        ps.setString(14, s.studyTime);
        ps.setString(15, s.roomEnvironment);
        ps.setString(16, s.guestFrequency);
        ps.setString(17, s.foodPreference);
        ps.setString(18, s.foodSharing);
        ps.setString(19, s.roomOrganization);
        ps.setString(20, s.hostelPrefIds);
        ps.setString(21, s.occupancyPref);
        ps.setString(22, s.gender == null ? "MALE" : s.gender);
    }

    private static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static void setInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }
}
