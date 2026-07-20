package com.brightfutureschool.controller.teacher;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.TeacherDao;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Teacher;
import com.brightfutureschool.util.ImageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;

import java.util.List;

public class TeacherController {

    @FXML private FlowPane teachersFlow;
    @FXML private TextField searchField;
    @FXML private Button deleteModeBtn;

    private final TeacherDao teacherDao = new TeacherDao();
    private final ClassDao classDao = new ClassDao();
    private boolean deleteMode = false;

    @FXML
    public void initialize() {
        loadTeachers();
    }

    private void loadTeachers() {
        try {
            List<Teacher> teachers = teacherDao.getAllTeachers();
            renderCards(teachers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderCards(List<Teacher> teachers) {
        teachersFlow.getChildren().clear();
        for (Teacher t : teachers) {
            teachersFlow.getChildren().add(buildTeacherCard(t));
        }
    }

    private VBox buildTeacherCard(Teacher t) {
        double frameW = 170, frameH = 160;

        ImageView photoView = new ImageView();
        photoView.setFitWidth(frameW);
        photoView.setFitHeight(frameH);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(frameW, frameH);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        photoView.setClip(clip);

        StackPane photoWrap = new StackPane(photoView);
        photoWrap.setPrefSize(frameW, frameH);
        photoWrap.setMinSize(frameW, frameH);
        photoWrap.setMaxSize(frameW, frameH);
        photoWrap.setStyle("-fx-background-color: #E8D9C3; -fx-background-radius: 12 12 0 0;");
        photoWrap.setAlignment(Pos.CENTER);

        Image img = ImageUtil.decodeFromBase64(t.getPhotoBase64());
        if (img != null) {
            photoView.setImage(img);
            photoView.setPreserveRatio(false);

            double imgW = img.getWidth();
            double imgH = img.getHeight();
            double imgRatio = imgW / imgH;
            double frameRatio = frameW / frameH;

            double viewportW, viewportH, viewX, viewY;
            if (imgRatio > frameRatio) {
                // image is wider than frame -- crop left/right
                viewportH = imgH;
                viewportW = imgH * frameRatio;
                viewX = (imgW - viewportW) / 2;
                viewY = 0;
            } else {
                // image is taller than frame -- crop top/bottom
                viewportW = imgW;
                viewportH = imgW / frameRatio;
                viewX = 0;
                viewY = (imgH - viewportH) / 2;
            }
            photoView.setViewport(new javafx.geometry.Rectangle2D(viewX, viewY, viewportW, viewportH));
        } else {
            Label placeholderIcon = new Label("👤");
            placeholderIcon.setStyle("-fx-font-size: 50px; -fx-opacity: 0.4;");
            photoWrap.getChildren().add(placeholderIcon);
        }
        Label nameLabel = new Label(t.getFullName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label subjectLabel = new Label(t.getSubject() == null || t.getSubject().isEmpty() ? "Subject: -" : "Subject: " + t.getSubject());
        subjectLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7B5B3E;");

        VBox infoBox = new VBox(3, nameLabel, subjectLabel);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setStyle("-fx-padding: 8;");

        VBox card = new VBox(photoWrap, infoBox);
        card.getStyleClass().add("class-card");
        card.setPrefWidth(170);
        card.setStyle(card.getStyle() + "; -fx-padding: 0;");

        if (deleteMode) {
            card.setStyle(card.getStyle() + "; -fx-border-color: #E74C3C;");
            card.setOnMouseClicked(e -> confirmAndDelete(t));
        } else {
            card.setOnMouseClicked(e -> openDetail(t));
        }

        return card;
    }

    private void confirmAndDelete(Teacher t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete " + t.getFullName() + "?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                try {
                    teacherDao.deleteTeacher(t.getId());
                    loadTeachers();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openDetail(Teacher t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teacher/TeacherDetailDialog.fxml"));
            Parent root = loader.load();
            TeacherDetailDialogController controller = loader.getController();
            controller.initEditData(t, this::loadTeachers);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Teacher Details");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddTeacher() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teacher/TeacherDetailDialog.fxml"));
            Parent root = loader.load();
            TeacherDetailDialogController controller = loader.getController();
            controller.initAddData(this::loadTeachers);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Teacher");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onToggleDeleteMode() {
        deleteMode = !deleteMode;
        deleteModeBtn.setText(deleteMode ? "✕ Exit Delete Mode" : "🗑 Delete Mode");
        loadTeachers();
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        try {
            List<Teacher> results = query.isEmpty() ? teacherDao.getAllTeachers() : teacherDao.searchTeachers(query);
            renderCards(results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}