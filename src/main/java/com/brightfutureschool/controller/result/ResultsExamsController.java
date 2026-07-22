package com.brightfutureschool.controller.result;

import com.brightfutureschool.controller.result.CreateExamDialogController;
import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.ExamDao;
import com.brightfutureschool.dao.local.MarksDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.util.List;
import java.util.Map;

public class ResultsExamsController {

    @FXML private ComboBox<SchoolClass> classComboBox;
    @FXML private VBox examsView;
    @FXML private VBox marksView;
    @FXML private ToggleButton deleteModeToggle;
    @FXML private Label examsHintLabel;
    @FXML private FlowPane examsFlow;
    @FXML private Label marksHeaderLabel;
    @FXML private TextField searchField;
    @FXML private Button editToggleButton;
    @FXML private TableView<ResultRow> marksTable;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final ExamDao examDao = new ExamDao();
    private final MarksDao marksDao = new MarksDao();

    private SchoolClass currentClass;
    private Exam currentExam;
    private List<ExamSubject> currentSubjects;
    private ObservableList<ResultRow> allRows = FXCollections.observableArrayList();
    private boolean editMode = false;
    private boolean deleteExamMode = false;

    @FXML
    public void initialize() {
        marksTable.getSelectionModel().setCellSelectionEnabled(true);
        marksTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

        loadClassesIntoDropdown();
        classComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentClass = newVal;
                loadExams();
                showExamsView();
            }
        });
    }

    private void loadClassesIntoDropdown() {
        try {
            List<SchoolClass> classes = classDao.getAllClasses();
            classComboBox.setItems(FXCollections.observableArrayList(classes));
            if (!classes.isEmpty()) {
                classComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            showError("Failed to load classes", e);
        }
    }

    private void loadExams() {
        try {
            examsFlow.getChildren().clear();
            List<Exam> exams = examDao.getExamsByClass(currentClass.getId());
            for (Exam exam : exams) {
                examsFlow.getChildren().add(buildExamCard(exam));
            }
        } catch (Exception e) {
            showError("Failed to load exams", e);
        }
    }

    private VBox buildExamCard(Exam exam) {
        Label icon = new Label("📝");
        icon.getStyleClass().add("exam-icon-text");
        javafx.scene.layout.StackPane iconBadge = new javafx.scene.layout.StackPane(icon);
        iconBadge.getStyleClass().add("exam-icon-badge");

        Label title = new Label(exam.getExamName());
        title.getStyleClass().add("exam-title-text");
        title.setWrapText(true);

        Label year = new Label(exam.getExamYear() == null || exam.getExamYear().isEmpty() ? "" : exam.getExamYear());
        year.getStyleClass().add("exam-year-text");

        VBox textBox = new VBox(4, title, year);

        HBox topRow = new HBox(12, iconBadge, textBox);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(topRow);
        card.getStyleClass().add("exam-card");
        card.setPrefWidth(220);

        if (deleteExamMode) {
            card.setStyle(card.getStyle() + "; -fx-border-color: #E74C3C;");
            card.setOnMouseClicked(e -> confirmAndDeleteExam(exam));
        } else {
            card.setOnMouseClicked(e -> openExam(exam));
        }
        return card;
    }

    @FXML
    private void onToggleDeleteMode() {
        deleteExamMode = deleteModeToggle.isSelected();
        examsHintLabel.setText(deleteExamMode
                ? "Click an exam to delete it. This will permanently remove all subjects and marks for that exam."
                : "");
        loadExams();
    }

    private void confirmAndDeleteExam(Exam exam) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete " + exam.getExamName() + "?");
        confirm.setContentText("This will permanently delete this exam, all its subjects, and every student's marks for it. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    examDao.deleteExam(exam.getId());
                    loadExams();
                } catch (Exception e) {
                    showError("Failed to delete exam", e);
                }
            }
        });
    }




    @FXML
    private void onCreateExam() {
        if (currentClass == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result/CreateExamDialog.fxml"));
            Parent root = loader.load();
            CreateExamDialogController controller = loader.getController();
            controller.initData(currentClass, this::loadExams);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Exam");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open Create Exam dialog", e);
        }
    }

    private void openExam(Exam exam) {
        currentExam = exam;
        marksHeaderLabel.setText(exam.getExamName() + (exam.getExamYear() != null ? " (" + exam.getExamYear() + ")" : ""));
        searchField.clear();
        editMode = false;
        editToggleButton.setText("✏ Edit Results");
        try {
            loadMarksTable();
            showMarksView();
        } catch (Exception e) {
            showError("Failed to load results", e);
        }
    }

    private void loadMarksTable() throws Exception {
        currentSubjects = examDao.getSubjectsForExam(currentExam.getId());
        List<Student> students = studentDao.getStudentsByClass(currentClass.getId());
        Map<Long, Map<Long, Double>> marksMap = marksDao.getMarksForExam(currentExam.getId());

        allRows = FXCollections.observableArrayList();
        for (Student s : students) {
            ResultRow row = new ResultRow(s);
            Map<Long, Double> studentMarks = marksMap.get(s.getId());
            if (studentMarks != null) {
                for (ExamSubject subject : currentSubjects) {
                    row.setMark(subject.getId(), studentMarks.get(subject.getId()));
                }
            }
            allRows.add(row);
        }

        buildTableColumns();
        marksTable.setItems(allRows);
        marksTable.setEditable(editMode);
    }

    private void buildTableColumns() {
        marksTable.getColumns().clear();

        TableColumn<ResultRow, String> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudent().getRollNo()));
        rollCol.setPrefWidth(80);

        TableColumn<ResultRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudent().getFullName()));
        nameCol.setPrefWidth(180);

        marksTable.getColumns().add(rollCol);
        marksTable.getColumns().add(nameCol);

        for (ExamSubject subject : currentSubjects) {
            TableColumn<ResultRow, Double> subjectCol = new TableColumn<>(subject.getSubjectName() + " (/" + subject.getTotalMarks() + ")");
            subjectCol.setPrefWidth(120);

            subjectCol.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getMark(subject.getId())));

            subjectCol.setCellFactory(col -> new MarksCell(subject));

            marksTable.getColumns().add(subjectCol);
        }

        TableColumn<ResultRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cell -> {
            ResultRow row = cell.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    (int) row.getTotalObtained(currentSubjects) + "/" + row.getTotalMax(currentSubjects));
        });
        totalCol.setPrefWidth(90);

        TableColumn<ResultRow, String> percentCol = new TableColumn<>("%");
        percentCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(String.format("%.1f", cell.getValue().getPercentage(currentSubjects))));
        percentCol.setPrefWidth(60);

        TableColumn<ResultRow, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getGrade(currentSubjects)));
        gradeCol.setPrefWidth(60);

        TableColumn<ResultRow, Void> cardCol = new TableColumn<>("Result Card");
        cardCol.setPrefWidth(110);
        cardCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🖨 View");
            {
                btn.setOnAction(e -> openResultCard(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        marksTable.getColumns().add(totalCol);
        marksTable.getColumns().add(percentCol);
        marksTable.getColumns().add(gradeCol);
        marksTable.getColumns().add(cardCol);
    }

    private void openResultCard(ResultRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result/ResultCard.fxml"));
            Parent root = loader.load();
            com.brightfutureschool.controller.result.ResultCardController controller = loader.getController();
            controller.setData(currentClass, currentExam, currentSubjects, row);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Result Card — " + row.getStudent().getFullName());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open result card", e);
        }
    }

    @FXML
    private void onToggleEdit() {
        editMode = !editMode;
        marksTable.setEditable(editMode);
        editToggleButton.setText(editMode ? "🔒 Lock (Read Only)" : "✏ Edit Results");
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            marksTable.setItems(allRows);
            return;
        }
        ObservableList<ResultRow> filtered = FXCollections.observableArrayList();
        for (ResultRow row : allRows) {
            Student s = row.getStudent();
            if (s.getFullName().toLowerCase().contains(query) || s.getRollNo().contains(query)) {
                filtered.add(row);
            }
        }
        marksTable.setItems(filtered);
    }

    @FXML
    private void onBackToExams() {
        showExamsView();
    }

    private void showExamsView() {
        examsView.setVisible(true);
        examsView.setManaged(true);
        marksView.setVisible(false);
        marksView.setManaged(false);
    }

    private void showMarksView() {
        examsView.setVisible(false);
        examsView.setManaged(false);
        marksView.setVisible(true);
        marksView.setManaged(true);
    }

    // Custom editable cell for marks columns — single-click to edit, Enter/arrow keys to navigate.
    private class MarksCell extends TableCell<ResultRow, Double> {
        private final TextField textField = new TextField();
        private final ExamSubject subject;

        MarksCell(ExamSubject subject) {
            this.subject = subject;
            setAlignment(javafx.geometry.Pos.CENTER);
            textField.setAlignment(javafx.geometry.Pos.CENTER);
            textField.setStyle("-fx-padding: 2 4 2 4;");

            // Enter: commit and move down
            textField.setOnAction(e -> commitAndMove(1, 0));

            // Arrow keys: commit current value first, then move focus
            textField.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case UP -> { commitAndMove(-1, 0); e.consume(); }
                    case DOWN -> { commitAndMove(1, 0); e.consume(); }
                    case LEFT -> { if (textField.getCaretPosition() == 0) { commitAndMove(0, -1); e.consume(); } }
                    case RIGHT -> { if (textField.getCaretPosition() == textField.getText().length()) { commitAndMove(0, 1); e.consume(); } }
                    case ESCAPE -> cancelEdit();
                    default -> { }
                }
            });

            // Single click starts editing (instead of the default double-click)
            setOnMouseClicked(e -> {
                if (!isEmpty() && getTableView().isEditable()) {
                    getTableView().edit(getIndex(), getTableColumn());
                }
            });
        }

        @Override
        public void startEdit() {
            if (!isEditable() || !getTableView().isEditable()) return;
            super.startEdit();
            textField.setText(formatValue(getItem()));
            setText(null);
            setGraphic(textField);
            javafx.application.Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(formatValue(getItem()));
            setGraphic(null);
        }

        @Override
        protected void updateItem(Double value, boolean empty) {
            super.updateItem(value, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                textField.setText(formatValue(value));
                setGraphic(textField);
                setText(null);
            } else {
                setGraphic(null);
                setText(formatValue(value));
            }
        }

        private String formatValue(Double value) {
            if (value == null) return "";
            return (value == value.intValue()) ? String.valueOf(value.intValue()) : String.valueOf(value);
        }

        private Double parseValue(String text) {
            if (text == null || text.trim().isEmpty()) return null;
            try {
                return Double.parseDouble(text.trim());
            } catch (Exception e) {
                return null;
            }
        }

        private void commitAndMove(int rowDelta, int colDelta) {
            Double newValue = parseValue(textField.getText());

            if (newValue != null && newValue > subject.getTotalMarks()) {
                showAlert("Marks cannot exceed total marks (" + subject.getTotalMarks() + ") for " + subject.getSubjectName());
                textField.setText(formatValue(getItem()));
                return;
            }

            ResultRow row = getTableView().getItems().get(getIndex());
            row.setMark(subject.getId(), newValue);
            try {
                marksDao.updateMark(subject.getId(), row.getStudent().getId(), newValue);
            } catch (Exception e) {
                showError("Failed to save mark", e);
            }

            commitEdit(newValue);
            getTableView().refresh();

            moveSelection(rowDelta, colDelta);
        }

        private void moveSelection(int rowDelta, int colDelta) {
            TableView<ResultRow> table = getTableView();
            int newRow = getIndex() + rowDelta;
            int newColIndex = table.getColumns().indexOf(getTableColumn()) + colDelta;

            if (newRow < 0 || newRow >= table.getItems().size()) return;
            if (newColIndex < 0 || newColIndex >= table.getColumns().size()) return;

            TableColumn<ResultRow, ?> targetCol = table.getColumns().get(newColIndex);

            javafx.application.Platform.runLater(() -> {
                table.getSelectionModel().select(newRow, targetCol);
                table.scrollTo(newRow);
                if (table.isEditable()) {
                    table.edit(newRow, targetCol);
                }
            });
        }
    }

    private void showError(String header, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }
}