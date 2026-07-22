package com.brightfutureschool.controller.info;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PoweredByGateController {

    @FXML private ImageView gateLogo;

    private static final String GATE_WHATSAPP_NUMBER = "923183074951";

    @FXML
    public void initialize() {
        var logoUrl = getClass().getResource("/images/gate_logo.png");
        if (logoUrl != null) gateLogo.setImage(new Image(logoUrl.toExternalForm()));
    }

    @FXML
    private void onMessageWhatsApp() {
        try {
            String message = URLEncoder.encode("Hi, I'm reaching out regarding Bright Future School App.", StandardCharsets.UTF_8);
            String url = "https://wa.me/" + GATE_WHATSAPP_NUMBER + "?text=" + message;
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}