package com.brightfutureschool.controller;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SplashController {

    @FXML private ImageView splashLogo;
    @FXML private ImageView watermarkLogo;
    @FXML private VBox centerBox;
    @FXML private StackPane spinnerPane;
    @FXML
    public void initialize() {
        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            Image logo = new Image(logoUrl.toExternalForm());
            splashLogo.setImage(logo);
            watermarkLogo.setImage(logo);
        }

        // Build 8 dots in a circle
        Group dotsGroup = new Group();
        int totalDots = 8;
        double radius = 20;
        for (int i = 0; i < totalDots; i++) {
            double angle = 2 * Math.PI * i / totalDots;
            Circle dot = new Circle(4, Color.web("#7B5B3E"));
            dot.setTranslateX(radius * Math.cos(angle));
            dot.setTranslateY(radius * Math.sin(angle));
            dot.setOpacity(1.0 - (i * 1.0 / totalDots)); // fades around the ring
            dotsGroup.getChildren().add(dot);
        }
        spinnerPane.getChildren().add(dotsGroup);

        RotateTransition spin = new RotateTransition(Duration.millis(900), dotsGroup);
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.play();

        // Entrance animation
        centerBox.setScaleX(0.01);
        centerBox.setScaleY(0.01);
        centerBox.setOpacity(0);

        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(700), centerBox);
        zoomIn.setFromX(0.01);
        zoomIn.setFromY(0.01);
        zoomIn.setToX(1);
        zoomIn.setToY(1);
        zoomIn.setInterpolator(Interpolator.SPLINE(0.17, 0.67, 0.35, 1.0));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(700), centerBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        new ParallelTransition(zoomIn, fadeIn).play();
    }
}