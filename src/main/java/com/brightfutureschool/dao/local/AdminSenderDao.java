package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminSenderDao {

    public List<String> getAllNumbers() throws SQLException {
        String sql = "SELECT number FROM admin_senders ORDER BY id ASC";
        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(rs.getString("number"));
        }
        return result;
    }

    public void addNumber(String number) throws SQLException {
        String sql = "INSERT OR IGNORE INTO admin_senders (number) VALUES (?)";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            ps.executeUpdate();
        }
    }

    public void deleteNumber(String number) throws SQLException {
        String sql = "DELETE FROM admin_senders WHERE number = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            ps.executeUpdate();
        }
    }
}