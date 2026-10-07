package com.brightfutureschool.controller.student;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportClassDialogController {

    // The header a valid Bright Future School Student Records export must have.
    // Order does not matter on read; every value is looked up by name.
    private static final String[] REQUIRED_COLUMNS = {
            "Roll No", "Full Name", "Father Name", "Mother Name", "Gender", "Date of Birth",
            "Religion", "Nationality", "Contact", "Father Contact", "Address",
            "Father Profession", "Mother Profession", "Student B-Form", "Father CNIC", "Mother CNIC"
    };

    @FXML private Label fileLabel;
    @FXML private TextField classNameField;
    @FXML private TextField sectionField;
    @FXML private Label summaryLabel;
    @FXML private Label skippedLabel;
    @FXML private Label statusLabel;
    @FXML private Button importButton;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();

    private File file;
    private List<Map<String, String>> validRows;
    private Runnable onSuccess;

    public void initData(File file, Runnable onSuccess) {
        this.file = file;
        this.onSuccess = onSuccess;
        fileLabel.setText("File: " + file.getName());
        loadAndPreview();
    }

    private void loadAndPreview() {
        importButton.setDisable(true);
        validRows = new ArrayList<>();
        List<String> skippedLines = new ArrayList<>();

        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            statusLabel.setText("Could not read this file: " + e.getMessage());
            return;
        }
        if (lines.isEmpty()) {
            statusLabel.setText("This file is empty.");
            return;
        }

        String[] header = splitCsvLine(lines.get(0));
        for (int i = 0; i < header.length; i++) header[i] = header[i].trim();

        for (String required : REQUIRED_COLUMNS) {
            boolean found = false;
            for (String h : header) {
                if (h.equalsIgnoreCase(required)) { found = true; break; }
            }
            if (!found) {
                statusLabel.setText("This doesn't look like a Bright Future School Student Records export. "
                        + "Missing column: \"" + required + "\".");
                return;
            }
        }

        for (int lineNo = 2; lineNo <= lines.size(); lineNo++) {
            String raw = lines.get(lineNo - 1);
            if (raw == null || raw.trim().isEmpty()) continue;

            String[] cols = splitCsvLine(raw);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length && i < cols.length; i++) {
                row.put(header[i].toLowerCase(), cols[i].trim());
            }

            String fullName = row.getOrDefault("full name", "");
            String fatherName = row.getOrDefault("father name", "");
            if (fullName.isEmpty() || fatherName.isEmpty()) {
                skippedLines.add(String.valueOf(lineNo));
                continue;
            }
            validRows.add(row);
        }

        // Guess the class name and section from the file name, e.g. "Play Group - A.csv"
        String baseName = file.getName().replaceFirst("(?i)\\.csv$", "");
        String guessedName = baseName;
        String guessedSection = "";
        int dash = baseName.lastIndexOf(" - ");
        if (dash > 0) {
            guessedName = baseName.substring(0, dash).trim();
            guessedSection = baseName.substring(dash + 3).trim();
        }
        classNameField.setText(guessedName);
        sectionField.setText(guessedSection);

        summaryLabel.setText(validRows.size() + " student(s) found in this file.");
        if (!skippedLines.isEmpty()) {
            skippedLabel.setText(skippedLines.size() + " row(s) skipped (missing Full Name or Father Name): line "
                    + String.join(", ", skippedLines));
        } else {
            skippedLabel.setText("");
        }

        if (validRows.isEmpty()) {
            statusLabel.setText("No usable student rows were found in this file.");
        } else {
            importButton.setDisable(false);
        }
    }

    // Splits one CSV line the proper way: a comma inside "quotes" (e.g. an Address with a comma in it)
    // does not end the field, and "" inside a quoted field means a literal quote character.
    // Plain unquoted fields still split on comma exactly as before, so ordinary rows are unaffected.
    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip the second quote of an escaped ""
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    @FXML
    private void onImport() {
        String name = classNameField.getText() == null ? "" : classNameField.getText().trim();
        String section = sectionField.getText() == null ? "" : sectionField.getText().trim();

        if (name.isEmpty() || section.isEmpty()) {
            statusLabel.setText("Please fill in class name and section.");
            return;
        }

        try {
            for (SchoolClass other : classDao.getAllClasses()) {
                if (other.getClassName().equalsIgnoreCase(name) && other.getSection().equalsIgnoreCase(section)) {
                    statusLabel.setText("A class \"" + name + " - " + section
                            + "\" already exists. Rename or delete it first, then try again.");
                    return;
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
            return;
        }

        SchoolClass newClass;
        try {
            newClass = classDao.createClass(new SchoolClass(name, section));
        } catch (Exception e) {
            statusLabel.setText("Could not create the class: " + e.getMessage());
            return;
        }

        // Everything from here happens against the brand-new class only. If anything fails,
        // deleting the class removes every student already inserted (ON DELETE CASCADE),
        // so a failed import can never leave a half-built class behind.
        try {
            for (Map<String, String> row : validRows) {
                Student s = new Student();
                s.setClassId(newClass.getId());
                s.setRollNo(studentDao.generateNextRollNo(newClass.getId(), newClass.getRollBase()));
                s.setFullName(row.getOrDefault("full name", ""));
                s.setFatherName(row.getOrDefault("father name", ""));
                s.setMotherName(row.getOrDefault("mother name", ""));
                s.setGender(row.getOrDefault("gender", ""));
                s.setDateOfBirth(row.getOrDefault("date of birth", ""));
                s.setReligion(row.getOrDefault("religion", ""));
                s.setNationality(row.getOrDefault("nationality", ""));
                s.setContact(row.getOrDefault("contact", ""));
                s.setFatherContact(row.getOrDefault("father contact", ""));
                s.setAddress(row.getOrDefault("address", ""));
                s.setFatherProfession(row.getOrDefault("father profession", ""));
                s.setMotherProfession(row.getOrDefault("mother profession", ""));
                s.setStudentBform(row.getOrDefault("student b-form", ""));
                s.setFatherCnic(row.getOrDefault("father cnic", ""));
                s.setMotherCnic(row.getOrDefault("mother cnic", ""));
                studentDao.addStudent(s);
            }
        } catch (Exception e) {
            try {
                classDao.deleteClass(newClass.getId());
            } catch (Exception cleanupError) {
                cleanupError.printStackTrace();
            }
            statusLabel.setText("Import failed partway through, so nothing was kept: " + e.getMessage());
            return;
        }

        if (onSuccess != null) onSuccess.run();
        closeDialog();
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) classNameField.getScene().getWindow()).close();
    }
}