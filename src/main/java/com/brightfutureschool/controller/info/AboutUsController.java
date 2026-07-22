package com.brightfutureschool.controller.info;

import com.brightfutureschool.config.GateInfo;
import javafx.fxml.FXML;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;

public class AboutUsController {

    @FXML
    private void onWhatsApp() {
        openUrl("https://wa.me/" + GateInfo.WHATSAPP_NUMBER);
    }

    @FXML
    private void onEmail() {
        openUrl("mailto:" + GateInfo.EMAIL);
    }

    @FXML
    private void onFacebook() {
        openUrl(GateInfo.FACEBOOK_URL);
    }

    @FXML
    private void onInstagram() {
        openUrl(GateInfo.INSTAGRAM_URL);
    }

    @FXML
    private void onLinkedIn() {
        openUrl(GateInfo.LINKEDIN_URL);
    }

    @FXML
    private void onTwitter() {
        openUrl(GateInfo.TWITTER_URL);
    }

    @FXML
    private void onGithub() {
        openUrl(GateInfo.GITHUB_URL);
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}