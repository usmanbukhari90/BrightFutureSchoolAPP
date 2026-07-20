package com.brightfutureschool.controller.contact;

import com.brightfutureschool.dao.local.AdminSenderDao;
import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ContactController {

    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private Label senderLabel;
    @FXML private TextArea messageArea;
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, Boolean> colSelect;
    @FXML private TableColumn<StudentRow, String> colRollNo;
    @FXML private TableColumn<StudentRow, String> colName;
    @FXML private TableColumn<StudentRow, String> colContact;
    @FXML private TableColumn<StudentRow, Void> colAction;

    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final AdminSenderDao senderDao = new AdminSenderDao();

    public static class StudentRow {
        Student student;
        SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        StudentRow(Student student) { this.student = student; }
    }

    @FXML
    public void initialize() {
        colSelect.setCellValueFactory(d -> d.getValue().selected);
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        studentsTable.setEditable(true);

        colRollNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().student.getRollNo()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().student.getFullName()));
        colContact.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().student.getContact() == null || d.getValue().student.getContact().isEmpty()
                        ? "Not provided" : d.getValue().student.getContact()
        ));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Send WhatsApp");
            {
                btn.setOnAction(e -> {
                    StudentRow row = getTableView().getItems().get(getIndex());
                    sendToStudent(row.student);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                StudentRow row = getTableView().getItems().get(getIndex());
                boolean hasContact = row.student.getContact() != null && !row.student.getContact().isEmpty();
                btn.setDisable(!hasContact);
                setGraphic(btn);
            }
        });

        loadClasses();
        loadSenderLabel();
        classDropdown.setOnAction(e -> loadStudents());
    }

    private void loadClasses() {
        try {
            List<SchoolClass> classes = classDao.getAllClasses();
            classDropdown.setItems(FXCollections.observableArrayList(classes));
            if (!classes.isEmpty()) {
                classDropdown.getSelectionModel().selectFirst();
                loadStudents();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSenderLabel() {
        try {
            List<String> numbers = senderDao.getAllNumbers();
            senderLabel.setText(numbers.isEmpty()
                    ? "No sender number saved"
                    : "Sending as: " + numbers.get(0) + (numbers.size() > 1 ? " (+" + (numbers.size() - 1) + " more)" : ""));
        } catch (Exception e) {
            senderLabel.setText("");
        }
    }

    private void loadStudents() {
        SchoolClass selected = classDropdown.getValue();
        if (selected == null) return;
        try {
            List<Student> students = studentDao.getStudentsByClass(selected.getId());
            ObservableList<StudentRow> rows = FXCollections.observableArrayList();
            for (Student s : students) rows.add(new StudentRow(s));
            studentsTable.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onManageSenders() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/contact/ManageSendersDialog.fxml"));
            Parent root = loader.load();
            ManageSendersDialogController controller = loader.getController();
            controller.setOnChange(this::loadSenderLabel);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Manage My WhatsApp Numbers");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSendToSelected() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert("Please type a message first.");
            return;
        }
        List<StudentRow> selectedRows = studentsTable.getItems().stream()
                .filter(r -> r.selected.get())
                .toList();
        if (selectedRows.isEmpty()) {
            showAlert("Please select at least one student (checkbox).");
            return;
        }
        sendToMultiple(selectedRows.stream().map(r -> r.student).toList(), message);
    }

    @FXML
    private void onSendToClass() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert("Please type a message first.");
            return;
        }
        List<Student> all = studentsTable.getItems().stream().map(r -> r.student).toList();
        sendToMultiple(all, message);
    }

    private void sendToMultiple(List<Student> students, String message) {
        List<Student> withContact = students.stream()
                .filter(s -> s.getContact() != null && !s.getContact().isEmpty())
                .toList();

        if (withContact.isEmpty()) {
            showAlert("None of the selected students have a WhatsApp contact saved.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Send WhatsApp Messages");
        confirm.setHeaderText("This will open " + withContact.size() + " WhatsApp chat(s), one at a time.");
        confirm.setContentText("You'll need to click Send inside WhatsApp for each one. Continue?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                for (Student s : withContact) {
                    openWhatsAppChat(s.getContact(), message);
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
            }
        });
    }

    private void sendToStudent(Student student) {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert("Please type a message first.");
            return;
        }
        openWhatsAppChat(student.getContact(), message);
    }

    private void openWhatsAppChat(String contact, String message) {
        String cleanNumber = contact.replaceAll("[^0-9]", "");
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

        try {
            // Try opening directly in WhatsApp Desktop app (skips browser landing page)
            String desktopUrl = "whatsapp://send?phone=" + cleanNumber + "&text=" + encodedMessage;
            Desktop.getDesktop().browse(new URI(desktopUrl));
        } catch (Exception desktopFailed) {
            try {
                // Fallback: opens in default browser via wa.me
                String webUrl = "https://wa.me/" + cleanNumber + "?text=" + encodedMessage;
                Desktop.getDesktop().browse(new URI(webUrl));
            } catch (Exception e) {
                showAlert("Could not open WhatsApp for this number. If WhatsApp shows this number is invalid, ask the student/parent for their correct WhatsApp number.");
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}