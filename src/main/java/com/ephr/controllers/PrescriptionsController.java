package com.ephr.controllers;

import com.ephr.helpers.DatabaseHelper;
import com.ephr.helpers.EncryptionHelper;
import com.ephr.models.Prescriptions;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class PrescriptionsController {

    @FXML private TableView<Prescriptions> prescriptionsTable;
    @FXML private TableColumn<Prescriptions, String> medicationCol;
    @FXML private TableColumn<Prescriptions, String> dosageCol;
    @FXML private TableColumn<Prescriptions, String> instructionsCol;
    @FXML private TableColumn<Prescriptions, String> startDateCol;
    @FXML private TableColumn<Prescriptions, String> endDateCol;
    @FXML private TableColumn<Prescriptions, String> typeCol;

    @FXML private ChoiceBox<String> patientChoiceBox;
    @FXML private ChoiceBox<String> doctorChoiceBox;
    @FXML private ChoiceBox<String> medicationChoiceBox;
    @FXML private TextField dosageField;
    @FXML private TextArea instructionsArea;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ChoiceBox<String> typeChoiceBox;
    @FXML private Label formStatusLabel;

    private final Map<String, Integer> patientMap = new HashMap<>();
    private final Map<String, Integer> doctorMap = new HashMap<>();
    private final Map<String, Integer> medicationMap = new HashMap<>();
    private final Map<Integer, String> medicationIdToName = new HashMap<>();
    private boolean btgGranted = false;
    private int allowedPatientId = -1;
    private LocalDateTime btgExpiryTime;

    private String email; // logged-in user email

    private final String DB_URL = "jdbc:sqlite:src/main/resources/database/users.db";

    @FXML
    private void initialize() {
        medicationCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                medicationIdToName.getOrDefault(data.getValue().getMedicationId(), "Unknown"))
        );

        dosageCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                decryptIfAllowed(data.getValue().getPatientId(), data.getValue().getDosage()))
        );

        instructionsCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                decryptIfAllowed(data.getValue().getPatientId(), data.getValue().getInstructions()))
        );

        startDateCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStartDate())
        );

        endDateCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getEndDate())
        );

        typeCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getType())
        );

        typeChoiceBox.setItems(FXCollections.observableArrayList("acute", "repeat"));
        typeChoiceBox.setValue("acute");

        loadPatientChoices();
        loadDoctorChoices();
        loadMedications();
        loadPrescriptions();
    }

    private String decryptIfAllowed(int patientId, String encryptedValue) {
        if (btgGranted &&
            patientId == allowedPatientId &&
            LocalDateTime.now().isBefore(btgExpiryTime)) {
            return EncryptionHelper.decrypt(encryptedValue);
        }
        return "[Protected – BtG Required]";
    }

    public void setUserContext(String email) {
        this.email = email;
    }

    private void loadPatientChoices() {
        patientMap.clear();
        ObservableList<String> names = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT p.id AS patient_id, u.first_name || ' ' || u.last_name AS name
                FROM patient p JOIN users u ON p.user_id = u.id
            """);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("patient_id");
                patientMap.put(name, id);
                names.add(name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        patientChoiceBox.setItems(names);
    }

    private void loadDoctorChoices() {
        doctorMap.clear();
        ObservableList<String> names = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT d.id AS doctor_id, u.first_name || ' ' || u.last_name AS name
                FROM doctor d JOIN users u ON d.user_id = u.id
            """);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("doctor_id");
                doctorMap.put(name, id);
                names.add(name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        doctorChoiceBox.setItems(names);
    }

    private void loadMedications() {
        medicationMap.clear();
        medicationIdToName.clear();
        ObservableList<String> meds = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM medications");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                medicationMap.put(name, id);
                medicationIdToName.put(id, name);
                meds.add(name);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        medicationChoiceBox.setItems(meds);
    }

    private void loadPrescriptions() {
        ObservableList<Prescriptions> list = FXCollections.observableArrayList();

        int currentUserId = DatabaseHelper.getUserIdByEmail(this.email);  // set this.email at controller init

        String sql = """
            SELECT p.*
            FROM prescription p
            JOIN access_control ac ON ac.resource_type = 'prescription'
                                AND ac.resource_id = p.id
                                AND ac.user_id = ?
            WHERE ac.permission = 'read'
            ORDER BY p.created_at DESC
        """;

        try (Connection conn = DatabaseHelper.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new Prescriptions(
                    rs.getInt("id"),
                    rs.getInt("patient_id"),
                    rs.getInt("doctor_id"),
                    rs.getInt("medication_id"),
                    rs.getString("dosage"),
                    rs.getString("instructions"),
                    rs.getString("start_date"),
                    rs.getString("end_date"),
                    rs.getString("prescription_type")
                ));
            }

            prescriptionsTable.setItems(list);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddPrescription() {
        String patient = patientChoiceBox.getValue();
        String doctor = doctorChoiceBox.getValue();
        String medication = medicationChoiceBox.getValue();
        String dosage = dosageField.getText();
        String instructions = instructionsArea.getText();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String type = typeChoiceBox.getValue();

        if (patient == null || doctor == null || medication == null || dosage.isBlank() || instructions.isBlank() || start == null || end == null) {
            formStatusLabel.setText("❌ Fill all required fields.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT INTO prescription (patient_id, doctor_id, medication_id, dosage, instructions, start_date, end_date, prescription_type, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
             """)) {

            stmt.setInt(1, patientMap.get(patient));
            stmt.setInt(2, doctorMap.get(doctor));
            stmt.setInt(3, medicationMap.get(medication));
            stmt.setString(4, EncryptionHelper.encrypt(dosage));
            stmt.setString(5, EncryptionHelper.encrypt(instructions));
            stmt.setString(6, start.toString());
            stmt.setString(7, end.toString());
            stmt.setString(8, type);

            stmt.executeUpdate();
            formStatusLabel.setStyle("-fx-text-fill: green;");
            formStatusLabel.setText("✅ Prescription added.");
            loadPrescriptions();

        } catch (SQLException e) {
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Error saving.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadPrescriptions();
        formStatusLabel.setText("🔄 Refreshed.");
    }

    public void setBtGState(boolean granted, int patientId, LocalDateTime expiry) {
        this.btgGranted = granted;
        this.allowedPatientId = patientId;
        this.btgExpiryTime = expiry;
    }
    
}