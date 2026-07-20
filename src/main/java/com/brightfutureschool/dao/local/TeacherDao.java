package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.Teacher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherDao {

    public List<Teacher> getAllTeachers() throws SQLException {
        String sql = "SELECT * FROM teachers ORDER BY sr_no ASC";
        List<Teacher> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public List<Teacher> searchTeachers(String query) throws SQLException {
        String sql = "SELECT * FROM teachers WHERE full_name LIKE ? OR subject LIKE ? ORDER BY sr_no ASC";
        List<Teacher> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    private int getNextSrNo() throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM teachers";
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt("cnt") + 1;
        }
    }

    public Teacher addTeacher(Teacher t) throws SQLException {
        t.setSrNo(getNextSrNo());
        String sql = """
            INSERT INTO teachers
            (sr_no, full_name, father_name, contact, date_of_birth, cnic, qualification,
             previous_experience, nationality, religion, photo_base64, assigned_class_id, subject, salary)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getSrNo());
            ps.setString(2, t.getFullName());
            ps.setString(3, t.getFatherName());
            ps.setString(4, t.getContact());
            ps.setString(5, t.getDateOfBirth());
            ps.setString(6, t.getCnic());
            ps.setString(7, t.getQualification());
            ps.setString(8, t.getPreviousExperience());
            ps.setString(9, t.getNationality());
            ps.setString(10, t.getReligion());
            ps.setString(11, t.getPhotoBase64());
            if (t.getAssignedClassId() != null) ps.setLong(12, t.getAssignedClassId()); else ps.setNull(12, Types.INTEGER);
            ps.setString(13, t.getSubject());
            ps.setDouble(14, t.getSalary());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setId(keys.getLong(1));
            }
        }
        return t;
    }

    public void updateTeacher(Teacher t) throws SQLException {
        String sql = """
            UPDATE teachers SET
                full_name = ?, father_name = ?, contact = ?, date_of_birth = ?, cnic = ?,
                qualification = ?, previous_experience = ?, nationality = ?, religion = ?,
                photo_base64 = ?, assigned_class_id = ?, subject = ?, salary = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getFullName());
            ps.setString(2, t.getFatherName());
            ps.setString(3, t.getContact());
            ps.setString(4, t.getDateOfBirth());
            ps.setString(5, t.getCnic());
            ps.setString(6, t.getQualification());
            ps.setString(7, t.getPreviousExperience());
            ps.setString(8, t.getNationality());
            ps.setString(9, t.getReligion());
            ps.setString(10, t.getPhotoBase64());
            if (t.getAssignedClassId() != null) ps.setLong(11, t.getAssignedClassId()); else ps.setNull(11, Types.INTEGER);
            ps.setString(12, t.getSubject());
            ps.setDouble(13, t.getSalary());
            ps.setLong(14, t.getId());
            ps.executeUpdate();
        }
    }

    public void deleteTeacher(long teacherId) throws SQLException {
        String sql = "DELETE FROM teachers WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, teacherId);
            ps.executeUpdate();
        }
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        Teacher t = new Teacher();
        t.setId(rs.getLong("id"));
        t.setSrNo(rs.getInt("sr_no"));
        t.setFullName(rs.getString("full_name"));
        t.setFatherName(rs.getString("father_name"));
        t.setContact(rs.getString("contact"));
        t.setDateOfBirth(rs.getString("date_of_birth"));
        t.setCnic(rs.getString("cnic"));
        t.setQualification(rs.getString("qualification"));
        t.setPreviousExperience(rs.getString("previous_experience"));
        t.setNationality(rs.getString("nationality"));
        t.setReligion(rs.getString("religion"));
        t.setPhotoBase64(rs.getString("photo_base64"));
        long classId = rs.getLong("assigned_class_id");
        t.setAssignedClassId(rs.wasNull() ? null : classId);
        t.setSubject(rs.getString("subject"));
        t.setSalary(rs.getDouble("salary"));
        return t;
    }
}