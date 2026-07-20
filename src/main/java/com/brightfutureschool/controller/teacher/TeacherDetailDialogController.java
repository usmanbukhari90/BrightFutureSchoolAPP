package com.brightfutureschool.controller.teacher;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.TeacherDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Teacher;
import com.brightfutureschool.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class TeacherDetailDialogController {

    @FXML private ImageView photoView;
    @FXML private Label srNoLabel;
    @FXML private TextField fullNameField, fatherNameField, contactField, dobField, cnicField;
    @FXML private TextArea qualificationField, experienceField;
    @FXML private TextField nationalityField, religionField, subjectField, salaryField;
    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private Label errorLabel;

    private final TeacherDao teacherDao = new TeacherDao();
    private final ClassDao classDao = new ClassDao();

    private Teacher editingTeacher; // null if adding new
    private Runnable onSuccess;
    private String pendingPhotoBase64;

    @FXML
    public void initialize() {
        try {
            List<SchoolClass> classes = classDao.getAllClasses();
            classDropdown.setItems(FXCollections.observableArrayList(classes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initAddData(Runnable onSuccess) {
        this.onSuccess = onSuccess;
        this.editingTeacher = null;
        srNoLabel.setText("(assigned automatically)");
    }

    public void initEditData(Teacher t, Runnable onSuccess) {
        this.onSuccess = onSuccess;
        this.editingTeacher = t;

        srNoLabel.setText(String.valueOf(t.getSrNo()));
        fullNameField.setText(t.getFullName());
        fatherNameField.setText(t.getFatherName());
        contactField.setText(t.getContact());
        dobField.setText(t.getDateOfBirth());
        cnicField.setText(t.getCnic());
        qualificationField.setText(t.getQualification());
        experienceField.setText(t.getPreviousExperience());
        nationalityField.setText(t.getNationality());
        religionField.setText(t.getReligion());
        subjectField.setText(t.getSubject());
        salaryField.setText(t.getSalary() > 0 ? String.valueOf((int) t.getSalary()) : "");

        pendingPhotoBase64 = t.getPhotoBase64();
        Image img = ImageUtil.decodeFromBase64(t.getPhotoBase64());
        if (img != null) photoView.setImage(img);

        if (t.getAssignedClassId() != null) {
            classDropdown.getItems().stream()
                    .filter(c -> c.getId() == t.getAssignedClassId())
                    .findFirst()
                    .ifPresent(c -> classDropdown.getSelectionModel().select(c));
        }
    }

    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Teacher Photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(photoView.getScene().getWindow());
        if (file == null) return;

        Image image = new Image(file.toURI().toString());
        photoView.setImage(image);
        pendingPhotoBase64 = ImageUtil.encodeToBase64(image);
    }

    @FXML
    private void onSave() {
        String name = fullNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Full name is required.");
            return;
        }

        double salary = 0;
        String salaryText = salaryField.getText().trim();
        if (!salaryText.isEmpty()) {
            try {
                salary = Double.parseDouble(salaryText);
            } catch (NumberFormatException e) {
                showError("Salary must be a valid number.");
                return;
            }
        }

        try {
            Teacher t = editingTeacher != null ? editingTeacher : new Teacher();
            t.setFullName(name);
            t.setFatherName(fatherNameField.getText().trim());
            t.setContact(contactField.getText().trim());
            t.setDateOfBirth(dobField.getText().trim());
            t.setCnic(cnicField.getText().trim());
            t.setQualification(qualificationField.getText().trim());
            t.setPreviousExperience(experienceField.getText().trim());
            t.setNationality(nationalityField.getText().trim());
            t.setReligion(religionField.getText().trim());
            t.setPhotoBase64(pendingPhotoBase64);
            t.setSubject(subjectField.getText().trim());
            t.setSalary(salary);

            SchoolClass selectedClass = classDropdown.getValue();
            t.setAssignedClassId(selectedClass != null ? selectedClass.getId() : null);

            if (editingTeacher != null) {
                teacherDao.updateTeacher(t);
            } else {
                teacherDao.addTeacher(t);
            }

            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (Exception e) {
            showError("Failed to save: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeDialog() {
        ((Stage) fullNameField.getScene().getWindow()).close();
    }
}