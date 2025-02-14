package com.ephr;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

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
        try {
            Main mainApp = new Main();
            mainApp.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavigation() {
        // Handle navigation logic for buttons (Appointments, Reports, etc.)
        // Example:
        System.out.println("Navigating to another section...");
    }
}