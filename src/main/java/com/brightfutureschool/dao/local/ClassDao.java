package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.SchoolClass;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDao {

    private static final int BLOCK_SIZE = 100;
    private static final int FIRST_ROLL_BASE = 1000;

    public List<SchoolClass> getAllClasses() throws SQLException {
        String sql = "SELECT * FROM classes ORDER BY roll_base ASC";
        List<SchoolClass> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public SchoolClass createClass(SchoolClass schoolClass) throws SQLException {
        int nextRollBase = getNextRollBase();

        String sql = "INSERT INTO classes (class_name, section, roll_base) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, schoolClass.getClassName());
            ps.setString(2, schoolClass.getSection());
            ps.setInt(3, nextRollBase);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    schoolClass.setId(keys.getLong(1));
                }
            }
        }
        schoolClass.setRollBase(nextRollBase);
        return schoolClass;
    }

    public void deleteClass(long classId) throws SQLException {
        String sql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.executeUpdate();
        }
    }

    private int getNextRollBase() throws SQLException {
        String sql = "SELECT MAX(roll_base) AS maxBase FROM classes";
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int maxBase = rs.getInt("maxBase");
                if (!rs.wasNull()) {
                    return maxBase + BLOCK_SIZE;
                }
            }
            return FIRST_ROLL_BASE;
        }
    }

    private SchoolClass mapRow(ResultSet rs) throws SQLException {
        SchoolClass c = new SchoolClass();
        c.setId(rs.getLong("id"));
        c.setClassName(rs.getString("class_name"));
        c.setSection(rs.getString("section"));
        c.setRollBase(rs.getInt("roll_base"));
        return c;
    }
}