package com.ephr;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MainEPHRController {

    @FXML
    private Button appointmentsButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button prescriptionsButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label patientAgeLabel;

    @FXML
    private Label patientGenderLabel;

    @FXML
    private TableView<?> medicalHistoryTable;

    @FXML
    private TableColumn<?, ?> dateColumn;

    @FXML
    private TableColumn<?, ?> conditionColumn;

    @FXML
    private TableColumn<?, ?> doctorColumn;

    @FXML
    private void initialize() {
        // Load patient details
        patientNameLabel.setText("John Doe");
        patientAgeLabel.setText("30");
        patientGenderLabel.setText("Male");

        // Populate medical history (use data from the database in a real scenario)
        // For now, this is just a placeholder
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Logging out...");
    
        // ✅ First, clear any session-related data (if stored)
        clearUserSession(); // Implemented below
    
        // ✅ Construct the Auth0 logout URL
        String logoutURL = "https://" + Auth0Helper.getDomain() + "/v2/logout" +
                           "?client_id=" + Auth0Helper.getClientId() +
                           "&returnTo=http://localhost:8080"; 
    
        // ✅ Ensure WebView does not cause auto-login
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(logoutURL);
        System.out.println("Navigating to Auth0 Logout...");
    
        // ✅ Redirect to login screen AFTER ensuring session is cleared
        Platform.runLater(() -> {
            try {
                Main.showLoginScreen();
                System.out.println("🔄 Switched to LoginPage.fxml (Session Cleared)");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("❌ Error loading login screen.");
            }
        });
    }
    
    /**
     * Clears stored session data to prevent auto-login after logout.
     */
    private void clearUserSession() {
        System.out.println("🧹 Clearing session data...");
    
        // Example: If you're storing user session data, reset it here
        System.setProperty("user_session", ""); // If using system properties
        // You can also clear any stored tokens, cookies, or credentials
    }    


    @FXML
    private void handleNavigation() {
        // Handle navigation logic for buttons (Appointments, Reports, etc.)
        // Example:
        System.out.println("Navigating to another section...");
    }
}