package com.brightfutureschool.controller.info;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;

public class PoweredByGateController {

    @FXML private Label whatsappLabel;

    private static final String WHATSAPP_NUMBER = "923XXXXXXXXX"; // TODO: replace with your actual WhatsApp number (country code, no +, no spaces)

    @FXML
    private void onMessageOnWhatsApp() {
        try {
            String url = "https://wa.me/" + WHATSAPP_NUMBER
                    + "?text=" + java.net.URLEncoder.encode("Hi, I'm reaching out regarding the Bright Future School App.", "UTF-8");
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        ((Stage) whatsappLabel.getScene().getWindow()).close();
    }
}