package com.brightfutureschool.controller.contact;

import com.brightfutureschool.dao.local.AdminSenderDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ManageSendersDialogController {

    @FXML private TextField numberField;
    @FXML private ListView<String> numbersList;
    @FXML private Label errorLabel;

    private final AdminSenderDao senderDao = new AdminSenderDao();
    private Runnable onChange;

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @FXML
    public void initialize() {
        loadNumbers();
    }

    private void loadNumbers() {
        try {
            numbersList.setItems(FXCollections.observableArrayList(senderDao.getAllNumbers()));
        } catch (Exception e) {
            showError("Failed to load numbers: " + e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        String number = numberField.getText().trim().replaceAll("[^0-9]", "");
        if (number.isEmpty()) {
            showError("Enter a valid number with country code, digits only.");
            return;
        }
        try {
            senderDao.addNumber(number);
            numberField.clear();
            loadNumbers();
            if (onChange != null) onChange.run();
        } catch (Exception e) {
            showError("Failed to add: " + e.getMessage());
        }
    }

    @FXML
    private void onClose() {
        String selected = numbersList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Right-click-free simple delete: selecting + closing doesn't delete;
            // deletion handled via double-click below instead.
        }
        ((Stage) numberField.getScene().getWindow()).close();
    }

    @FXML
    private void onListClicked(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            String selected = numbersList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                senderDao.deleteNumber(selected);
                loadNumbers();
                if (onChange != null) onChange.run();
            } catch (Exception e) {
                showError("Failed to delete: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}