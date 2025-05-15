package com.ephr.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import com.ephr.Main;
import com.ephr.blockchain.BlockchainLedger;
import com.ephr.helpers.Auth0Helper;
import com.ephr.helpers.DatabaseHelper;
import com.ephr.models.PatientRecord;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class MainEPHRController {

    // Left Sidebar Buttons
    @FXML private Button dashboardButton;
    @FXML private Button medicalHistoryButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button diagnosticReportsButton;
    @FXML private Button appointmentsButton;
    @FXML private Button auditButton;
    @FXML private Button logoutButton;

    @FXML private Label recordsLabel;
    @FXML private TableView<PatientRecord> patientTable;

    // Doctor or Admin view
    @FXML private Button breakGlassButton;
    private boolean btgGranted = false;
    private int btgPatientId = -1;
    private LocalDateTime btgExpiryTime;
    @FXML
    private Label btgTimerLabel;

    private Timeline btgCountdownTimer;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button refreshButton;
    @FXML private Button deleteButton;
    @FXML private TableColumn<PatientRecord, String> firstNameCol, lastNameCol, emailCol, genderCol,
            dobCol, nhsCol, statusCol, phoneCol, contactCol, addressLine1Col, addressLine2Col, postcodeCol;
    @FXML private TableColumn<PatientRecord, Boolean> sharingCol, scrCol;

    // Add User Form
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

    @FXML private AnchorPane contentArea;

    // User Profile
    @FXML private TitledPane userProfilePane;
    @FXML private Label profileFirstName, profileLastName, profileEmail, profileGender, profileDob,
                    profilePhone, profileContact, profileAddress1, profileAddress2, profilePostcode;
    @FXML private Button manageAccessButton;

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
        loadAndShowPatientTable();
    
        switch (role.toLowerCase()) {
            case "admin" -> showAll();
            case "doctor" -> showDoctorView();
            case "nurse" -> showNurseView();
            case "receptionist" -> showReceptionistView();
            case "patient" -> showPatientView();
            default -> System.out.println("⚠ Unknown role: " + role);
        }
    }

    private void showAll() {
        setNodeVisibility(
            true,
            medicalHistoryButton,
            prescriptionsButton,
            diagnosticReportsButton,
            appointmentsButton,
            searchField,
            searchButton,
            deleteButton,
            refreshButton,
            breakGlassButton,
            auditButton
        );
    }
    
    private void showDoctorView() {
        setNodeVisibility(
            true,
            diagnosticReportsButton,
            prescriptionsButton,
            appointmentsButton,
            searchField,
            searchButton,
            refreshButton,
            breakGlassButton
        );
        setNodeVisibility(
            false,
            deleteButton,
            auditButton
        );
    }
    
    private void showNurseView() {
        showDoctorView(); // reuse doctor view
        setNodeVisibility(
            false,
            prescriptionsButton,
            breakGlassButton,
            auditButton
        );
    }
    
    private void showReceptionistView() {
        setNodeVisibility(
            true,
            appointmentsButton,
            searchField,
            searchButton,
            refreshButton
        );
        setNodeVisibility(
            false,
            prescriptionsButton, 
            diagnosticReportsButton, 
            deleteButton,
            auditButton
        );
    }
    
    private void showPatientView() {
        setNodeVisibility(
            true,
            userProfilePane,
            manageAccessButton

        );
        setNodeVisibility(
            false,
            medicalHistoryButton,
            prescriptionsButton,
            diagnosticReportsButton,
            appointmentsButton,
            searchField,
            searchButton,
            deleteButton,
            refreshButton,
            patientTable,
            auditButton
        );
        
        loadUserProfile(email);
    }
    
    private void setNodeVisibility(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
    
    @FXML
    private void handleManageAccess() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grant Access to a Healthcare Professional");
        dialog.setHeaderText("Choose a user and record type to share");

        ComboBox<String> userDropdown = new ComboBox<>(DatabaseHelper.getDoctorAndNurseEmails());
        ChoiceBox<String> resourceTypeBox = new ChoiceBox<>(FXCollections.observableArrayList("prescription", "medical_history", "diagnostic_report"));
        ChoiceBox<String> permissionBox = new ChoiceBox<>(FXCollections.observableArrayList("read"));
        permissionBox.setValue("read");

        CheckBox grantAllBox = new CheckBox("Grant access to all records of this type");

        TableView<Map<String, Object>> recordTable = new TableView<>();
        ObservableList<Map<String, Object>> tableData = FXCollections.observableArrayList();
        recordTable.setPrefHeight(200);
        recordTable.setPlaceholder(new Label("No records loaded."));

        grantAllBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recordTable.setVisible(!newVal);
            recordTable.setManaged(!newVal);
        });

        resourceTypeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            tableData.clear();
            recordTable.getColumns().clear();
            int patientId = DatabaseHelper.getPatientIdByEmail(email);

            switch (newVal) {
                case "prescription" -> {
                    var records = DatabaseHelper.getPrescriptionsForPatient(patientId);
                    if (!records.isEmpty()) {
                        TableColumn<Map<String, Object>, String> col1 = new TableColumn<>("Medication");
                        col1.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("medication")));
                        TableColumn<Map<String, Object>, String> col2 = new TableColumn<>("Start");
                        col2.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("start_date")));
                        TableColumn<Map<String, Object>, String> col3 = new TableColumn<>("End");
                        col3.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("end_date")));
                        recordTable.getColumns().addAll(col1, col2, col3);
                    }
                    tableData.setAll(records);
                }
                case "medical_history" -> {
                    var records = DatabaseHelper.getMedicalHistoryForPatient(patientId);
                    if (!records.isEmpty()) {
                        TableColumn<Map<String, Object>, String> col1 = new TableColumn<>("Condition");
                        col1.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("condition")));
                        TableColumn<Map<String, Object>, String> col2 = new TableColumn<>("Diagnosis Date");
                        col2.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("diagnosis_date")));
                        TableColumn<Map<String, Object>, String> col3 = new TableColumn<>("Severity");
                        col3.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("severity")));
                        recordTable.getColumns().addAll(col1, col2, col3);
                    }
                    tableData.setAll(records);
                }
                case "diagnostic_report" -> {
                    var records = DatabaseHelper.getDiagnosticReportsForPatient(patientId);
                    if (!records.isEmpty()) {
                        TableColumn<Map<String, Object>, String> col1 = new TableColumn<>("Type");
                        col1.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("report_type")));
                        TableColumn<Map<String, Object>, String> col2 = new TableColumn<>("Date");
                        col2.setCellValueFactory(data -> new SimpleStringProperty((String) data.getValue().get("report_date")));
                        recordTable.getColumns().addAll(col1, col2);
                    }
                    tableData.setAll(records);
                }
            }

            recordTable.setItems(tableData);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(600);

        grid.add(new Label("User Email:"), 0, 0);
        grid.add(userDropdown, 1, 0);

        grid.add(new Label("Record Type:"), 0, 1);
        grid.add(resourceTypeBox, 1, 1);

        grid.add(new Label("Permission:"), 0, 2);
        grid.add(permissionBox, 1, 2);

        grid.add(grantAllBox, 1, 3);

        grid.add(new Label("Select Record:"), 0, 4);
        grid.add(recordTable, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int patientId = DatabaseHelper.getPatientIdByEmail(email);
            int userId = DatabaseHelper.getUserIdByEmail(userDropdown.getValue());
            String resourceType = resourceTypeBox.getValue();
            String permission = permissionBox.getValue();

            boolean grantedAny = false;

            if (grantAllBox.isSelected()) {
                for (Map<String, Object> row : tableData) {
                    int recordId = (int) row.get("id");
                    boolean granted = DatabaseHelper.insertPatientGrantedAccess(
                        userId, resourceType, recordId, permission, patientId, false
                    );
                    if (granted) grantedAny = true;
                }
            } else {
                var selected = recordTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    new Alert(Alert.AlertType.ERROR, "❌ No record selected.").showAndWait();
                    return;
                }
                int recordId = (int) selected.get("id");
                grantedAny = DatabaseHelper.insertPatientGrantedAccess(
                    userId, resourceType, recordId, permission, patientId, false
                );
            }

            Alert alert = new Alert(grantedAny ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setContentText(grantedAny
                ? "✅ Access granted."
                : "⚠️ Access already existed or failed.");
            alert.show();
        }
    }

    @FXML
    private void handleBreakGlass() {
        PatientRecord selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Patient Selected");
            alert.setHeaderText("Warning: Please select a patient before initiating Break-the-Glass.");
            alert.setContentText("Click on a patient row first.");
            alert.show();
            return;
        }

        String fullName = selected.getFirstName() + " " + selected.getLastName();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Break-the-Glass Access");
        dialog.setHeaderText("🛑 You are requesting emergency access to protected medical data.");

        // UI Elements
        TextField nameField = new TextField(fullName);
        nameField.setEditable(false);

        ToggleGroup reasonGroup = new ToggleGroup();
        RadioButton patientCareBtn = new RadioButton("Patient Care");
        RadioButton accessBtn = new RadioButton("Patient Access");
        RadioButton billingBtn = new RadioButton("Billing/Payment");
        patientCareBtn.setToggleGroup(reasonGroup);
        accessBtn.setToggleGroup(reasonGroup);
        billingBtn.setToggleGroup(reasonGroup);
        patientCareBtn.setSelected(true);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Operations", "Medical Records", "Research", "Unspecified");
        categoryBox.setValue("Operations");

        TextArea explanationArea = new TextArea();
        explanationArea.setPromptText("Enter your justification here...");
        explanationArea.setWrapText(true);
        explanationArea.setPrefRowCount(4);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(600);

        grid.add(new Label("Patient Name:"), 0, 0);
        grid.add(nameField, 1, 0, 2, 1);

        grid.add(new Label("Reason:"), 0, 1);
        HBox reasonBox = new HBox(10, patientCareBtn, accessBtn, billingBtn);
        grid.add(reasonBox, 1, 1, 2, 1);

        grid.add(new Label("Detailed Reason:"), 0, 2);
        grid.add(categoryBox, 1, 2, 2, 1);

        grid.add(new Label("Further Explanation:"), 0, 3);
        grid.add(explanationArea, 1, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String selectedReason = ((RadioButton) reasonGroup.getSelectedToggle()).getText();
            String category = categoryBox.getValue();
            String explanation = explanationArea.getText().trim();
        
            if (explanation.isBlank()) {
                showError("Justification is required.");
                return;
            }
        
            this.btgPatientId = DatabaseHelper.getPatientIdByEmail(selected.getEmail());
            this.btgGranted = true;
            this.btgExpiryTime = LocalDateTime.now().plusMinutes(5); // ⏲️ 5-minute expiry
            startBtGCountdown();
            logBtGAccess(email, fullName, selectedReason, category, explanation);
        }
    }

    private void startBtGCountdown() {
        if (btgCountdownTimer != null) {
            btgCountdownTimer.stop();
        }

        btgTimerLabel.setVisible(true);

        btgCountdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), btgExpiryTime).getSeconds();

            if (secondsRemaining <= 0) {
                btgGranted = false;
                btgPatientId = -1;
                btgTimerLabel.setText("BtG expired");
                btgCountdownTimer.stop();

                PauseTransition hideLabel = new PauseTransition(Duration.seconds(3));
                hideLabel.setOnFinished(e -> btgTimerLabel.setVisible(false));
                hideLabel.play();
            } else {
                long mins = secondsRemaining / 60;
                long secs = secondsRemaining % 60;
                btgTimerLabel.setText("BtG active: " + String.format("%02d:%02d", mins, secs));
            }
        }));
        btgCountdownTimer.setCycleCount(Animation.INDEFINITE);
        btgCountdownTimer.play();
    }

    private void logBtGAccess(String userEmail, String patientName, String reason, String category, String justification) {
        // Structure the BtG event as JSON
        String json = String.format(
            "{\"user\":\"%s\", \"patient\":\"%s\", \"reason\":\"%s\", \"category\":\"%s\", \"justification\":\"%s\"}",
            userEmail, patientName, reason, category, justification.replace("\"", "'")
        );

        // Add to blockchain ledger
        new BlockchainLedger().addRecord(json);
        System.out.println("📦 BtG access recorded in blockchain ledger.");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Input Required");
        alert.setContentText(message);
        alert.showAndWait();
    }    

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();

        ObservableList<PatientRecord> allRecords = DatabaseHelper.getAllPatientRecords();
        ObservableList<PatientRecord> filtered = FXCollections.observableArrayList();

        for (PatientRecord record : allRecords) {
            String fullName = (record.getFirstName() + " " + record.getLastName()).toLowerCase();
            String dob = record.getDateOfBirth().toLowerCase();

            if (fullName.contains(searchTerm) || dob.contains(searchTerm)) {
                filtered.add(record);
            }
        }

        patientTable.setItems(filtered);
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

    private void loadUserProfile(String email) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/database/users.db");
            PreparedStatement stmt = conn.prepareStatement("""
                SELECT first_name, last_name, email, gender, date_of_birth,
                        phone_number, preferred_contact,
                        address_line1, address_line2, postcode
                FROM users
                WHERE email = ?
            """)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                profileFirstName.setText(rs.getString("first_name"));
                profileLastName.setText(rs.getString("last_name"));
                profileEmail.setText(rs.getString("email"));
                profileGender.setText(rs.getString("gender"));
                profileDob.setText(rs.getString("date_of_birth"));
                profilePhone.setText(rs.getString("phone_number"));
                profileContact.setText(rs.getString("preferred_contact"));
                profileAddress1.setText(rs.getString("address_line1"));
                profileAddress2.setText(rs.getString("address_line2"));
                profilePostcode.setText(rs.getString("postcode"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAndShowPatientTable() {
        ObservableList<PatientRecord> data = DatabaseHelper.getAllPatientRecords();

        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        dobCol.setCellValueFactory(cellData -> {
            String rawDate = cellData.getValue().getDateOfBirth();
            try {
                LocalDate parsedDate = LocalDate.parse(rawDate); // expects yyyy-MM-dd
                String formatted = parsedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return new ReadOnlyStringWrapper(formatted);
            } catch (Exception e) {
                return new ReadOnlyStringWrapper(rawDate); // fallback to raw if parsing fails
            }
        });

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
                doctorId = DatabaseHelper.getDoctorIdByEmail(email);
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
        breakGlassButton.setVisible(false);
        breakGlassButton.setManaged(false);
    
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
    private void handleDashboardButton() {
        try {
            Main.showEPHRScreen(email, role); // Reloads MainEPHR.fxml using your Main.java logic
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    @FXML
    private void handleAppointmentsButton() {
        loadContent("/fxml/Appointments.fxml", controller -> {});
    }

    @FXML
    private void handleMedicalHistoryButton() {
        loadContent("/fxml/MedicalHistory.fxml", controller -> {
            ((MedicalHistoryController) controller).setUserContext(this.email);
            ((MedicalHistoryController) controller).setBtGState(btgGranted, btgPatientId, btgExpiryTime);
        });
    }

    @FXML
    private void handlePrescriptionsButton() {
        loadContent("/fxml/Prescriptions.fxml", controller -> {
            ((PrescriptionsController) controller).setUserContext(this.email);
            ((PrescriptionsController) controller).setBtGState(btgGranted, btgPatientId, btgExpiryTime);
        });
    }

    @FXML
    private void handleDiagnosticReportsButton() {
        loadContent("/fxml/DiagnosticReports.fxml", controller -> {
            ((DiagnosticReportsController) controller).setUserContext(this.email);
            ((DiagnosticReportsController) controller).setBtGState(btgGranted, btgPatientId, btgExpiryTime);
        });
    }

    @FXML
    private void handleAuditScreen() {
        loadContent("/fxml/BtGAuditScreen.fxml", controller -> {});
    }
    
    private void loadContent(String fxmlPath, java.util.function.Consumer<Object> controllerCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            Object controller = loader.getController();
            controllerCallback.accept(controller);
    
            contentArea.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

}