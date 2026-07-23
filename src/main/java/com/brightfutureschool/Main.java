package com.brightfutureschool;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        com.brightfutureschool.db.DatabaseManager.initSchema();
        Parent splashRoot = FXMLLoader.load(getClass().getResource("/fxml/Splash.fxml"));
        Scene scene = new Scene(splashRoot, 800, 600);

        primaryStage.setTitle("Bright Future School App");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), splashRoot);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                try {
                    Parent mainRoot = FXMLLoader.load(getClass().getResource("/fxml/MainLayout.fxml"));
                    mainRoot.setOpacity(0);
                    scene.setRoot(mainRoot);

                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), mainRoot);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            fadeOut.play();
        });
        delay.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}