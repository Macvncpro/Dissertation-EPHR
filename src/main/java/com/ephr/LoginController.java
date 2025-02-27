package com.ephr;

import com.auth0.AuthenticationController;
import com.auth0.Tokens;
import com.auth0.IdentityVerificationException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class LoginController {
    @FXML
    private WebView webView; // WebView for Auth0 login

    @FXML
    private Label errorLabel;

    private final AuthenticationController authController = Auth0Helper.getAuthController();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            WebEngine webEngine = webView.getEngine();
            String authURL = "https://" + Auth0Helper.getDomain() + "/authorize" +
                    "?client_id=" + Auth0Helper.getClientId() +
                    "&response_type=code" +
                    "&redirect_uri=http://localhost:8080/callback" +
                    "&scope=openid profile email";

            webEngine.load(authURL); // Auto-load Auth0 login
        });
    }

    public void handleCallback(HttpServletRequest request) {
        try {
            Tokens tokens = Auth0Helper.handleCallback(request);
            System.out.println("Access Token: " + tokens.getAccessToken());

            // Redirect to EPHR screen after login
            Platform.runLater(() -> {
                try {
                    Main.showEPHRScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("Error loading the EPHR screen.");
                }
            });
        } catch (IdentityVerificationException e) {
            Platform.runLater(() -> errorLabel.setText("Authentication failed."));
        }
    }
}