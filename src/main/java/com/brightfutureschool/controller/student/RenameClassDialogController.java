package com.brightfutureschool.controller.student;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.model.SchoolClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RenameClassDialogController {

    @FXML private Label currentLabel;
    @FXML private TextField classNameField;
    @FXML private TextField sectionField;
    @FXML private Label statusLabel;

    private final ClassDao classDao = new ClassDao();
    private SchoolClass schoolClass;
    private Runnable onSuccess;

    public void initData(SchoolClass schoolClass, Runnable onSuccess) {
        this.schoolClass = schoolClass;
        this.onSuccess = onSuccess;
        currentLabel.setText("Current: " + schoolClass.getClassName() + " - " + schoolClass.getSection());
        classNameField.setText(schoolClass.getClassName());
        sectionField.setText(schoolClass.getSection());
    }

    @FXML
    private void onSave() {
        String name = classNameField.getText() == null ? "" : classNameField.getText().trim();
        String section = sectionField.getText() == null ? "" : sectionField.getText().trim();

        if (name.isEmpty() || section.isEmpty()) {
            statusLabel.setText("Please fill in class name and section.");
            return;
        }

        // Nothing changed: just close
        if (name.equals(schoolClass.getClassName()) && section.equals(schoolClass.getSection())) {
            closeDialog();
            return;
        }

        try {
            for (SchoolClass other : classDao.getAllClasses()) {
                if (other.getId() != schoolClass.getId()
                        && other.getClassName().equalsIgnoreCase(name)
                        && other.getSection().equalsIgnoreCase(section)) {
                    statusLabel.setText("A class \"" + name + " - " + section + "\" already exists.");
                    return;
                }
            }
            classDao.updateClass(schoolClass.getId(), name, section);
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
