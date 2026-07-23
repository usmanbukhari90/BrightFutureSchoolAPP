package com.brightfutureschool.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_PATH = getDatabasePath();

    private static String getDatabasePath() {
        String appDataDir = System.getenv("APPDATA");
        if (appDataDir == null) {
            appDataDir = System.getProperty("user.home");
        }
        java.io.File folder = new java.io.File(appDataDir, "BrightFutureSchoolApp");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new java.io.File(folder, "brightfutureschool.db").getAbsolutePath();
    }
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initSchema() {
        String createClasses = """
            CREATE TABLE IF NOT EXISTS classes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_name TEXT NOT NULL,
                section TEXT NOT NULL,
                roll_base INTEGER NOT NULL
            );
        """;


        String createExams = """
            CREATE TABLE IF NOT EXISTS exams (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                exam_name TEXT NOT NULL,
                exam_year TEXT,
                FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
            );
        """;

        String createExamSubjects = """
            CREATE TABLE IF NOT EXISTS exam_subjects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_id INTEGER NOT NULL,
                subject_name TEXT NOT NULL,
                total_marks INTEGER NOT NULL,
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
            );
        """;

        String createStudentMarks = """
            CREATE TABLE IF NOT EXISTS student_marks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_subject_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                marks_obtained REAL,
                FOREIGN KEY (exam_subject_id) REFERENCES exam_subjects(id) ON DELETE CASCADE,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                UNIQUE (exam_subject_id, student_id)
            );
        """;

        String createAttendance = """
            CREATE TABLE IF NOT EXISTS attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                class_id INTEGER NOT NULL,
                attendance_date TEXT NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
                UNIQUE (student_id, attendance_date)
            );
        """;

        String createStudents = """
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                roll_no TEXT NOT NULL UNIQUE,
                full_name TEXT NOT NULL,
                father_name TEXT NOT NULL,
                mother_name TEXT NOT NULL,
                address TEXT,
                contact TEXT,
                father_contact TEXT,
                gender TEXT,
                religion TEXT,
                nationality TEXT,
                photo_base64 TEXT,
                date_of_birth TEXT,
                father_profession TEXT,
                mother_profession TEXT,
                student_bform TEXT,
                father_cnic TEXT,
                mother_cnic TEXT,
                FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
            );
        """;

        String createFeeRecords = """
    CREATE TABLE IF NOT EXISTS fee_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id INTEGER NOT NULL,
        fee_type TEXT NOT NULL,
        amount REAL NOT NULL,
        month TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'PENDING',
        paid_date TEXT,
        created_date TEXT NOT NULL,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    );
""";

        String createFeeRefunds = """
    CREATE TABLE IF NOT EXISTS fee_refunds (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id INTEGER NOT NULL,
        fee_type TEXT NOT NULL,
        amount REAL NOT NULL,
        month TEXT NOT NULL,
        refund_date TEXT NOT NULL,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    );
""";

        String createFeeReceipts = """
    CREATE TABLE IF NOT EXISTS fee_receipts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        receipt_no TEXT NOT NULL UNIQUE,
        student_id INTEGER NOT NULL,
        month TEXT NOT NULL,
        generated_date TEXT NOT NULL,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    );
""";
        String createAdminSenders = """
    CREATE TABLE IF NOT EXISTS admin_senders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        number TEXT NOT NULL UNIQUE
    );
""";
        String createTeachers = """
    CREATE TABLE IF NOT EXISTS teachers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        sr_no INTEGER,
        full_name TEXT NOT NULL,
        father_name TEXT,
        contact TEXT,
        date_of_birth TEXT,
        cnic TEXT,
        qualification TEXT,
        previous_experience TEXT,
        nationality TEXT,
        religion TEXT,
        photo_base64 TEXT,
        assigned_class_id INTEGER,
        subject TEXT,
        salary REAL,
        FOREIGN KEY (assigned_class_id) REFERENCES classes(id) ON DELETE SET NULL
    );
""";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createClasses);
            stmt.execute(createStudents);
            stmt.execute(createFeeRecords);
            stmt.execute(createExams);
            stmt.execute(createExamSubjects);
            stmt.execute(createStudentMarks);
            runMigrations(conn);
            stmt.execute(createClasses);
            stmt.execute(createStudents);
            stmt.execute(createFeeRecords);
            stmt.execute(createFeeRefunds);
            stmt.execute(createFeeReceipts);
            stmt.execute(createAttendance);
            stmt.execute(createAdminSenders);
            stmt.execute(createTeachers);
            runMigrations(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    // Adds any newly-introduced columns to existing tables without wiping data.
    private static void runMigrations(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "classes", "roll_base", "INTEGER NOT NULL DEFAULT 1000");
        addColumnIfMissing(conn, "students", "father_contact", "TEXT");
        addColumnIfMissing(conn, "students", "gender", "TEXT");
        addColumnIfMissing(conn, "students", "religion", "TEXT");
        addColumnIfMissing(conn, "students", "nationality", "TEXT");
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                }
            }
        }
    }
}