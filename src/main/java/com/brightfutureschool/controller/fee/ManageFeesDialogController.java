package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageFeesDialogController {

    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private ComboBox<String> monthDropdown;
    @FXML private TableView<Row> recordsTable;
    @FXML private TableColumn<Row, String> colRollNo;
    @FXML private TableColumn<Row, String> colName;
    @FXML private TableColumn<Row, String> colFeeType;
    @FXML private TableColumn<Row, String> colAmount;
    @FXML private TableColumn<Row, String> colStatus;
    @FXML private TableColumn<Row, Void> colDelete;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final FeeDao feeDao = new FeeDao();

    public static class Row {
        FeeRecord record;
        Student student;
        Row(FeeRecord record, Student student) {
            this.record = record;
            this.student = student;
        }
    }

    @FXML
    public void initialize() {
        colRollNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().student.getRollNo()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().student.getFullName()));
        colFeeType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().record.getFeeType()));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("Rs. " + (int) d.getValue().record.getAmount()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().record.getStatus()));

        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-text-fill: #E74C3C;");
                deleteBtn.setOnAction(e -> {
                    Row row = getTableView().getItems().get(getIndex());
                    confirmAndDelete(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        loadClasses();
        classDropdown.setOnAction(e -> { loadMonths(); loadRecords(); });
        monthDropdown.setOnAction(e -> loadRecords());
    }

    private void loadClasses() {
        try {
            List<SchoolClass> classes = classDao.getAllClasses();
            classDropdown.setItems(FXCollections.observableArrayList(classes));
            if (!classes.isEmpty()) {
                classDropdown.getSelectionModel().selectFirst();
                loadMonths();
                loadRecords();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMonths() {
        SchoolClass selected = classDropdown.getValue();
        if (selected == null) return;
        try {
            List<String> months = feeDao.getDistinctMonthsForClass(selected.getId());
            String currentMonth = YearMonth.now().toString();
            if (!months.contains(currentMonth)) months.add(0, currentMonth);
            monthDropdown.setItems(FXCollections.observableArrayList(months));
            monthDropdown.getSelectionModel().select(currentMonth);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecords() {
        SchoolClass selectedClass = classDropdown.getValue();
        String selectedMonth = monthDropdown.getValue();
        if (selectedClass == null || selectedMonth == null) return;

        try {
            List<Student> students = studentDao.getStudentsByClass(selectedClass.getId());
            Map<Long, Student> studentMap = new HashMap<>();
            for (Student s : students) studentMap.put(s.getId(), s);

            List<FeeRecord> records = feeDao.getRecordsForClassAndMonth(selectedClass.getId(), selectedMonth);
            ObservableList<Row> rows = FXCollections.observableArrayList();
            for (FeeRecord r : records) {
                Student s = studentMap.get(r.getStudentId());
                if (s != null) rows.add(new Row(r, s));
            }
            recordsTable.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmAndDelete(Row row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Fee");
        confirm.setHeaderText("Delete " + row.record.getFeeType() + " (Rs. " + (int) row.record.getAmount() + ") for " + row.student.getFullName() + "?");
        confirm.setContentText("PAID".equals(row.record.getStatus())
                ? "This fee was already paid. A refund of Rs. " + (int) row.record.getAmount() + " will be recorded."
                : "This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double refund = feeDao.deleteFeeRecord(row.record.getId());
                    if (refund > 0) {
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Refund Recorded");
                        info.setContentText("Refund of Rs. " + (int) refund + " has been recorded for " + row.student.getFullName() + ".");
                        info.showAndWait();
                    }
                    loadRecords();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) recordsTable.getScene().getWindow();
        stage.close();
    }
}