package com.ephr;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.io.IOException;

public class LoginController {
    @FXML
    private WebView webView;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            WebEngine webEngine = webView.getEngine();
            String authURL = "https://" + Auth0Helper.getDomain() + "/authorize" +
                    "?client_id=" + Auth0Helper.getClientId() +
                    "&response_type=code" +
                    "&redirect_uri=http://localhost:8080/callback" +
                    "&scope=openid profile email";

            webEngine.load(authURL);

            // Listen for URL changes to detect successful login
            webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
                System.out.println("WebView navigated to: " + newLocation);  // Debugging output
            
                if (newLocation.startsWith("http://localhost:8080/callback")) {
                    System.out.println("✅ Callback detected!");
                    handleSuccessfulLogin();
                }
            });            
        });
    }

    private void handleSuccessfulLogin() {
        System.out.println("✅ Login Successful, Redirecting to Main EPHR Screen...");
        Platform.runLater(() -> {
            try {
                Main.showEPHRScreen();
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading the EPHR screen.");
            }
        });
    }
}