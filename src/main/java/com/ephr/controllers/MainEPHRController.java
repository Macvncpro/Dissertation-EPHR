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
    @FXML private Button deleteButton;
    @FXML private TableColumn<PatientRecord, String> firstNameCol;
    @FXML private TableColumn<PatientRecord, String> lastNameCol;
    @FXML private TableColumn<PatientRecord, String> emailCol;
    @FXML private TableColumn<PatientRecord, String> genderCol;
    @FXML private TableColumn<PatientRecord, String> dobCol;
    @FXML private TableColumn<PatientRecord, String> nhsCol;
    @FXML private TableColumn<PatientRecord, String> statusCol;
    @FXML private TableColumn<PatientRecord, Boolean> sharingCol;
    @FXML private TableColumn<PatientRecord, Boolean> scrCol;
    @FXML private TableColumn<PatientRecord, String> phoneCol;
    @FXML private TableColumn<PatientRecord, String> contactCol;
    @FXML private TableColumn<PatientRecord, String> addressLine1Col;
    @FXML private TableColumn<PatientRecord, String> addressLine2Col;
    @FXML private TableColumn<PatientRecord, String> postcodeCol;

    @FXML private TitledPane addUserPane;
    @FXML private TextField firstNameField, lastNameField, emailField;
    @FXML private DatePicker dobPicker;
    @FXML private ChoiceBox<String> genderChoiceBox, roleChoiceBox;
    @FXML private Label formStatusLabel;

    @FXML private TextField nhsNumberField;
    @FXML private ChoiceBox<String> statusChoiceBox;
    @FXML private ChoiceBox<String> dataSharingChoiceBox;
    @FXML private ChoiceBox<String> scrConsentChoiceBox;    
    @FXML private ChoiceBox<String> doctorChoiceBox;
    @FXML private TextField phoneField;
    @FXML private ChoiceBox<String> contactChoiceBox;
    @FXML private TextField addressLine1Field;
    @FXML private TextField addressLine2Field;
    @FXML private TextField postcodeField;

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
            genderChoiceBox.setValue("male");
    
            if (role.equalsIgnoreCase("admin")) {
                roleChoiceBox.setItems(FXCollections.observableArrayList("admin", "doctor", "nurse", "receptionist", "patient"));
            } else {
                roleChoiceBox.setItems(FXCollections.observableArrayList("patient"));
                roleChoiceBox.setValue("patient");
                roleChoiceBox.setDisable(true); // Locks it for receptionists
            }
            roleChoiceBox.setValue("patient");
    
            // Restrict DOB picker to prevent future dates
            dobPicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isAfter(LocalDate.now()));
                }
            });
    
            // Populate doctor dropdown
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
            doctorChoiceBox.setDisable(false);
    
            // Consent and Status Choices
            statusChoiceBox.setItems(FXCollections.observableArrayList("active", "inactive", "deceased"));
            statusChoiceBox.setValue("active");
    
            dataSharingChoiceBox.setItems(FXCollections.observableArrayList("Yes", "No"));
            dataSharingChoiceBox.setValue("Yes");
    
            scrConsentChoiceBox.setItems(FXCollections.observableArrayList("Yes", "No"));
            scrConsentChoiceBox.setValue("Yes");

            contactChoiceBox.setItems(FXCollections.observableArrayList("email", "phone", "SMS", "NHS App", "letter"));
            contactChoiceBox.setValue("email");
    
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
    private void handleDeletePatient() {
        PatientRecord selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a patient to delete.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this patient?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean success = DatabaseHelper.deletePatientByEmail(selected.getEmail());
                if (success) {
                    handleRefreshTable();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to delete the patient.", ButtonType.OK);
                    error.showAndWait();
                }
            }
        });
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

        nhsCol.setCellValueFactory(new PropertyValueFactory<>("nhsNumber"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        sharingCol.setCellValueFactory(new PropertyValueFactory<>("dataSharingConsent"));
        scrCol.setCellValueFactory(new PropertyValueFactory<>("scrConsent"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        contactCol.setCellValueFactory(new PropertyValueFactory<>("preferredContact"));

        addressLine1Col.setCellValueFactory(new PropertyValueFactory<>("addressLine1"));
        addressLine2Col.setCellValueFactory(new PropertyValueFactory<>("addressLine2"));
        postcodeCol.setCellValueFactory(new PropertyValueFactory<>("postcode"));

        patientTable.setItems(data);
        patientTable.setVisible(true);
        recordsLabel.setVisible(true);
        refreshButton.setVisible(true);
        deleteButton.setVisible(true);
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
    
        String dob = selectedDate.toString(); // This gives yyyy-MM-dd format
        String gender = genderChoiceBox.getValue();
        String newUserRole = roleChoiceBox.getValue();
    
        String nhsNumber = nhsNumberField.getText().replaceAll("\\s+", ""); // Remove spaces for validation

        // Validate NHS Number: Must be 10-digit numeric (basic check)
        if (!nhsNumber.matches("\\d{10}")) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ NHS Number must be 10 digits.");
            return;
        }

        String status = statusChoiceBox.getValue();
        boolean dataSharing = "Yes".equals(dataSharingChoiceBox.getValue());
        boolean scrConsent = "Yes".equals(scrConsentChoiceBox.getValue());

        if (newUserRole.equalsIgnoreCase("patient") && nhsNumber.isBlank()) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ NHS Number is required for patients.");
            return;
        }

        String phone = phoneField.getText();
        
        if (!phone.matches("^07\\d{9}$")) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Enter a valid UK mobile number (e.g. 07XXXXXXXXX).");
            return;
        }        
    
        String preferredContact = contactChoiceBox.getValue();

        String address1 = addressLine1Field.getText();
        String address2 = addressLine2Field.getText();
        String postcode = postcodeField.getText();

        if (address1.isBlank() || postcode.isBlank()) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Address Line 1 and Postcode are required.");
            return;
        }

        // Validate postcode format (basic check)
        if (!postcode.toUpperCase().matches("^([A-Z]{1,2}\\d[A-Z\\d]? \\d[A-Z]{2})$")) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Enter a valid UK postcode (e.g., SW1A 1AA).");
            return;
        }        
    
        // Validate required fields
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()
            || gender == null || newUserRole == null) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Please fill out all fields.");
            return;
        }
    
        // Insert into users table
        int userId = DatabaseHelper.insertUserAndReturnId(
            firstName, lastName, email, dob, gender, newUserRole,
            phone, preferredContact, address1, address2, postcode
        );
        
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
                userId,nhsNumber,status,doctorId,dataSharing,scrConsent);
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
    
        // NHS number formatter
        nhsNumberField.textProperty().addListener((obs, oldText, newText) -> {
            String digitsOnly = newText.replaceAll("\\D", ""); // strip non-digits
            if (digitsOnly.length() > 10) {
                digitsOnly = digitsOnly.substring(0, 10); // limit to 10 digits
            }
    
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digitsOnly.length(); i++) {
                if (i > 0 && i % 3 == 0 && i != 9) {
                    formatted.append(" ");
                }
                formatted.append(digitsOnly.charAt(i));
            }
    
            nhsNumberField.setText(formatted.toString());
            nhsNumberField.positionCaret(formatted.length()); // keep caret at end
        });

        postcodeField.setTextFormatter(new TextFormatter<>(change -> {
            String raw = change.getControlNewText().toUpperCase().replaceAll("[^A-Z0-9]", "");
        
            // Limit length (UK postcodes are 5–7 chars without space)
            if (raw.length() > 7) return null;
        
            // Insert space before final 3 characters if long enough
            String formatted;
            if (raw.length() > 3) {
                formatted = raw.substring(0, raw.length() - 3) + " " + raw.substring(raw.length() - 3);
            } else {
                formatted = raw;
            }
        
            // Apply change
            change.setText(formatted);
            change.setRange(0, change.getControlText().length()); // replace all
            return change;
        }));           
   
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