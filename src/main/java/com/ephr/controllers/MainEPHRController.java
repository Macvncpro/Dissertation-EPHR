package com.ephr.controllers;

import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import com.ephr.Main;
import com.ephr.helpers.Auth0Helper;
import com.ephr.helpers.DatabaseHelper;
import com.ephr.models.PatientRecord;

import javafx.scene.control.cell.PropertyValueFactory;

public class MainEPHRController {

    @FXML private Button appointmentsButton;
    @FXML private Button reportsButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button logoutButton;

    @FXML private Label patientNameLabel;
    @FXML private Label patientAgeLabel;
    @FXML private Label patientGenderLabel;

    @FXML private Label recordsLabel;
    @FXML private TableView<PatientRecord> patientTable;

    @FXML private TableColumn<PatientRecord, String> firstNameCol;
    @FXML private TableColumn<PatientRecord, String> lastNameCol;
    @FXML private TableColumn<PatientRecord, String> emailCol;
    @FXML private TableColumn<PatientRecord, String> genderCol;
    @FXML private TableColumn<PatientRecord, String> dobCol;

    @FXML private TableColumn<PatientRecord, String> medicalCol;
    @FXML private TableColumn<PatientRecord, String> allergyCol;
    @FXML private TableColumn<PatientRecord, String> insuranceCompanyCol;
    @FXML private TableColumn<PatientRecord, String> insuranceNumberCol;

    private String email;
    private String role;

    public void setUserContext(String email, String role) {
        this.email = email;
        this.role = role;

        applyPermissions();
    }

    private void applyPermissions() {
        switch (role.toLowerCase()) {
            case "admin" -> {
                // Show everything
            }
            case "doctor", "nurse", "receptionist" -> {
                loadAndShowPatientTable();
            }
            case "staff", "patient" -> {
                prescriptionsButton.setVisible(false);
                reportsButton.setVisible(false);
                appointmentsButton.setVisible(false);
            }
            default -> System.out.println("⚠ Unknown role: " + role);
        }
    }

    private void loadAndShowPatientTable() {
        ObservableList<PatientRecord> data = DatabaseHelper.getAllPatientRecords();

        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        dobCol.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));

        medicalCol.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
        allergyCol.setCellValueFactory(new PropertyValueFactory<>("allergies"));
        insuranceCompanyCol.setCellValueFactory(new PropertyValueFactory<>("insuranceCompany"));
        insuranceNumberCol.setCellValueFactory(new PropertyValueFactory<>("insuranceNumber"));

        patientTable.setItems(data);
        patientTable.setVisible(true);
        recordsLabel.setVisible(true);
    }

    @FXML
    private void initialize() {
        // UI defaults
        patientTable.setVisible(false);
        recordsLabel.setVisible(false);
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