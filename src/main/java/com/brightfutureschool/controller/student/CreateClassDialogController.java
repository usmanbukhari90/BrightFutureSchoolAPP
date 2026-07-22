package com.brightfutureschool.controller.student;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.model.SchoolClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateClassDialogController {

    @FXML private TextField classNameField;
    @FXML private TextField sectionField;
    @FXML private Label statusLabel;

    private final ClassDao classDao = new ClassDao();
    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void onCreate() {
        String name = classNameField.getText() == null ? "" : classNameField.getText().trim();
        String section = sectionField.getText() == null ? "" : sectionField.getText().trim();

        if (name.isEmpty() || section.isEmpty()) {
            statusLabel.setText("Please fill in class name and section.");
            return;
        }

        try {
            SchoolClass newClass = new SchoolClass(name, section);
            classDao.createClass(newClass);
            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) classNameField.getScene().getWindow()).close();
    }
}