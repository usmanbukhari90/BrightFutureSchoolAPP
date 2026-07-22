package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.model.FeeAssignment;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class DeleteFeeDialogController {

    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private ComboBox<FeeAssignment> feeAssignmentDropdown;
    @FXML private Label summaryLabel;
    @FXML private Label errorLabel;

    private final ClassDao classDao = new ClassDao();
    private final FeeDao feeDao = new FeeDao();

    @FXML
    public void initialize() {
        loadClasses();
        classDropdown.setOnAction(e -> loadFeeAssignments());
        feeAssignmentDropdown.setOnAction(e -> updateSummary());
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
            showError("Failed to load classes: " + e.getMessage());
        }
    }

    private void loadFeeAssignments() {
        SchoolClass selected = classDropdown.getValue();
        if (selected == null) return;
        try {
            List<FeeAssignment> assignments = feeDao.getDistinctFeeAssignments(selected.getId());
            feeAssignmentDropdown.setItems(FXCollections.observableArrayList(assignments));
            summaryLabel.setText("");
            if (!assignments.isEmpty()) {
                feeAssignmentDropdown.getSelectionModel().selectFirst();
                updateSummary();
            }
        } catch (Exception e) {
            showError("Failed to load fees: " + e.getMessage());
        }
    }

    private void updateSummary() {
        SchoolClass selectedClass = classDropdown.getValue();
        FeeAssignment assignment = feeAssignmentDropdown.getValue();
        if (selectedClass == null || assignment == null) return;

        try {
            List<FeeRecord> records = feeDao.getRecordsForClassFeeTypeAndMonth(
                    selectedClass.getId(), assignment.getFeeType(), assignment.getMonth());

            int total = records.size();
            long paidCount = records.stream().filter(r -> "PAID".equals(r.getStatus())).count();
            double refundTotal = records.stream()
                    .filter(r -> "PAID".equals(r.getStatus()))
                    .mapToDouble(FeeRecord::getAmount)
                    .sum();

            summaryLabel.setText(total + " student(s) assigned this fee. " +
                    paidCount + " already paid — Rs. " + (int) refundTotal + " will be refunded if deleted.");
        } catch (Exception e) {
            showError("Failed to calculate summary: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        SchoolClass selectedClass = classDropdown.getValue();
        FeeAssignment assignment = feeAssignmentDropdown.getValue();
        if (selectedClass == null || assignment == null) {
            showError("Please select a class and a fee.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete \"" + assignment + "\" for all students in " + selectedClass + "?");
        confirm.setContentText("This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    double refund = feeDao.deleteFeeByTypeAndMonth(
                            selectedClass.getId(), assignment.getFeeType(), assignment.getMonth());

                    Alert done = new Alert(Alert.AlertType.INFORMATION);
                    done.setTitle("Deleted");
                    done.setContentText(refund > 0
                            ? "Fee deleted. Total refund recorded: Rs. " + (int) refund
                            : "Fee deleted. No refunds were necessary.");
                    done.showAndWait();

                    closeDialog();
                } catch (Exception e) {
                    showError("Failed to delete: " + e.getMessage());
                }
            }
        });
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
        Stage stage = (Stage) classDropdown.getScene().getWindow();
        stage.close();
    }
}