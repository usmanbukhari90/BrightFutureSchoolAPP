package com.brightfutureschool.backup;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.ExamDao;
import com.brightfutureschool.dao.local.MarksDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class BackupService {

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final ExamDao examDao = new ExamDao();
    private final MarksDao marksDao = new MarksDao();
    private final FeeDao feeDao = new FeeDao();

    // Runs the full backup into a new "Session <year>" folder inside destinationRoot.
    // Returns the created session folder so the caller can offer to open it.
    public File runBackup(File destinationRoot) throws Exception {
        int year = LocalDate.now().getYear();
        File sessionFolder = uniqueFolder(destinationRoot, "Session " + year);
        sessionFolder.mkdirs();

        exportStudentRecords(sessionFolder);
        exportTeacherRecordsPlaceholder(sessionFolder);
        exportResults(sessionFolder);
        exportDuesAndBalance(sessionFolder);


        return sessionFolder;
    }

    private void exportStudentRecords(File sessionFolder) throws Exception {
        File studentFolder = new File(sessionFolder, "Student Records");
        studentFolder.mkdirs();

        List<SchoolClass> classes = classDao.getAllClasses();
        for (SchoolClass c : classes) {
            File file = new File(studentFolder, safeName(c.getClassName() + " - " + c.getSection()) + ".csv");
            List<Student> students = studentDao.getStudentsByClass(c.getId());

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
                writer.println(String.join(",",
                        "Roll No", "Full Name", "Father Name", "Mother Name", "Gender", "Date of Birth",
                        "Religion", "Nationality", "Contact", "Father Contact", "Address",
                        "Father Profession", "Mother Profession", "Student B-Form", "Father CNIC", "Mother CNIC"));

                for (Student s : students) {
                    writer.println(csvRow(
                            s.getRollNo(), s.getFullName(), s.getFatherName(), s.getMotherName(),
                            s.getGender(), s.getDateOfBirth(), s.getReligion(), s.getNationality(),
                            s.getContact(), s.getFatherContact(), s.getAddress(),
                            s.getFatherProfession(), s.getMotherProfession(),
                            s.getStudentBform(), s.getFatherCnic(), s.getMotherCnic()
                    ));
                }
            }
        }
    }

    private void exportTeacherRecordsPlaceholder(File sessionFolder) throws IOException {
        File teacherFolder = new File(sessionFolder, "Teacher Records");
        teacherFolder.mkdirs();
        File note = new File(teacherFolder, "README.txt");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(note.toPath(), StandardCharsets.UTF_8))) {
            writer.println("Teacher records module has not been built into the system yet.");
            writer.println("This folder is reserved and will be populated automatically once that module is added.");
        }
    }

    private void exportResults(File sessionFolder) throws Exception {
        File resultsFolder = new File(sessionFolder, "Results");
        resultsFolder.mkdirs();

        List<SchoolClass> classes = classDao.getAllClasses();
        for (SchoolClass c : classes) {
            List<Exam> exams = examDao.getExamsByClass(c.getId());
            if (exams.isEmpty()) continue;

            File classFolder = new File(resultsFolder, safeName(c.getClassName() + " - " + c.getSection()));
            classFolder.mkdirs();

            List<Student> students = studentDao.getStudentsByClass(c.getId());

            for (Exam exam : exams) {
                List<ExamSubject> subjects = examDao.getSubjectsForExam(exam.getId());
                Map<Long, Map<Long, Double>> marksMap = marksDao.getMarksForExam(exam.getId());

                String examLabel = exam.getExamName() + (exam.getExamYear() != null && !exam.getExamYear().isEmpty() ? " - " + exam.getExamYear() : "");
                File file = new File(classFolder, safeName(examLabel) + ".csv");

                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
                    StringBuilder header = new StringBuilder("Roll No,Student Name");
                    for (ExamSubject subject : subjects) {
                        header.append(",").append(csvEscape(subject.getSubjectName() + " (/" + subject.getTotalMarks() + ")"));
                    }
                    header.append(",Total,Percentage,Grade,Result");
                    writer.println(header);

                    for (Student s : students) {
                        Map<Long, Double> studentMarks = marksMap.getOrDefault(s.getId(), Map.of());
                        double obtained = 0;
                        int maxTotal = 0;

                        StringBuilder row = new StringBuilder(csvRow(s.getRollNo(), s.getFullName()));
                        for (ExamSubject subject : subjects) {
                            Double mark = studentMarks.get(subject.getId());
                            maxTotal += subject.getTotalMarks();
                            if (mark != null) obtained += mark;
                            row.append(",").append(mark == null ? "-" : formatNumber(mark));
                        }

                        double percentage = maxTotal == 0 ? 0 : (obtained / maxTotal) * 100.0;
                        row.append(",").append(formatNumber(obtained)).append("/").append(maxTotal);
                        row.append(",").append(String.format("%.1f%%", percentage));
                        row.append(",").append(csvEscape(gradeFromPercentage(percentage)));
                        row.append(",").append(percentage >= 33 ? "PASS" : "FAIL");

                        writer.println(row);
                    }
                }
            }
        }
    }

    private void exportDuesAndBalance(File sessionFolder) throws Exception {
        File feeFolder = new File(sessionFolder, "Dues and Balance");
        feeFolder.mkdirs();

        List<SchoolClass> classes = classDao.getAllClasses();
        for (SchoolClass c : classes) {
            List<String> months = feeDao.getDistinctMonthsForClass(c.getId());
            if (months.isEmpty()) continue;

            File classFolder = new File(feeFolder, safeName(c.getClassName() + " - " + c.getSection()));
            classFolder.mkdirs();

            // Map studentId -> Student for name/roll lookups
            Map<Long, Student> studentsById = new java.util.HashMap<>();
            for (Student s : studentDao.getStudentsByClass(c.getId())) {
                studentsById.put(s.getId(), s);
            }

            for (String month : months) {
                List<FeeRecord> records = feeDao.getRecordsForClassAndMonth(c.getId(), month);
                File file = new File(classFolder, safeName(month) + ".csv");

                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
                    writer.println(String.join(",",
                            "Roll No", "Student Name", "Fee Type", "Amount", "Status", "Paid Date", "Created Date"));

                    double totalCollected = 0;
                    double totalPending = 0;

                    for (FeeRecord r : records) {
                        Student s = studentsById.get(r.getStudentId());
                        String rollNo = s != null ? s.getRollNo() : "-";
                        String name = s != null ? s.getFullName() : "Unknown";

                        writer.println(csvRow(
                                rollNo, name, r.getFeeType(), formatNumber(r.getAmount()),
                                r.getStatus(), r.getPaidDate() == null ? "-" : r.getPaidDate(),
                                r.getCreatedDate() == null ? "-" : r.getCreatedDate()
                        ));

                        if ("PAID".equals(r.getStatus())) {
                            totalCollected += r.getAmount();
                        } else {
                            totalPending += r.getAmount();
                        }
                    }

                    writer.println();
                    writer.println("Total Collected," + formatNumber(totalCollected));
                    writer.println("Total Pending," + formatNumber(totalPending));
                }
            }
        }
    }

    private String gradeFromPercentage(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C+";
        if (pct >= 40) return "C";
        if (pct >= 33) return "D";
        return "F";
    }

    private String formatNumber(double value) {
        return (value == (int) value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private String csvRow(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(csvEscape(values[i]));
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String safeName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "-");
    }

    // If "Session 2026" already exists (e.g. backing up twice in one day), append a timestamp instead of overwriting.
    private File uniqueFolder(File root, String baseName) {
        File candidate = new File(root, baseName);
        if (!candidate.exists()) return candidate;
        String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return new File(root, baseName + " (" + stamp + ")");
    }
}