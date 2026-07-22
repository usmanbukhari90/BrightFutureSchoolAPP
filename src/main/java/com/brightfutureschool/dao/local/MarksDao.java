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
        String sql = "UPDATE student_marks SET marks_obtained = ? WHERE exam_subject_id = ? AND student_id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (marksObtained == null) {
                ps.setNull(1, Types.REAL);
            } else {
                ps.setDouble(1, marksObtained);
            }
            ps.setLong(2, examSubjectId);
            ps.setLong(3, studentId);
            ps.executeUpdate();
        }
    }
}