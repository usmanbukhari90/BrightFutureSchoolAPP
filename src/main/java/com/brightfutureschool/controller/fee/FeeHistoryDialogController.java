package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import com.brightfutureschool.util.MonthUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.util.List;

public class FeeHistoryDialogController {

    @FXML private Label studentNameLabel, fatherNameLabel, classLabel, rollNoLabel;
    @FXML private Label totalAssignedLabel, totalPaidLabel, totalPendingLabel, totalRefundedLabel;
    @FXML private TableView<FeeRecord> historyTable;
    @FXML private TableColumn<FeeRecord, String> colMonth;
    @FXML private TableColumn<FeeRecord, String> colFeeType;
    @FXML private TableColumn<FeeRecord, String> colAmount;
    @FXML private TableColumn<FeeRecord, String> colStatus;
    @FXML private TableColumn<FeeRecord, String> colPaidDate;

    private final FeeDao feeDao = new FeeDao();
    private final ClassDao classDao = new ClassDao();

    @FXML
    public void initialize() {
        colMonth.setCellValueFactory(d -> new SimpleStringProperty(MonthUtil.format(d.getValue().getMonth())));
        colFeeType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFeeType()));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("Rs. " + (int) d.getValue().getAmount()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    getStyleClass().removeAll("status-paid-cell", "status-pending-cell");
                } else {
                    setText(status);
                    getStyleClass().removeAll("status-paid-cell", "status-pending-cell");
                    getStyleClass().add("PAID".equals(status) ? "status-paid-cell" : "status-pending-cell");
                }
            }
        });
        colPaidDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPaidDate() != null ? d.getValue().getPaidDate() : "-"
        ));
    }

    public void loadStudent(Student student) {
        try {
            SchoolClass schoolClass = classDao.getAllClasses().stream()
                    .filter(c -> c.getId() == student.getClassId())
                    .findFirst().orElse(null);

            studentNameLabel.setText(student.getFullName());
            fatherNameLabel.setText(student.getFatherName());
            classLabel.setText(schoolClass != null ? schoolClass.toString() : "-");
            rollNoLabel.setText(student.getRollNo());

            List<FeeRecord> history = feeDao.getHistoryForStudent(student.getId());
            historyTable.setItems(FXCollections.observableArrayList(history));

            double totalAssigned = 0, totalPaid = 0;
            for (FeeRecord r : history) {
                totalAssigned += r.getAmount();
                if ("PAID".equals(r.getStatus())) totalPaid += r.getAmount();
            }
            double totalPending = totalAssigned - totalPaid;

            // Sum refunds across all months for this student
            double totalRefunded = 0;
            for (String month : history.stream().map(FeeRecord::getMonth).distinct().toList()) {
                totalRefunded += feeDao.getRefundsForStudentMonth(student.getId(), month);
            }

            totalAssignedLabel.setText("Rs. " + (int) totalAssigned);
            totalPaidLabel.setText("Rs. " + (int) totalPaid);
            totalPendingLabel.setText("Rs. " + (int) totalPending);
            totalRefundedLabel.setText("Rs. " + (int) totalRefunded);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        ((Stage) historyTable.getScene().getWindow()).close();
    }
}