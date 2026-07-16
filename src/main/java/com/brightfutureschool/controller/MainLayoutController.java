package com.brightfutureschool.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainLayoutController {

    @FXML private ImageView logoImage;
    @FXML private ImageView sidebarLogo;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;

    @FXML
    private void toggleSidebar() {
        boolean visible = sidebar.isVisible();
        sidebar.setVisible(!visible);
        sidebar.setManaged(!visible);
    }
    @FXML
    public void initialize() {
        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            Image logo = new Image(logoUrl.toExternalForm());
            logoImage.setImage(logo);
            sidebarLogo.setImage(logo);
        }

        // Explicit initial state — sidebar starts open
        sidebar.setVisible(false);
        sidebar.setManaged(false);

        showDashboard(); // default page on load
    }

    private void setContent(String message) {
        contentArea.getChildren().setAll(new Label(message));
    }

    private void setContent(String fxmlPath, String errorContext) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent("Failed to load " + errorContext + ": " + e.getMessage());
        }
    }

    @FXML
    private void showDashboard() {
        loadIntoContent("/fxml/Dashboard.fxml");
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            javafx.scene.Parent node = javafx.fxml.FXMLLoader.load(getClass().getResource(fxmlPath));
            if (node instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML private void showStudents()    { setContent("/fxml/student/StudentRecords.fxml", "Student Records"); }
    @FXML private void showTeachers() { loadIntoContent("/fxml/teacher/Teachers.fxml"); }
    @FXML private void showAttendance()  { setContent("/fxml/attendance/Attendance.fxml", "Attendance"); }
    @FXML private void showFee() { loadIntoContent("/fxml/fee/Fee.fxml"); }
    @FXML private void showResults()     { setContent("/fxml/result/ResultsExams.fxml", "Results & Exams"); }
    @FXML private void showContact() { loadIntoContent("/fxml/contact/Contact.fxml"); }
    @FXML private void showSettings()    { setContent("/fxml/BackupSettings.fxml", "Backup Settings"); }
    @FXML private void showAbout()       { setContent("/fxml/info/AboutUs.fxml", "About Us"); }

    @FXML
    private void onPrivacyPolicy() {
        openInfoDialog("/fxml/info/PrivacyPolicy.fxml", "Privacy Policy");
    }

    @FXML
    private void onTermsConditions() {
        openInfoDialog("/fxml/info/TermsConditions.fxml", "Terms & Conditions");
    }

    @FXML
    private void onPoweredByGate() {
        openInfoDialog("/fxml/info/PoweredByGate.fxml", "Powered by Gate");
    }

    private void openInfoDialog(String fxmlPath, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
