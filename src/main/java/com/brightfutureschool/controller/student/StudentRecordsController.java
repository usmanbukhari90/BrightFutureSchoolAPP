package com.brightfutureschool.controller.student;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class StudentRecordsController {
    @FXML private VBox landingView;
    @FXML private VBox classesView;
    @FXML private VBox studentsView;
    @FXML private FlowPane classesFlow;
    @FXML private Label classesHintLabel;
    @FXML private Label studentsHeaderLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> rollNoColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, String> fatherNameColumn;
    @FXML private TableColumn<Student, String> contactColumn;
    @FXML private TableColumn<Student, String> addressColumn;
    @FXML private TableColumn<Student, String> bformColumn;
    @FXML private TableColumn<Student, Void> viewColumn;
    @FXML private TableColumn<Student, Void> editColumn;
    @FXML private TableColumn<Student, Void> deleteColumn;

    private boolean deleteMode = false;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private SchoolClass currentClass;

    @FXML
    public void initialize() {
        studentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rollNoColumn.setCellValueFactory(new PropertyValueFactory<>("rollNo"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fatherNameColumn.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contact"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        bformColumn.setCellValueFactory(new PropertyValueFactory<>("studentBform"));

        viewColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🔍 View");
            {
                btn.setOnAction(e -> openStudentDetail(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        editColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✏ Edit");
            {
                btn.setOnAction(e -> openEditStudent(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        deleteColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-text-fill: #E74C3C;");
                btn.setOnAction(e -> confirmAndDeleteStudent(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        showLandingView();
    }

    @FXML
    private void onLandingCreateClass() {
        onCreateClass(); // opens the modal dialog; on success we jump to the class list
    }

    @FXML
    private void onLandingAvailableClasses() {
        deleteMode = false;
        classesHintLabel.setText("Click a class to open it.");
        loadClasses();
        showClassesView();
    }

    @FXML
    private void onLandingDeleteClass() {
        deleteMode = true;
        classesHintLabel.setText("Click a class to delete it. This will also delete all students in that class.");
        loadClasses();
        showClassesView();
    }

    @FXML
    private void onBackToLanding() {
        showLandingView();
    }

    private void loadClasses() {
        try {
            classesFlow.getChildren().clear();
            List<SchoolClass> classes = classDao.getAllClasses();
            for (SchoolClass c : classes) {
                classesFlow.getChildren().add(buildClassCard(c));
            }
            showClassesView();
        } catch (Exception e) {
            showError("Failed to load classes", e);
        }
    }

    private VBox buildClassCard(SchoolClass c) {
        Label icon = new Label("🏫");
        icon.setStyle("-fx-font-size: 34px;");

        Label title = new Label(c.getClassName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2C3E50;");

        Label section = new Label("Section " + c.getSection());
        section.setStyle("-fx-font-size: 12px; -fx-text-fill: #7B5B3E; -fx-font-weight: bold;");

        int studentCount = 0;
        try {
            studentCount = studentDao.getStudentsByClass(c.getId()).size();
        } catch (Exception ignored) {}

        Label countLabel = new Label(studentCount + (studentCount == 1 ? " Student" : " Students"));
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2C3E50; -fx-opacity: 0.7;");

        VBox card = new VBox(6, icon, title, section, countLabel);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.getStyleClass().add("class-card");
        card.setPrefWidth(170);
        card.setPrefHeight(150);

        if (deleteMode) {
            card.getStyleClass().add("class-card-delete");
            card.setOnMouseClicked(e -> confirmAndDeleteClass(c));
        } else {
            card.setOnMouseClicked(e -> openClass(c));
        }

        // Subtle grow-on-hover animation
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private void confirmAndDeleteClass(SchoolClass c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete " + c.getClassName() + " - " + c.getSection() + "?");
        confirm.setContentText("This will permanently delete this class and all its students. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    classDao.deleteClass(c.getId());
                    loadClasses();
                } catch (Exception e) {
                    showError("Failed to delete class", e);
                }
            }
        });
    }
    private void openClass(SchoolClass c) {
        currentClass = c;
        studentsHeaderLabel.setText(c.getClassName() + " - " + c.getSection());
        searchField.clear();
        loadStudents();
        showStudentsView();
    }

    private void loadStudents() {
        try {
            List<Student> students = studentDao.getStudentsByClass(currentClass.getId());
            studentsTable.setItems(FXCollections.observableArrayList(students));
        } catch (Exception e) {
            showError("Failed to load students", e);
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        try {
            List<Student> results = query.isEmpty()
                    ? studentDao.getStudentsByClass(currentClass.getId())
                    : studentDao.searchInClass(currentClass.getId(), query);
            studentsTable.setItems(FXCollections.observableArrayList(results));
        } catch (Exception e) {
            showError("Search failed", e);
        }
    }

    private void confirmAndDeleteStudent(Student s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete " + s.getFullName() + " (Roll No: " + s.getRollNo() + ")?");
        confirm.setContentText("This will permanently delete this student's record. This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    studentDao.deleteStudent(s.getId());
                    loadStudents();
                } catch (Exception e) {
                    showError("Failed to delete student", e);
                }
            }
        });
    }

    @FXML
    private void onBackToClasses() {
        showClassesView();
    }

    @FXML
    private void onCreateClass() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/CreateClassDialog.fxml"));
            Parent root = loader.load();
            CreateClassDialogController controller = loader.getController();
            controller.setOnSuccess(() -> {
                loadClasses();
                showClassesView(); // after creating, show the updated class list
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Create Class");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open Create Class dialog", e);
        }
    }

    @FXML
    private void onAddStudent() {
        if (currentClass == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/AddStudentDialog.fxml"));
            Parent root = loader.load();

            AddStudentDialogController controller = loader.getController();
            controller.initData(currentClass, this::loadStudents);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add Student");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open Add Student dialog", e);
        }
    }
    private void openEditStudent(Student s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/AddStudentDialog.fxml"));
            Parent root = loader.load();
            AddStudentDialogController controller = loader.getController();
            controller.initEditData(s, this::loadStudents);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit Student");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open edit form", e);
        }
    }
    private void openStudentDetail(Student s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student/StudentDetail.fxml"));
            Parent root = loader.load();
            StudentDetailController controller = loader.getController();
            controller.setStudent(s);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Student Record");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open student record", e);
        }
    }

    private void showLandingView() {
        landingView.setVisible(true);
        landingView.setManaged(true);
        classesView.setVisible(false);
        classesView.setManaged(false);
        studentsView.setVisible(false);
        studentsView.setManaged(false);
    }

    private void showClassesView() {
        landingView.setVisible(false);
        landingView.setManaged(false);
        classesView.setVisible(true);
        classesView.setManaged(true);
        studentsView.setVisible(false);
        studentsView.setManaged(false);
    }

    private void showStudentsView() {
        landingView.setVisible(false);
        landingView.setManaged(false);
        classesView.setVisible(false);
        classesView.setManaged(false);
        studentsView.setVisible(true);
        studentsView.setManaged(true);
    }

    private void showError(String header, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}