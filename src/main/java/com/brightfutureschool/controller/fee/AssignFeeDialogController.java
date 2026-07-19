package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.time.YearMonth;
import java.util.List;

public class AssignFeeDialogController {

    @FXML private ComboBox<String> feeTypeDropdown;
    @FXML private TextField amountField;
    @FXML private RadioButton wholeClassRadio;
    @FXML private RadioButton singleStudentRadio;
    @FXML private TableView<StudentAmountRow> studentAmountTable;
    @FXML private TableColumn<StudentAmountRow, String> colStuRollNo;
    @FXML private TableColumn<StudentAmountRow, String> colStuName;
    @FXML private TableColumn<StudentAmountRow, String> colStuAmount;
    @FXML private Label errorLabel;

    private final StudentDao studentDao = new StudentDao();
    private final FeeDao feeDao = new FeeDao();

    private long classId;
    private Long selectedStudentId; // null if none pre-selected in Fee table
    private boolean assigned = false;

    private ObservableList<StudentAmountRow> allRows = FXCollections.observableArrayList();

    // Row wrapper for the editable table
    public static class StudentAmountRow {
        Student student;
        SimpleStringProperty amount;

        StudentAmountRow(Student student, String defaultAmount) {
            this.student = student;
            this.amount = new SimpleStringProperty(defaultAmount);
        }
        public String getRollNo() { return student.getRollNo(); }
        public String getName() { return student.getFullName(); }
        public SimpleStringProperty amountProperty() { return amount; }
    }

    @FXML
    public void initialize() {
        feeTypeDropdown.setItems(FXCollections.observableArrayList(
                "Admission Fee", "Tuition Fee", "Arrears / Balance", "Paper Money"
        ));

        colStuRollNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRollNo()));
        colStuName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colStuAmount.setCellValueFactory(d -> d.getValue().amountProperty());
        colStuAmount.setCellFactory(TextFieldTableCell.forTableColumn());
        colStuAmount.setOnEditCommit(e -> e.getRowValue().amount.set(e.getNewValue()));
        studentAmountTable.setEditable(true);
    }

    public void setContext(long classId, Long selectedStudentId) {
        this.classId = classId;
        this.selectedStudentId = selectedStudentId;
        singleStudentRadio.setDisable(selectedStudentId == null);

        try {
            List<Student> students = studentDao.getStudentsByClass(classId);
            allRows.clear();
            for (Student s : students) {
                allRows.add(new StudentAmountRow(s, ""));
            }
            refreshTableForMode();
        } catch (Exception e) {
            showError("Failed to load students: " + e.getMessage());
        }
    }

    @FXML
    private void onModeChanged() {
        refreshTableForMode();
    }

    private void refreshTableForMode() {
        if (singleStudentRadio.isSelected() && selectedStudentId != null) {
            ObservableList<StudentAmountRow> single = FXCollections.observableArrayList();
            for (StudentAmountRow row : allRows) {
                if (row.student.getId() == selectedStudentId) single.add(row);
            }
            studentAmountTable.setItems(single);
        } else {
            studentAmountTable.setItems(allRows);
        }
    }

    @FXML
    private void onApplyDefaultToAll() {
        String defaultAmount = amountField.getText().trim();
        if (defaultAmount.isEmpty()) {
            showError("Enter a default amount first.");
            return;
        }
        for (StudentAmountRow row : studentAmountTable.getItems()) {
            row.amount.set(defaultAmount);
        }
        studentAmountTable.refresh();
    }

    @FXML
    private void onAssign() {
        String feeType = feeTypeDropdown.getEditor().getText().trim();
        if (feeType.isEmpty()) {
            showError("Please enter a fee type.");
            return;
        }

        String currentMonth = YearMonth.now().toString();
        List<StudentAmountRow> rows = studentAmountTable.getItems();

        if (rows.isEmpty()) {
            showError("No students to assign.");
            return;
        }

        try {
            for (StudentAmountRow row : rows) {
                String amtText = row.amount.get() == null ? "" : row.amount.get().trim();
                if (amtText.isEmpty()) continue; // skip students left blank (e.g. already exempted)
                double amount;
                try {
                    amount = Double.parseDouble(amtText);
                } catch (NumberFormatException nfe) {
                    showError("Invalid amount for " + row.getName() + ".");
                    return;
                }
                if (amount < 0) {
                    showError("Amount cannot be negative for " + row.getName() + ".");
                    return;
                }
                feeDao.assignFeeToStudent(row.student.getId(), feeType, amount, currentMonth);
            }
            assigned = true;
            closeDialog();
        } catch (Exception e) {
            showError("Failed to assign fee: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        assigned = false;
        closeDialog();
    }

    public boolean wasAssigned() {
        return assigned;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}