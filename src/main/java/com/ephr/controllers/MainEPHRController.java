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
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import com.ephr.Main;
import com.ephr.helpers.Auth0Helper;
import com.ephr.helpers.DatabaseHelper;
import com.ephr.models.PatientRecord;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class MainEPHRController {

    @FXML private Button dashboardButton;
    @FXML private Button medicalHistoryButton;
    @FXML private Button prescriptionsButton;
    @FXML private Button diagnosticReportsButton;
    @FXML private Button appointmentsButton;
    @FXML private Button logoutButton;

    @FXML private Label recordsLabel;
    @FXML private TableView<PatientRecord> patientTable;

    @FXML private Button breakGlassButton;
    private boolean btgGranted = false;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
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

    @FXML private AnchorPane contentArea;

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
            refreshButton
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
            refreshButton
        );
        setNodeVisibility(false, deleteButton);
    }
    
    private void showNurseView() {
        showDoctorView(); // reuse doctor view
        prescriptionsButton.setVisible(false);
        prescriptionsButton.setManaged(false);
    }
    
    private void showReceptionistView() {
        setNodeVisibility(
            true,
            appointmentsButton,
            searchField,
            searchButton,
            refreshButton
        );
        setNodeVisibility(false, prescriptionsButton, diagnosticReportsButton, deleteButton);
    }
    
    private void showPatientView() {
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
            patientTable
        );
    }
    
    private void setNodeVisibility(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    @FXML
    private void handleBreakGlass() {
        PatientRecord selected = patientTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("❌ Please select a patient record before using Break-the-Glass.");
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

            this.btgGranted = true;
            // logBtGAccess(email, fullName, selectedReason, category, explanation);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Access Granted");
            alert.setHeaderText("✅ Break-the-Glass Enabled");
            alert.setContentText("You may now access protected data for: " + fullName);
            alert.show();
        }
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
        loadContent("/fxml/Appointments.fxml");
    }

    @FXML
    private void handleMedicalHistoryButton() throws IOException {
        loadContent("/fxml/MedicalHistory.fxml");
    }

    @FXML
    private void handlePrescriptionsButton() {
        loadContent("/fxml/Prescriptions.fxml");
    }

    @FXML
    private void handleDiagnosticReportsButton() {
        loadContent("/fxml/DiagnosticReports.fxml");
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
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