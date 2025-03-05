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
    
        // ✅ Clear user session to prevent auto-login
        clearUserSession();
    
        // ✅ Instantly switch to login page BEFORE logging out from Auth0
        Platform.runLater(() -> {
            try {
                Main.showLoginScreen();
                System.out.println("🔄 Instantly switched to LoginPage.fxml (Session Cleared)");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("❌ Error loading login screen.");
            }
        });
    
        // ✅ Perform a **forced** Auth0 logout in the background
        Platform.runLater(() -> {
            try {
                Thread.sleep(500); // Small delay to prevent race conditions
                String logoutURL = "https://" + Auth0Helper.getDomain() + "/v2/logout" +
                                   "?client_id=" + Auth0Helper.getClientId() +
                                   "&returnTo=http://localhost:8080/logout_complete";  // <-- NEW: Redirect to dummy logout page
    
                WebView webView = new WebView();
                WebEngine webEngine = webView.getEngine();
                webEngine.load(logoutURL);
    
                System.out.println("✅ Navigating to Auth0 Logout...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Clears stored session data to prevent auto-login after logout.
     */
    private void clearUserSession() {
        System.out.println("🧹 Clearing session data...");
        System.setProperty("user_session", ""); 
        System.clearProperty("AUTH0_ACCESS_TOKEN");
        System.clearProperty("AUTH0_ID_TOKEN");
    }    

    @FXML
    private void handleNavigation() {
        // Handle navigation logic for buttons (Appointments, Reports, etc.)
        // Example:
        System.out.println("Navigating to another section...");
    }
}