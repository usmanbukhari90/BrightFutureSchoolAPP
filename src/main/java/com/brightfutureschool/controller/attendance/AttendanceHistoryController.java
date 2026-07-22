package com.brightfutureschool.controller.attendance;

import com.brightfutureschool.dao.local.AttendanceDao;
import com.brightfutureschool.model.AttendanceRecord;
import com.brightfutureschool.model.Student;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryController {

    @FXML private Label studentNameLabel;
    @FXML private Label rollNoLabel;
    @FXML private Label fatherNameLabel;
    @FXML private Label summaryLabel;
    @FXML private TableView<AttendanceRecord> historyTable;
    @FXML private TableColumn<AttendanceRecord, String> dateColumn;
    @FXML private TableColumn<AttendanceRecord, String> dayColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;

    private final AttendanceDao attendanceDao = new AttendanceDao();

    public void setStudent(Student student) {
        studentNameLabel.setText(student.getFullName());
        rollNoLabel.setText("Roll No: " + student.getRollNo());
        fatherNameLabel.setText("Father Name: " + student.getFatherName());

        try {
            List<AttendanceRecord> history = attendanceDao.getHistoryForStudent(student.getId());

            dateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getAttendanceDate()));
            dayColumn.setCellValueFactory(cell -> {
                LocalDate date = LocalDate.parse(cell.getValue().getAttendanceDate());
                return new javafx.beans.property.SimpleStringProperty(
                        date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            });
            statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        setStyle("PRESENT".equals(status)
                                ? "-fx-text-fill: #4CAF50; -fx-font-weight: bold;"
                                : "-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    }
                }
            });

            historyTable.setItems(FXCollections.observableArrayList(history));

            long presentCount = history.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
            long absentCount = history.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
            summaryLabel.setText("Total Present: " + presentCount + "   |   Total Absent: " + absentCount + "   |   Total Days Marked: " + history.size());

        } catch (Exception e) {
            e.printStackTrace();
            summaryLabel.setText("Failed to load attendance history: " + e.getMessage());
        }
    }

    @FXML
    private void onClose() {
        ((Stage) studentNameLabel.getScene().getWindow()).close();
    }
}