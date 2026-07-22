package com.brightfutureschool.controller.result;

import com.brightfutureschool.dao.local.ExamDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.ExamSubject;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CreateExamDialogController {

    @FXML private TextField examNameField;
    @FXML private TextField examYearField;
    @FXML private VBox subjectsBox;
    @FXML private Label statusLabel;

    private final ExamDao examDao = new ExamDao();
    private final StudentDao studentDao = new StudentDao();

    private SchoolClass targetClass;
    private Runnable onSuccess;

    private final List<HBox> subjectRows = new ArrayList<>();

    public void initData(SchoolClass targetClass, Runnable onSuccess) {
        this.targetClass = targetClass;
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        addSubjectRow(); // start with one blank row
    }

    @FXML
    private void onAddSubjectRow() {
        addSubjectRow();
    }

    private void addSubjectRow() {
        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject name (e.g. English)");
        subjectField.setPrefWidth(220);

        TextField marksField = new TextField();
        marksField.setPromptText("Total marks");
        marksField.setPrefWidth(100);
        marksField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                marksField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-text-fill: #E74C3C;");

        HBox row = new HBox(10, subjectField, marksField, removeBtn);
        row.setStyle("-fx-alignment: CENTER_LEFT;");

        removeBtn.setOnAction(e -> {
            subjectsBox.getChildren().remove(row);
            subjectRows.remove(row);
        });

        subjectRows.add(row);
        subjectsBox.getChildren().add(row);
    }

    @FXML
    private void onCreate() {
        String examName = safe(examNameField.getText());
        String examYear = safe(examYearField.getText());

        if (examName.isEmpty()) {
            statusLabel.setText("Please enter an exam name.");
            return;
        }

        List<ExamSubject> subjects = new ArrayList<>();
        for (HBox row : subjectRows) {
            TextField subjectField = (TextField) row.getChildren().get(0);
            TextField marksField = (TextField) row.getChildren().get(1);
            String subjectName = safe(subjectField.getText());
            String marksText = safe(marksField.getText());

            if (subjectName.isEmpty() || marksText.isEmpty()) continue;
            subjects.add(new ExamSubject(subjectName, Integer.parseInt(marksText)));
        }

        if (subjects.isEmpty()) {
            statusLabel.setText("Please add at least one subject with total marks.");
            return;
        }

        try {
            List<Student> studentsInClass = studentDao.getStudentsByClass(targetClass.getId());
            examDao.createExamWithSubjects(targetClass.getId(), examName, examYear, subjects, studentsInClass);

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

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void closeDialog() {
        ((Stage) examNameField.getScene().getWindow()).close();
    }
}