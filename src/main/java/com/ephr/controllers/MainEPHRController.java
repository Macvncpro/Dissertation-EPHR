package com.ephr.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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

    @FXML private Label recordsLabel;
    @FXML private TableView<PatientRecord> patientTable;

    @FXML private Button refreshButton;
    @FXML private TableColumn<PatientRecord, String> firstNameCol;
    @FXML private TableColumn<PatientRecord, String> lastNameCol;
    @FXML private TableColumn<PatientRecord, String> emailCol;
    @FXML private TableColumn<PatientRecord, String> genderCol;
    @FXML private TableColumn<PatientRecord, String> dobCol;

    @FXML private TableColumn<PatientRecord, String> medicalCol;
    @FXML private TableColumn<PatientRecord, String> allergyCol;
    @FXML private TableColumn<PatientRecord, String> insuranceCompanyCol;
    @FXML private TableColumn<PatientRecord, String> insuranceNumberCol;

    @FXML private TitledPane addUserPane;
    @FXML private TextField firstNameField, lastNameField, emailField;
    @FXML private DatePicker dobPicker;
    @FXML private ChoiceBox<String> genderChoiceBox, roleChoiceBox;
    @FXML private Label formStatusLabel;

    @FXML private TextField medicalHistoryField;
    @FXML private TextField allergiesField;
    @FXML private TextField insuranceCompanyField;
    @FXML private TextField insuranceNumberField;
    @FXML private ChoiceBox<String> doctorChoiceBox;
    

    private String email;
    private String role;
    private Map<String, Integer> doctorMap = new HashMap<>();

    public void setUserContext(String email, String role) {
        this.email = email;
        this.role = role;

        applyPermissions();

        if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("receptionist")) {
            addUserPane.setVisible(true);

            genderChoiceBox.setItems(FXCollections.observableArrayList("male", "female"));

            if (role.equalsIgnoreCase("admin")) {
                roleChoiceBox.setItems(FXCollections.observableArrayList("admin", "doctor", "nurse", "receptionist", "patient"));
            } else {
                roleChoiceBox.setItems(FXCollections.observableArrayList("patient"));
            }

            // Restrict DOB picker to prevent future dates
            dobPicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isAfter(LocalDate.now()));
                }
            });

            // Populate doctor dropdown (doctorChoiceBox)
            ObservableList<String> doctorNames = FXCollections.observableArrayList();
            doctorMap.clear();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/database/users.db");
                PreparedStatement stmt = conn.prepareStatement("SELECT id, first_name, last_name FROM users WHERE role = 'doctor'");
                ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    doctorMap.put(fullName, id);
                    doctorNames.add(fullName);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            doctorChoiceBox.setItems(doctorNames);
            doctorChoiceBox.setDisable(!role.equalsIgnoreCase("receptionist") && !role.equalsIgnoreCase("admin"));

        } else {
            addUserPane.setVisible(false);
        }
    }


    private void applyPermissions() {
        switch (role.toLowerCase()) {
            case "admin" -> {
                // Show everything
            }
            case "doctor", "nurse", "receptionist" -> {
                loadAndShowPatientTable();
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
    private void handleRefreshTable() {
        loadAndShowPatientTable();
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
        refreshButton.setVisible(true);
    }

    @FXML
    private void handleCreateUser() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
    
        LocalDate selectedDate = dobPicker.getValue();
        if (selectedDate == null) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Please select a valid date of birth.");
            return;
        }
    
        String dob = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String gender = genderChoiceBox.getValue();
        String newUserRole = roleChoiceBox.getValue();
    
        String medicalHistory = medicalHistoryField.getText();
        String allergies = allergiesField.getText();
        String insuranceCompany = insuranceCompanyField.getText();
        String insuranceNumber = insuranceNumberField.getText();
    
        // Validate required fields
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()
            || gender == null || newUserRole == null) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Please fill out all fields.");
            return;
        }
    
        // Insert into users table
        int userId = DatabaseHelper.insertUserAndReturnId(firstName, lastName, email, dob, gender, newUserRole);
        boolean success = (userId != -1);
    
        // If it's a patient, also insert into patient table with doctor link
        if (success && newUserRole.equalsIgnoreCase("patient")) {
            Integer doctorId = null;
    
            String selectedDoctor = doctorChoiceBox.getValue();
            if (selectedDoctor != null && doctorMap.containsKey(selectedDoctor)) {
                int selectedDoctorUserId = doctorMap.get(selectedDoctor);
                doctorId = DatabaseHelper.getDoctorIdByUserId(selectedDoctorUserId);
            }
            
    
            success = DatabaseHelper.insertPatientDetails(
                userId, medicalHistory, allergies, insuranceCompany, insuranceNumber, doctorId
            );
        }
    
        if (success) {
            formStatusLabel.setStyle("-fx-text-fill: green;");
            formStatusLabel.setText("✅ User created.");
            loadAndShowPatientTable();
        } else {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Failed to create user.");
        }
    }    

    @FXML
    private void initialize() {
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