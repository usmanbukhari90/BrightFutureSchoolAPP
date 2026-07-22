package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.Exam;
import com.brightfutureschool.model.ExamSubject;
import com.brightfutureschool.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDao {

    public List<Exam> getExamsByClass(long classId) throws SQLException {
        String sql = "SELECT * FROM exams WHERE class_id = ? ORDER BY id DESC";
        List<Exam> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapExam(rs));
            }
        }
        return result;
    }

    // Creates the exam, its subjects, and blank mark rows for every student currently in the class.
    public Exam createExamWithSubjects(long classId, String examName, String examYear,
                                       List<ExamSubject> subjects, List<Student> studentsInClass) throws SQLException {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                long examId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO exams (class_id, exam_name, exam_year) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, classId);
                    ps.setString(2, examName);
                    ps.setString(3, examYear);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        examId = keys.getLong(1);
                    }
                }

                List<Long> subjectIds = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO exam_subjects (exam_id, subject_name, total_marks) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    for (ExamSubject subject : subjects) {
                        ps.setLong(1, examId);
                        ps.setString(2, subject.getSubjectName());
                        ps.setInt(3, subject.getTotalMarks());
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();
                            subjectIds.add(keys.getLong(1));
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO student_marks (exam_subject_id, student_id, marks_obtained) VALUES (?, ?, NULL)")) {
                    for (Long subjectId : subjectIds) {
                        for (Student student : studentsInClass) {
                            ps.setLong(1, subjectId);
                            ps.setLong(2, student.getId());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                conn.commit();
                Exam exam = new Exam();
                exam.setId(examId);
                exam.setClassId(classId);
                exam.setExamName(examName);
                exam.setExamYear(examYear);
                return exam;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ExamSubject> getSubjectsForExam(long examId) throws SQLException {
        String sql = "SELECT * FROM exam_subjects WHERE exam_id = ? ORDER BY id ASC";
        List<ExamSubject> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExamSubject s = new ExamSubject();
                    s.setId(rs.getLong("id"));
                    s.setExamId(rs.getLong("exam_id"));
                    s.setSubjectName(rs.getString("subject_name"));
                    s.setTotalMarks(rs.getInt("total_marks"));
                    result.add(s);
                }
            }
        }
        return result;
    }

    public void deleteExam(long examId) throws SQLException {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM exams WHERE id = ?")) {
            ps.setLong(1, examId);
            ps.executeUpdate();
        }
    }

    private Exam mapExam(ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setId(rs.getLong("id"));
        e.setClassId(rs.getLong("class_id"));
        e.setExamName(rs.getString("exam_name"));
        e.setExamYear(rs.getString("exam_year"));
        return e;
    }
}