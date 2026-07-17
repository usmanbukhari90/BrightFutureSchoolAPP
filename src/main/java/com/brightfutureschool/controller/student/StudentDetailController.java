package com.brightfutureschool.controller.student;

import com.brightfutureschool.model.Student;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class StudentDetailController {

    @FXML private ImageView photoView;
    @FXML private Label nameLabel;
    @FXML private Label rollBadgeLabel;

    @FXML private Label dobValue;
    @FXML private Label genderValue;
    @FXML private Label religionValue;
    @FXML private Label nationalityValue;

    @FXML private Label fatherNameValue;
    @FXML private Label fatherProfessionValue;
    @FXML private Label fatherContactValue;
    @FXML private Label motherNameValue;
    @FXML private Label motherProfessionValue;

    @FXML private Label contactValue;
    @FXML private Label addressValue;

    @FXML private Label bformValue;
    @FXML private Label fatherCnicValue;
    @FXML private Label motherCnicValue;

    @FXML
    public void initialize() {
        Circle clip = new Circle(60, 60, 60);
        photoView.setClip(clip);
    }

    public void setStudent(Student s) {
        nameLabel.setText(s.getFullName());
        rollBadgeLabel.setText("Roll No: " + s.getRollNo());

        dobValue.setText(orDash(s.getDateOfBirth()));
        genderValue.setText(orDash(s.getGender()));
        religionValue.setText(orDash(s.getReligion()));
        nationalityValue.setText(orDash(s.getNationality()));

        fatherNameValue.setText(orDash(s.getFatherName()));
        fatherProfessionValue.setText(orDash(s.getFatherProfession()));
        fatherContactValue.setText(orDash(s.getFatherContact()));
        motherNameValue.setText(orDash(s.getMotherName()));
        motherProfessionValue.setText(orDash(s.getMotherProfession()));

        contactValue.setText(orDash(s.getContact()));
        addressValue.setText(orDash(s.getAddress()));

        bformValue.setText(orDash(s.getStudentBform()));
        fatherCnicValue.setText(orDash(s.getFatherCnic()));
        motherCnicValue.setText(orDash(s.getMotherCnic()));

        if (s.getPhotoBase64() != null && !s.getPhotoBase64().isEmpty()) {
            try {
                photoView.setImage(new Image(s.getPhotoBase64()));
            } catch (Exception ignored) { }
        }
    }

    private String orDash(String value) {
        return (value == null || value.isEmpty()) ? "-" : value;
    }

    @FXML
    private void onClose() {
        ((Stage) nameLabel.getScene().getWindow()).close();
    }
}