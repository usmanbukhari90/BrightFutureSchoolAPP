package com.brightfutureschool.dao.local;

import com.brightfutureschool.db.DatabaseManager;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.Student;
import com.brightfutureschool.model.FeeAssignment;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class FeeDao {

    // Assigns one fee record to every student in a class for the given month.
    // amount can vary per call, so admin can run this multiple times with different amounts if needed.
    public void assignFeeToClass(long classId, String feeType, double amount, String month) throws SQLException {
        List<Student> students = new StudentDao().getStudentsByClass(classId);
        String sql = """
            INSERT INTO fee_records (student_id, fee_type, amount, month, status, created_date)
            VALUES (?, ?, ?, ?, 'PENDING', ?)
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Student s : students) {
                ps.setLong(1, s.getId());
                ps.setString(2, feeType);
                ps.setDouble(3, amount);
                ps.setString(4, month);
                ps.setString(5, LocalDate.now().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Assigns a fee to a single student only (e.g. one student owes a different trip fee amount).
    public void assignFeeToStudent(long studentId, String feeType, double amount, String month) throws SQLException {
        String sql = """
            INSERT INTO fee_records (student_id, fee_type, amount, month, status, created_date)
            VALUES (?, ?, ?, ?, 'PENDING', ?)
        """;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, feeType);
            ps.setDouble(3, amount);
            ps.setString(4, month);
            ps.setString(5, LocalDate.now().toString());
            ps.executeUpdate();
        }
    }

    public void markAsPaid(long feeRecordId) throws SQLException {
        String sql = "UPDATE fee_records SET status = 'PAID', paid_date = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setLong(2, feeRecordId);
            ps.executeUpdate();
        }
    }


    public double getTotalPaidForMonth(String month) throws SQLException {
        String sql = "SELECT SUM(amount) AS total FROM fee_records WHERE status = 'PAID' AND month = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }

    public List<FeeRecord> getPaidRecordsForMonth(String month) throws SQLException {
        String sql = "SELECT * FROM fee_records WHERE status = 'PAID' AND month = ?";
        List<FeeRecord> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }


    // All fee records for one student across all months (full history).
    public List<FeeRecord> getHistoryForStudent(long studentId) throws SQLException {
        String sql = "SELECT * FROM fee_records WHERE student_id = ? ORDER BY month DESC";
        return queryList(sql, studentId);
    }

    // Only this student's current-month records (what dashboard/search should show by default).
    public List<FeeRecord> getCurrentMonthForStudent(long studentId) throws SQLException {
        String sql = "SELECT * FROM fee_records WHERE student_id = ? AND month = ? ORDER BY fee_type ASC";
        String currentMonth = YearMonth.now().toString(); // e.g. "2026-07"
        List<FeeRecord> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, currentMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    private List<FeeRecord> queryList(String sql, long studentId) throws SQLException {
        List<FeeRecord> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }
    // All fee records for a whole class in one specific month (used by the Fee & Balance table)
    public List<FeeRecord> getRecordsForClassAndMonth(long classId, String month) throws SQLException {
        String sql = """
        SELECT fr.* FROM fee_records fr
        JOIN students s ON s.id = fr.student_id
        WHERE s.class_id = ? AND fr.month = ?
        ORDER BY s.roll_no ASC
    """;
        List<FeeRecord> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    // Every distinct month a class has any fee record for (used to populate the month dropdown)
    public List<String> getDistinctMonthsForClass(long classId) throws SQLException {
        String sql = """
        SELECT DISTINCT fr.month FROM fee_records fr
        JOIN students s ON s.id = fr.student_id
        WHERE s.class_id = ? ORDER BY fr.month DESC
    """;
        List<String> months = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) months.add(rs.getString("month"));
            }
        }
        return months;
    }

    public FeeRecord getFeeRecordById(long id) throws SQLException {
        String sql = "SELECT * FROM fee_records WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // Deletes a fee record. If it was already paid, logs a refund entry first (e.g. cancelled trip fee already paid).
    public double deleteFeeRecord(long feeRecordId) throws SQLException {
        FeeRecord record = getFeeRecordById(feeRecordId);
        if (record == null) return 0;

        double refundAmount = 0;
        if ("PAID".equals(record.getStatus())) {
            refundAmount = record.getAmount();
            String insertRefund = """
            INSERT INTO fee_refunds (student_id, fee_type, amount, month, refund_date)
            VALUES (?, ?, ?, ?, ?)
        """;
            try (Connection conn = DatabaseManager.connect();
                 PreparedStatement ps = conn.prepareStatement(insertRefund)) {
                ps.setLong(1, record.getStudentId());
                ps.setString(2, record.getFeeType());
                ps.setDouble(3, refundAmount);
                ps.setString(4, record.getMonth());
                ps.setString(5, java.time.LocalDate.now().toString());
                ps.executeUpdate();
            }
        }

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM fee_records WHERE id = ?")) {
            ps.setLong(1, feeRecordId);
            ps.executeUpdate();
        }

        return refundAmount;
    }

    public double getRefundsForStudentMonth(long studentId, String month) throws SQLException {
        String sql = "SELECT SUM(amount) AS total FROM fee_refunds WHERE student_id = ? AND month = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }





    private FeeRecord mapRow(ResultSet rs) throws SQLException {
        FeeRecord f = new FeeRecord();
        f.setId(rs.getLong("id"));
        f.setStudentId(rs.getLong("student_id"));
        f.setFeeType(rs.getString("fee_type"));
        f.setAmount(rs.getDouble("amount"));
        f.setMonth(rs.getString("month"));
        f.setStatus(rs.getString("status"));
        f.setPaidDate(rs.getString("paid_date"));
        f.setCreatedDate(rs.getString("created_date"));
        return f;
    }


    public List<FeeAssignment> getDistinctFeeAssignments(long classId) throws SQLException {
        String sql = """
        SELECT DISTINCT fr.fee_type, fr.month FROM fee_records fr
        JOIN students s ON s.id = fr.student_id
        WHERE s.class_id = ?
        ORDER BY fr.month DESC, fr.fee_type ASC
    """;
        List<FeeAssignment> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new FeeAssignment(rs.getString("fee_type"), rs.getString("month")));
            }
        }
        return result;
    }

    public List<FeeRecord> getRecordsForClassFeeTypeAndMonth(long classId, String feeType, String month) throws SQLException {
        String sql = """
        SELECT fr.* FROM fee_records fr
        JOIN students s ON s.id = fr.student_id
        WHERE s.class_id = ? AND fr.fee_type = ? AND fr.month = ?
        ORDER BY s.roll_no ASC
    """;
        List<FeeRecord> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setString(2, feeType);
            ps.setString(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    // Bulk delete: removes this fee from every student who has it, refunding anyone who already paid.
    public double deleteFeeByTypeAndMonth(long classId, String feeType, String month) throws SQLException {
        List<FeeRecord> records = getRecordsForClassFeeTypeAndMonth(classId, feeType, month);
        double totalRefund = 0;

        try (Connection conn = DatabaseManager.connect()) {
            for (FeeRecord r : records) {
                if ("PAID".equals(r.getStatus())) {
                    totalRefund += r.getAmount();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO fee_refunds (student_id, fee_type, amount, month, refund_date) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setLong(1, r.getStudentId());
                        ps.setString(2, r.getFeeType());
                        ps.setDouble(3, r.getAmount());
                        ps.setString(4, r.getMonth());
                        ps.setString(5, java.time.LocalDate.now().toString());
                        ps.executeUpdate();
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM fee_records WHERE student_id IN (SELECT id FROM students WHERE class_id = ?) AND fee_type = ? AND month = ?")) {
                ps.setLong(1, classId);
                ps.setString(2, feeType);
                ps.setString(3, month);
                ps.executeUpdate();
            }
        }
        return totalRefund;
    }


    public String getOrCreateReceiptNumber(long studentId, String month) throws SQLException {
        String select = "SELECT receipt_no FROM fee_receipts WHERE student_id = ? AND month = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setLong(1, studentId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("receipt_no");
            }
        }

        try (Connection conn = DatabaseManager.connect()) {
            String receiptNo;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM fee_receipts")) {
                rs.next();
                int next = rs.getInt("cnt") + 1;
                receiptNo = "BFS-" + String.format("%05d", next);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO fee_receipts (receipt_no, student_id, month, generated_date) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, receiptNo);
                ps.setLong(2, studentId);
                ps.setString(3, month);
                ps.setString(4, java.time.LocalDate.now().toString());
                ps.executeUpdate();
            }
            return receiptNo;
        }
    }
}