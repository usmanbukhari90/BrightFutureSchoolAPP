package com.brightfutureschool.controller.student;

import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;

public class AddStudentDialogController {

    @FXML private Label formTitleLabel;
    @FXML private Button submitButton;

    @FXML private TextField nameField;
    @FXML private TextField fatherField;
    @FXML private TextField motherField;
    @FXML private TextArea addressField;
    @FXML private TextField contactField;
    @FXML private TextField fatherContactField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextField religionField;
    @FXML private TextField nationalityField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField fatherProfessionField;
    @FXML private TextField motherProfessionField;

    @FXML private TextField bform1, bform2, bform3;
    @FXML private TextField fatherCnic1, fatherCnic2, fatherCnic3;
    @FXML private TextField motherCnic1, motherCnic2, motherCnic3;

    @FXML private ImageView photoPreview;
    @FXML private Label photoStatusLabel;
    @FXML private Label statusLabel;

    private final StudentDao studentDao = new StudentDao();
    private long classId;
    private int rollBase;
    private Runnable onSuccess;

    private boolean editMode = false;
    private long editingStudentId;
    private String editingRollNo;

    private String photoBase64;
    private static final long MAX_PHOTO_BYTES = 200 * 1024;

    // ---- Entry points ----

    public void initData(SchoolClass schoolClass, Runnable onSuccess) {
        this.classId = schoolClass.getId();
        this.rollBase = schoolClass.getRollBase();
        this.onSuccess = onSuccess;
        this.editMode = false;
    }

    public void initEditData(Student existing, Runnable onSuccess) {
        this.classId = existing.getClassId();
        this.onSuccess = onSuccess;
        this.editMode = true;
        this.editingStudentId = existing.getId();
        this.editingRollNo = existing.getRollNo();

        formTitleLabel.setText("Edit Student — Roll No: " + existing.getRollNo());
        submitButton.setText("Update Student");

        nameField.setText(existing.getFullName());
        fatherField.setText(existing.getFatherName());
        motherField.setText(existing.getMotherName());
        addressField.setText(existing.getAddress());
        contactField.setText(existing.getContact());
        fatherContactField.setText(existing.getFatherContact());
        genderComboBox.setValue(existing.getGender());
        religionField.setText(existing.getReligion());
        nationalityField.setText(existing.getNationality());
        fatherProfessionField.setText(existing.getFatherProfession());
        motherProfessionField.setText(existing.getMotherProfession());

        if (existing.getDateOfBirth() != null && !existing.getDateOfBirth().isEmpty()) {
            try {
                dobPicker.setValue(LocalDate.parse(existing.getDateOfBirth()));
            } catch (Exception ignored) { }
        }

        fillCnicParts(existing.getStudentBform(), bform1, bform2, bform3);
        fillCnicParts(existing.getFatherCnic(), fatherCnic1, fatherCnic2, fatherCnic3);
        fillCnicParts(existing.getMotherCnic(), motherCnic1, motherCnic2, motherCnic3);

        if (existing.getPhotoBase64() != null && !existing.getPhotoBase64().isEmpty()) {
            photoBase64 = existing.getPhotoBase64();
            try {
                photoPreview.setImage(new Image(photoBase64));
                photoStatusLabel.setText("Current photo loaded.");
            } catch (Exception ignored) { }
        }
    }

    private void fillCnicParts(String value, TextField p1, TextField p2, TextField p3) {
        if (value == null || value.isEmpty()) return;
        String[] parts = value.split("-");
        if (parts.length == 3) {
            p1.setText(parts[0]);
            p2.setText(parts[1]);
            p3.setText(parts[2]);
        }
    }

    // ---- Setup ----

