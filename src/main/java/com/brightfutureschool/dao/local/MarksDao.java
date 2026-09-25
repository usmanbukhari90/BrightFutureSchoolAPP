package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.StudentMark;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MarksDao {

    // Returns a map of studentId -> (subjectId -> marksObtained) for a whole exam,
    // convenient for building the marks table in one query.
    public Map<Long, Map<Long, Double>> getMarksForExam(long examId) throws SQLException {
        String sql = """
            SELECT sm.student_id, sm.exam_subject_id, sm.marks_obtained
            FROM student_marks sm
            JOIN exam_subjects es ON sm.exam_subject_id = es.id
            WHERE es.exam_id = ?
        """;
        Map<Long, Map<Long, Double>> result = new HashMap<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long studentId = rs.getLong("student_id");
                    long subjectId = rs.getLong("exam_subject_id");
                    double marks = rs.getDouble("marks_obtained");
                    boolean isNull = rs.wasNull();

                    result.computeIfAbsent(studentId, k -> new HashMap<>())
                            .put(subjectId, isNull ? null : marks);
                }
            }
        }
        return result;
    }

    public void updateMark(long examSubjectId, long studentId, Double marksObtained) throws SQLException {
        String sql = """
            INSERT INTO student_marks (exam_subject_id, student_id, marks_obtained)
            VALUES (?, ?, ?)
            ON CONFLICT (exam_subject_id, student_id)
            DO UPDATE SET marks_obtained = excluded.marks_obtained
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, examSubjectId);
            ps.setLong(2, studentId);
            if (marksObtained == null) {
                ps.setNull(3, Types.REAL);
            } else {
                ps.setDouble(3, marksObtained);
            }
            ps.executeUpdate();
        }
    }
}