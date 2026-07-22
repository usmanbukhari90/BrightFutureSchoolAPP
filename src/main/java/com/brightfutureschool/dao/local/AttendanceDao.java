package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.AttendanceRecord;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceDao {

    // Returns studentId -> status ("PRESENT"/"ABSENT") for a given class + date.
    // Students with no record yet simply won't appear in the map (treated as "not marked").
    public Map<Long, String> getAttendanceForClassAndDate(long classId, String date) throws SQLException {
        String sql = "SELECT student_id, status FROM attendance WHERE class_id = ? AND attendance_date = ?";
        Map<Long, String> result = new HashMap<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getLong("student_id"), rs.getString("status"));
                }
            }
        }
        return result;
    }

    // Insert or update a single student's attendance for a given date.
    public void markAttendance(long studentId, long classId, String date, String status) throws SQLException {
        String sql = """
            INSERT INTO attendance (student_id, class_id, attendance_date, status)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (student_id, attendance_date)
            DO UPDATE SET status = excluded.status
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, classId);
            ps.setString(3, date);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    // Mark every student in a class the same way for a given date (Present All / Absent All).
    public void markAllForClass(long classId, String date, String status, List<Long> studentIds) throws SQLException {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO attendance (student_id, class_id, attendance_date, status)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (student_id, attendance_date)
                    DO UPDATE SET status = excluded.status
                """)) {
                for (Long studentId : studentIds) {
                    ps.setLong(1, studentId);
                    ps.setLong(2, classId);
                    ps.setString(3, date);
                    ps.setString(4, status);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Counts of PRESENT/ABSENT across ALL classes for a given date.
// Always returns both keys (defaulting to 0) so callers can safely unbox without null-checks.
    public Map<String, Integer> getStatusCountsForDate(String date) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS cnt FROM attendance WHERE attendance_date = ? GROUP BY status";
        Map<String, Integer> result = new HashMap<>();
        result.put("PRESENT", 0);
        result.put("ABSENT", 0);
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("status"), rs.getInt("cnt"));
                }
            }
        }
        return result;
    }
    // Full attendance history for one student, across all dates — used by the "Details" view.
    public List<AttendanceRecord> getHistoryForStudent(long studentId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE student_id = ? ORDER BY attendance_date DESC";
        List<AttendanceRecord> result = new java.util.ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setId(rs.getLong("id"));
                    r.setStudentId(rs.getLong("student_id"));
                    r.setClassId(rs.getLong("class_id"));
                    r.setAttendanceDate(rs.getString("attendance_date"));
                    r.setStatus(rs.getString("status"));
                    result.add(r);
                }
            }
        }
        return result;
    }
}