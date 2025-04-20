package com.ephr;

import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MainEPHRController {

    @FXML private Button appointmentsButton;
    @FXML private Button reportsButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button logoutButton;

    @FXML private Label patientNameLabel;
    @FXML private Label patientAgeLabel;
    @FXML private Label patientGenderLabel;

    private String email;
    private String role;

    // Call this after FXML is loaded to set context
    public void setUserContext(String email, String role) {
        this.email = email;
        this.role = role;

        // Example placeholder logic
        patientNameLabel.setText(email);
        patientAgeLabel.setText("N/A");
        patientGenderLabel.setText(role);

        applyPermissions();
    }

    private void applyPermissions() {
        switch (role.toLowerCase()) {
            case "admin" -> {
                // Full access
            }
            case "doctor" -> {
                prescriptionsButton.setVisible(true);
                reportsButton.setVisible(true);
                appointmentsButton.setVisible(true);
            }
            case "nurse" -> {
                prescriptionsButton.setVisible(false);
                reportsButton.setVisible(false);
            }
            case "staff" -> {
                prescriptionsButton.setVisible(false);
                reportsButton.setVisible(false);
                appointmentsButton.setVisible(false);
            }
            case "patient" -> {
                prescriptionsButton.setVisible(false);
                reportsButton.setVisible(false);
                appointmentsButton.setVisible(false);
            }
            default -> System.out.println("⚠ Unknown role: " + role);
        }
    }

    @FXML
    private void initialize() {
        // Optional: pre-load static UI here if needed
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Logging out...");
        clearUserSession();

        Platform.runLater(() -> {
            try {
                Main.showLoginScreen();
                System.out.println("🔄 Instantly switched to LoginPage.fxml (Session Cleared)");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("❌ Error loading login screen.");
            }
        });

        Platform.runLater(() -> {
            try {
                Thread.sleep(500);
                String logoutURL = "https://" + Auth0Helper.getDomain() + "/v2/logout" +
                                   "?client_id=" + Auth0Helper.getClientId() +
                                   "&returnTo=http://localhost:8080/logout_complete";

                WebView webView = new WebView();
                WebEngine webEngine = webView.getEngine();
                webEngine.load(logoutURL);

                System.out.println("✅ Navigating to Auth0 Logout...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void clearUserSession() {
        System.out.println("🧹 Clearing session data...");
        System.setProperty("user_session", ""); 
        System.clearProperty("AUTH0_ACCESS_TOKEN");
        System.clearProperty("AUTH0_ID_TOKEN");
    }

    @FXML
    private void handleNavigation() {
        System.out.println("Navigating to another section...");
    }
}