    @FXML
    public void initialize() {
        genderComboBox.setItems(FXCollections.observableArrayList("Male", "Female", "Prefer not to say"));

        restrictToDigits(bform1, 5, bform2);
        restrictToDigits(bform2, 7, bform3);
        restrictToDigits(bform3, 1, fatherCnic1);
        restrictToDigits(fatherCnic1, 5, fatherCnic2);
        restrictToDigits(fatherCnic2, 7, fatherCnic3);
        restrictToDigits(fatherCnic3, 1, motherCnic1);
        restrictToDigits(motherCnic1, 5, motherCnic2);
        restrictToDigits(motherCnic2, 7, motherCnic3);
        restrictToDigits(motherCnic3, 1, null);

        chainEnter(nameField, fatherField);
        chainEnter(fatherField, motherField);
        chainEnter(motherField, contactField);
        chainEnter(contactField, fatherContactField);
        chainEnter(fatherContactField, genderComboBox);
        chainEnter(genderComboBox, dobPicker);
        chainEnter(dobPicker, religionField);
        chainEnter(religionField, nationalityField);
        chainEnter(nationalityField, fatherProfessionField);
        chainEnter(fatherProfessionField, motherProfessionField);
        chainEnter(motherProfessionField, addressField);
    }

    // Auto-advances focus once a box is fully filled (used for B-Form / CNIC boxes)
    private void restrictToDigits(TextField field, int maxLen, Control nextFocus) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String digitsOnly = newVal.replaceAll("[^0-9]", "");
            if (digitsOnly.length() > maxLen) {
                digitsOnly = digitsOnly.substring(0, maxLen);
            }
            if (!digitsOnly.equals(newVal)) {
                field.setText(digitsOnly);
                return; // listener re-fires with corrected text; avoid double-advance
            }
            if (digitsOnly.length() == maxLen && nextFocus != null) {
                nextFocus.requestFocus();
            }
        });
    }

    // Moves focus to the next control when Enter is pressed
    private void chainEnter(Control current, Control next) {
        current.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                next.requestFocus();
                e.consume();
            }
        });
    }

    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(photoPreview.getScene().getWindow());
        if (file == null) return;

        try {
            if (file.length() > MAX_PHOTO_BYTES) {
                photoStatusLabel.setText("Photo too large — max 200 KB allowed.");
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime = file.getName().toLowerCase().endsWith("png") ? "image/png" : "image/jpeg";
            photoBase64 = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);

            photoPreview.setImage(new Image(file.toURI().toString()));
            photoStatusLabel.setText(file.getName() + " selected.");
        } catch (Exception e) {
            photoStatusLabel.setText("Could not read photo: " + e.getMessage());
        }
    }

    @FXML
    private void onAddStudent() {
        String name = safe(nameField.getText());
        String father = safe(fatherField.getText());
        String mother = safe(motherField.getText());

        if (name.isEmpty() || father.isEmpty() || mother.isEmpty()) {
            statusLabel.setText("Name, Father Name, and Mother Name are required.");
            return;
        }

        String bform = buildCnic(bform1, bform2, bform3);
        String fatherCnic = buildCnic(fatherCnic1, fatherCnic2, fatherCnic3);
        String motherCnic = buildCnic(motherCnic1, motherCnic2, motherCnic3);

        try {
            Student student = new Student();
            student.setClassId(classId);
            student.setFullName(name);
            student.setFatherName(father);
            student.setMotherName(mother);
            student.setAddress(safe(addressField.getText()));
            student.setContact(safe(contactField.getText()));
            student.setFatherContact(safe(fatherContactField.getText()));
            student.setGender(genderComboBox.getValue());
            student.setReligion(safe(religionField.getText()));
            student.setNationality(safe(nationalityField.getText()));
            student.setPhotoBase64(photoBase64);
            student.setDateOfBirth(dobPicker.getValue() == null ? null : dobPicker.getValue().toString());
            student.setFatherProfession(safe(fatherProfessionField.getText()));
            student.setMotherProfession(safe(motherProfessionField.getText()));
            student.setStudentBform(bform);
            student.setFatherCnic(fatherCnic);
            student.setMotherCnic(motherCnic);

            if (editMode) {
                student.setId(editingStudentId);
                student.setRollNo(editingRollNo);
                studentDao.updateStudent(student);
            } else {
                student.setRollNo(studentDao.generateNextRollNo(classId, rollBase));
                studentDao.addStudent(student);
            }

            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private String buildCnic(TextField p1, TextField p2, TextField p3) {
        String a = safe(p1.getText());
        String b = safe(p2.getText());
        String c = safe(p3.getText());
        if (a.length() != 5 || b.length() != 7 || c.length() != 1) {
            return null;
        }
        return a + "-" + b + "-" + c;
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void closeDialog() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}