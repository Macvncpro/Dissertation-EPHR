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
                    "&scope=openid profile email" +
                    "&prompt=login";  // <-- NEW: Forces login every time
    
            System.out.println("🔒 Redirecting to Auth0 login...");
            webEngine.load(authURL);
    
            // Listen for Auth0 callback
            webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
                System.out.println("🌐 WebView navigated to: " + newLocation);
                if (newLocation.startsWith("http://localhost:8080/callback")) {
                    System.out.println("✅ Auth0 Callback Detected!");
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