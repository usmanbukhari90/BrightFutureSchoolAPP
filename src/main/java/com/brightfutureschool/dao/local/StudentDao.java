package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDao {

    public List<Student> getStudentsByClass(long classId) throws SQLException {
        String sql = "SELECT * FROM students WHERE class_id = ? ORDER BY roll_no ASC";
        List<Student> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    public List<Student> searchInClass(long classId, String query) throws SQLException {
        String sql = "SELECT * FROM students WHERE class_id = ? AND (full_name LIKE ? OR roll_no LIKE ?) ORDER BY roll_no ASC";
        List<Student> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setString(2, "%" + query + "%");
            ps.setString(3, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    // Roll no is now scoped to the class's own 100-slot block (e.g. class roll_base=1000 -> 1000-1099)
    public String generateNextRollNo(long classId, int rollBase) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM students WHERE class_id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int countInClass = rs.getInt("cnt");
                if (countInClass >= 100) {
                    throw new IllegalStateException("This class has reached its 100-student limit.");
                }
                return String.valueOf(rollBase + countInClass);
            }
        }
    }
    public Student addStudent(Student s) throws SQLException {
        String sql = """
            INSERT INTO students
            (class_id, roll_no, full_name, father_name, mother_name, address, contact, photo_base64,
             date_of_birth, father_profession, mother_profession, student_bform, father_cnic, mother_cnic,  father_contact, gender, religion, nationality)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, s.getClassId());
            ps.setString(2, s.getRollNo());
            ps.setString(3, s.getFullName());
            ps.setString(4, s.getFatherName());
            ps.setString(5, s.getMotherName());
            ps.setString(6, s.getAddress());
            ps.setString(7, s.getContact());
            ps.setString(8, s.getPhotoBase64());
            ps.setString(9, s.getDateOfBirth());
            ps.setString(10, s.getFatherProfession());
            ps.setString(11, s.getMotherProfession());
            ps.setString(12, s.getStudentBform());
            ps.setString(13, s.getFatherCnic());
            ps.setString(14, s.getMotherCnic());
            ps.setString(15, s.getFatherContact());
            ps.setString(16, s.getGender());
            ps.setString(17, s.getReligion());
            ps.setString(18, s.getNationality());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getLong(1));
            }
        }
        return s;
    }
    public void updateStudent(Student s) throws SQLException {
        String sql = """
            UPDATE students SET
                full_name = ?, father_name = ?, mother_name = ?, address = ?, contact = ?,
                photo_base64 = ?, date_of_birth = ?, father_profession = ?, mother_profession = ?,
                student_bform = ?, father_cnic = ?, mother_cnic = ?,
                father_contact = ?, gender = ?, religion = ?, nationality = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getFatherName());
            ps.setString(3, s.getMotherName());
            ps.setString(4, s.getAddress());
            ps.setString(5, s.getContact());
            ps.setString(6, s.getPhotoBase64());
            ps.setString(7, s.getDateOfBirth());
            ps.setString(8, s.getFatherProfession());
            ps.setString(9, s.getMotherProfession());
            ps.setString(10, s.getStudentBform());
            ps.setString(11, s.getFatherCnic());
            ps.setString(12, s.getMotherCnic());
            ps.setString(13, s.getFatherContact());
            ps.setString(14, s.getGender());
            ps.setString(15, s.getReligion());
            ps.setString(16, s.getNationality());
            ps.setLong(17, s.getId());
            ps.executeUpdate();
        }
    }
    public void deleteStudent(long studentId) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.executeUpdate();
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getLong("id"));
        s.setClassId(rs.getLong("class_id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setFullName(rs.getString("full_name"));
        s.setFatherName(rs.getString("father_name"));
        s.setMotherName(rs.getString("mother_name"));
        s.setAddress(rs.getString("address"));
        s.setContact(rs.getString("contact"));
        s.setPhotoBase64(rs.getString("photo_base64"));
        s.setDateOfBirth(rs.getString("date_of_birth"));
        s.setFatherProfession(rs.getString("father_profession"));
        s.setMotherProfession(rs.getString("mother_profession"));
        s.setStudentBform(rs.getString("student_bform"));
        s.setFatherCnic(rs.getString("father_cnic"));
        s.setMotherCnic(rs.getString("mother_cnic"));
        s.setFatherContact(rs.getString("father_contact"));
        s.setGender(rs.getString("gender"));
        s.setReligion(rs.getString("religion"));
        s.setNationality(rs.getString("nationality"));
        return s;
    }
}