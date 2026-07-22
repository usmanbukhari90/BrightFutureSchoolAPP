package com.brightfutureschool.controller;

import com.brightfutureschool.backup.BackupService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

import java.awt.Desktop;
import java.io.File;

public class BackupSettingsController {

    @FXML private Label statusLabel;

    private final BackupService backupService = new BackupService();
    private File lastBackupFolder;

    @FXML
    private void onBackupNow() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Backup Location");
        File destination = chooser.showDialog(statusLabel.getScene().getWindow());
        if (destination == null) return;

        statusLabel.setText("Backing up, please wait...");
        try {
            lastBackupFolder = backupService.runBackup(destination);
            statusLabel.setText("✔ Backup completed successfully.\nSaved to: " + lastBackupFolder.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Backup failed: " + e.getMessage());
            showAlert("Backup Failed", e.getMessage());
        }
    }

    @FXML
    private void onOpenBackupFolder() {
        if (lastBackupFolder == null || !lastBackupFolder.exists()) {
            showAlert("No Backup Yet", "Please run a backup first.");
            return;
        }
        try {
            Desktop.getDesktop().open(lastBackupFolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}