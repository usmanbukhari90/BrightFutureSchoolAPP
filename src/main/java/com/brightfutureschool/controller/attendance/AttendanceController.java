package com.brightfutureschool.controller.attendance;

import com.brightfutureschool.dao.local.AttendanceDao;
import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.AttendanceRow;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.stage.Stage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttendanceController {

    @FXML private ComboBox<SchoolClass> classComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Label holidayBanner;
    @FXML private TextField searchField;
    @FXML private TableView<AttendanceRow> attendanceTable;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();

    private SchoolClass currentClass;
    private ObservableList<AttendanceRow> allRows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadClassesIntoDropdown();
        setupDatePicker();

        classComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentClass = newVal;
                refreshTable();
            }
        });

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateHolidayBanner(newVal);
            refreshTable();
        });

        if (datePicker.getValue() == null) {
            datePicker.setValue(LocalDate.now());
        }
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

    private void setupDatePicker() {
        datePicker.setDayCellFactory(new Callback<>() {
            @Override
            public javafx.scene.control.DateCell call(DatePicker picker) {
                return new javafx.scene.control.DateCell() {
                    @Override
                    public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        if (!empty && date != null && date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                            setDisable(true);
                            setStyle("-fx-background-color: #FDECEA; -fx-text-fill: #E74C3C;");
                            setTooltip(new Tooltip("Sunday — Holiday"));
                        }
                    }
                };
            }
        });
    }

    private void updateHolidayBanner(LocalDate date) {
        if (date != null && date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            holidayBanner.setText("⚠ Sunday is marked as a holiday — attendance is not recorded on this day.");
            holidayBanner.setVisible(true);
            holidayBanner.setManaged(true);
        } else {
            holidayBanner.setVisible(false);
            holidayBanner.setManaged(false);
        }
    }

    private void refreshTable() {
        if (currentClass == null || datePicker.getValue() == null) return;
        if (datePicker.getValue().getDayOfWeek() == DayOfWeek.SUNDAY) {
            attendanceTable.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            List<Student> students = studentDao.getStudentsByClass(currentClass.getId());
            String dateStr = datePicker.getValue().toString();
            Map<Long, String> existing = attendanceDao.getAttendanceForClassAndDate(currentClass.getId(), dateStr);

            allRows = FXCollections.observableArrayList();
            for (Student s : students) {
                allRows.add(new AttendanceRow(s, existing.get(s.getId())));
            }

            buildColumnsIfNeeded();
            attendanceTable.setItems(allRows);
        } catch (Exception e) {
            showError("Failed to load attendance", e);
        }
    }

    private boolean columnsBuilt = false;

    private void buildColumnsIfNeeded() {
        if (columnsBuilt) {
            attendanceTable.refresh();
            return;
        }
        columnsBuilt = true;

        TableColumn<AttendanceRow, String> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudent().getRollNo()));
        rollCol.setPrefWidth(80);

        TableColumn<AttendanceRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudent().getFullName()));
        nameCol.setPrefWidth(200);

        TableColumn<AttendanceRow, String> fatherCol = new TableColumn<>("Father Name");
        fatherCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStudent().getFatherName()));
        fatherCol.setPrefWidth(200);

        TableColumn<AttendanceRow, Void> attendanceCol = new TableColumn<>("Attendance");
        attendanceCol.setPrefWidth(140);
        attendanceCol.setCellFactory(col -> new TableCell<>() {
            private final ToggleButton presentBtn = new ToggleButton();
            private final ToggleButton absentBtn = new ToggleButton();
            private final ToggleGroup group = new ToggleGroup();
            private final HBox box = new HBox(8, presentBtn, absentBtn);

            {
                presentBtn.setGraphic(buildTick());
                absentBtn.setGraphic(buildCross());

                presentBtn.getStyleClass().addAll("attendance-toggle", "present-toggle");
                absentBtn.getStyleClass().addAll("attendance-toggle", "absent-toggle");
                presentBtn.setToggleGroup(group);
                absentBtn.setToggleGroup(group);
                box.setStyle("-fx-alignment: center;");

                presentBtn.setOnAction(e -> mark(getIndex(), "PRESENT"));
                absentBtn.setOnAction(e -> mark(getIndex(), "ABSENT"));
            }

            private javafx.scene.shape.Polyline buildTick() {
                javafx.scene.shape.Polyline tick = new javafx.scene.shape.Polyline(
                        2, 6, 6, 10, 14, 0
                );
                tick.setStroke(javafx.scene.paint.Color.WHITE);
                tick.setStrokeWidth(2.2);
                tick.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                tick.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                return tick;
            }

            private javafx.scene.Group buildCross() {
                javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(0, 0, 12, 12);
                javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(12, 0, 0, 12);
                line1.setStroke(javafx.scene.paint.Color.WHITE);
                line2.setStroke(javafx.scene.paint.Color.WHITE);
                line1.setStrokeWidth(2.2);
                line2.setStrokeWidth(2.2);
                line1.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                line2.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                return new javafx.scene.Group(line1, line2);
            }
            private void mark(int index, String status) {
                if (index < 0 || index >= getTableView().getItems().size()) return;
                AttendanceRow row = getTableView().getItems().get(index);
                row.setStatus(status);
                try {
                    attendanceDao.markAttendance(row.getStudent().getId(), currentClass.getId(), datePicker.getValue().toString(), status);
                } catch (Exception e) {
                    showError("Failed to save attendance", e);
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                AttendanceRow row = getTableView().getItems().get(getIndex());
                boolean isPresent = "PRESENT".equals(row.getStatus());
                boolean isAbsent = "ABSENT".equals(row.getStatus());

                presentBtn.setSelected(isPresent);
                absentBtn.setSelected(isAbsent);

                // Only show the white tick/cross graphic once selected — empty circle otherwise
                presentBtn.getGraphic().setVisible(isPresent);
                absentBtn.getGraphic().setVisible(isAbsent);

                setGraphic(box);
            }
        });

        TableColumn<AttendanceRow, Void> detailsCol = new TableColumn<>("Details");
        detailsCol.setPrefWidth(90);
        detailsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📋 View");
            {
                btn.setOnAction(e -> openHistory(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        attendanceTable.getColumns().add(rollCol);
        attendanceTable.getColumns().add(nameCol);
        attendanceTable.getColumns().add(fatherCol);
        attendanceTable.getColumns().add(attendanceCol);
        attendanceTable.getColumns().add(detailsCol);
        attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void openHistory(AttendanceRow row) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/attendance/AttendanceHistory.fxml"));
            javafx.scene.Parent root = loader.load();
            com.brightfutureschool.controller.attendance.AttendanceHistoryController controller = loader.getController();
            controller.setStudent(row.getStudent());

            Stage dialog = new Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("Attendance History — " + row.getStudent().getFullName());
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Could not open attendance history", e);
        }
    }

    @FXML
    private void onMarkAllPresent() {
        markAll("PRESENT");
    }

    @FXML
    private void onMarkAllAbsent() {
        markAll("ABSENT");
    }

    private void markAll(String status) {
        if (currentClass == null || datePicker.getValue() == null) return;
        if (datePicker.getValue().getDayOfWeek() == DayOfWeek.SUNDAY) {
            showAlert("Cannot mark attendance on a Sunday (holiday).");
            return;
        }
        try {
            List<Long> studentIds = new ArrayList<>();
            for (AttendanceRow row : allRows) studentIds.add(row.getStudent().getId());

            attendanceDao.markAllForClass(currentClass.getId(), datePicker.getValue().toString(), status, studentIds);

            for (AttendanceRow row : allRows) row.setStatus(status);
            attendanceTable.refresh();
        } catch (Exception e) {
            showError("Failed to mark attendance", e);
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            attendanceTable.setItems(allRows);
            return;
        }
        ObservableList<AttendanceRow> filtered = FXCollections.observableArrayList();
        for (AttendanceRow row : allRows) {
            Student s = row.getStudent();
            if (s.getFullName().toLowerCase().contains(query) || s.getRollNo().contains(query)) {
                filtered.add(row);
            }
        }
        attendanceTable.setItems(filtered);
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