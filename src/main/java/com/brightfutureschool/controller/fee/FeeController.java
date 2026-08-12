package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.FeeAssignment;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeeController {

    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private ComboBox<FeeAssignment> feeAssignmentDropdown;
    @FXML private TextField searchField;
    @FXML private TableView<Row> feeTable;
    @FXML private TableColumn<Row, String> colRollNo;
    @FXML private TableColumn<Row, String> colName;
    @FXML private TableColumn<Row, String> colAmount;
    @FXML private TableColumn<Row, String> colStatus;
    @FXML private TableColumn<Row, Void> colMarkPaid;
    @FXML private TableColumn<Row, Void> colPrint;
    @FXML private TableColumn<Row, Void> colDetails;

    private final StudentDao studentDao = new StudentDao();
    private final FeeDao feeDao = new FeeDao();
    private final ClassDao classDao = new ClassDao();

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
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("Rs. " + (int) d.getValue().record.getAmount()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().record.getStatus()));

        colMarkPaid.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Mark Paid");
            {
                btn.setOnAction(e -> markPaid(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Row row = getTableView().getItems().get(getIndex());
                btn.setDisable("PAID".equals(row.record.getStatus()));
                btn.setText("PARTIAL".equals(row.record.getStatus()) ? "Pay Remaining" : "Mark Paid");
                setGraphic(btn);
            }
        });
        colPrint.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Print/Save");
            {
                btn.setOnAction(e -> onPrintReceipt(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Row row = getTableView().getItems().get(getIndex());
                btn.setDisable(row.record.getPaidAmount() <= 0);
                setGraphic(btn);
            }
        });

        colDetails.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Details");
            {
                btn.setOnAction(e -> onViewDetails(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        loadClasses();
        classDropdown.setOnAction(e -> { loadFeeAssignments(); });
        feeAssignmentDropdown.setOnAction(e -> loadFeeTable());
    }

    private void loadClasses() {
        try {
            List<SchoolClass> classes = classDao.getAllClasses();
            classDropdown.setItems(FXCollections.observableArrayList(classes));
            if (!classes.isEmpty()) {
                classDropdown.getSelectionModel().selectFirst();
                loadFeeAssignments();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFeeAssignments() {
        SchoolClass selected = classDropdown.getValue();
        if (selected == null) return;
        try {
            List<FeeAssignment> assignments = feeDao.getDistinctFeeAssignments(selected.getId());
            feeAssignmentDropdown.setItems(FXCollections.observableArrayList(assignments));
            if (!assignments.isEmpty()) {
                feeAssignmentDropdown.getSelectionModel().selectFirst();
                loadFeeTable();
            } else {
                feeTable.getItems().clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFeeTable() {
        SchoolClass selectedClass = classDropdown.getValue();
        FeeAssignment assignment = feeAssignmentDropdown.getValue();
        if (selectedClass == null || assignment == null) return;

        try {
            List<Student> students = studentDao.getStudentsByClass(selectedClass.getId());
            Map<Long, Student> studentMap = new HashMap<>();
            for (Student s : students) studentMap.put(s.getId(), s);

            List<FeeRecord> records = feeDao.getRecordsForClassFeeTypeAndMonth(
                    selectedClass.getId(), assignment.getFeeType(), assignment.getMonth());

            ObservableList<Row> rows = FXCollections.observableArrayList();
            for (FeeRecord r : records) {
                Student s = studentMap.get(r.getStudentId());
                if (s != null) rows.add(new Row(r, s));
            }
            feeTable.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markPaid(Row row) {
        try {
            double currentRemaining = row.record.getAmount() - row.record.getPaidAmount();
            double arrearsBefore = feeDao.getOutstandingBalanceExcluding(row.student.getId(), row.record.getId());
            double totalOwed = currentRemaining + arrearsBefore;

            TextInputDialog dialog = new TextInputDialog(String.valueOf((int) totalOwed));
            dialog.setTitle("Record Payment");
            String arrearsNote = arrearsBefore > 0 ? " (includes Rs. " + (int) arrearsBefore + " in prior dues)" : "";
            dialog.setHeaderText(row.student.getFullName() + " — Total Owed: Rs. " + (int) totalOwed + arrearsNote);
            dialog.setContentText("Amount Paid (Rs.):");

            dialog.showAndWait().ifPresent(input -> {
                try {
                    double amount = Double.parseDouble(input.trim());
                    if (amount <= 0) {
                        new Alert(Alert.AlertType.WARNING, "Enter a valid positive amount.").showAndWait();
                        return;
                    }
                    if (amount > totalOwed) amount = totalOwed;

                    double applied = feeDao.recordPaymentForRecordWithArrears(row.student.getId(), row.record.getId(), amount);
                    loadFeeTable();

                    FeeRecord updated = feeDao.getFeeRecordById(row.record.getId());
                    openReceipt(row.student, updated, arrearsBefore, applied);
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.WARNING, "Please enter a valid number.").showAndWait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openReceipt(Student student, FeeRecord record, double arrearsBeforePayment, double paidThisTransaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fee/Receipt.fxml"));
            Parent root = loader.load();
            ReceiptController controller = loader.getController();
            controller.loadReceiptForPayment(student, record, arrearsBeforePayment, paidThisTransaction);

            ScrollPane scrollPane = new ScrollPane(root);
            scrollPane.setFitToWidth(true);

            Stage stage = new Stage();
            stage.setTitle("Fee Receipt - " + student.getFullName());
            stage.setScene(new Scene(scrollPane, 650, 750));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { loadFeeTable(); return; }
        SchoolClass selectedClass = classDropdown.getValue();
        FeeAssignment assignment = feeAssignmentDropdown.getValue();
        if (selectedClass == null || assignment == null) return;

        try {
            List<Student> matches = studentDao.searchInClass(selectedClass.getId(), query);
            List<FeeRecord> records = feeDao.getRecordsForClassFeeTypeAndMonth(
                    selectedClass.getId(), assignment.getFeeType(), assignment.getMonth());
            Map<Long, FeeRecord> recordByStudent = new HashMap<>();
            for (FeeRecord r : records) recordByStudent.put(r.getStudentId(), r);

            ObservableList<Row> rows = FXCollections.observableArrayList();
            for (Student s : matches) {
                FeeRecord r = recordByStudent.get(s.getId());
                if (r != null) rows.add(new Row(r, s));
            }
            feeTable.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAssignFee() {
        SchoolClass selected = classDropdown.getValue();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fee/AssignFeeDialog.fxml"));
            Parent root = loader.load();
            AssignFeeDialogController controller = loader.getController();

            Long selectedStudentId = null;
            Row selectedRow = feeTable.getSelectionModel().getSelectedItem();
            if (selectedRow != null) selectedStudentId = selectedRow.student.getId();

            controller.setContext(selected.getId(), selectedStudentId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Assign Fee");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.wasAssigned()) {
                loadFeeAssignments();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDeleteFee() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fee/DeleteFeeDialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Delete Fee");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadFeeAssignments();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onPrintReceipt(Row row) {
        if (row.record.getPaidAmount() <= 0) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fee/Receipt.fxml"));
            Parent root = loader.load();
            ReceiptController controller = loader.getController();
            controller.loadReceipt(row.student, row.record);

            ScrollPane scrollPane = new ScrollPane(root);
            scrollPane.setFitToWidth(true);

            Stage stage = new Stage();
            stage.setTitle("Fee Receipt - " + row.student.getFullName());
            stage.setScene(new Scene(scrollPane, 650, 750));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onViewDetails(Row row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/fee/FeeHistoryDialog.fxml"));
            Parent root = loader.load();
            FeeHistoryDialogController controller = loader.getController();
            controller.loadStudent(row.student);

            Stage stage = new Stage();
            stage.setTitle("Fee History - " + row.student.getFullName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}