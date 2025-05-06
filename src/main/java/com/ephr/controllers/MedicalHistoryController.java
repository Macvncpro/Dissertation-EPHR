package com.ephr.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.ephr.models.MedicalHistory;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MedicalHistoryController {

    @FXML private TableView<MedicalHistory> historyTable;
    @FXML private TableColumn<MedicalHistory, String> conditionCol;
    @FXML private TableColumn<MedicalHistory, String> diagnosisDateCol;
    @FXML private TableColumn<MedicalHistory, String> treatmentCol;
    @FXML private TableColumn<MedicalHistory, String> statusCol;
    @FXML private TableColumn<MedicalHistory, String> severityCol;
    @FXML private TableColumn<MedicalHistory, String> notesCol;

    @FXML private TextField conditionField;
    @FXML private DatePicker diagnosisDatePicker;
    @FXML private TextField treatmentField;
    @FXML private ChoiceBox<String> statusChoiceBox;
    @FXML private ChoiceBox<String> severityChoiceBox;
    @FXML private TextArea notesArea;
    @FXML private Button addButton;
    @FXML private Label formStatusLabel;
    @FXML private ChoiceBox<String> patientChoiceBox;

    private Map<String, Integer> patientMap = new HashMap<>();
    private int doctorId = -1;

    private final String DB_URL = "jdbc:sqlite:src/main/resources/database/users.db";

    @FXML
    private void initialize() {
        // Table setup
        conditionCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCondition()));
        diagnosisDateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDiagnosisDate()));
        treatmentCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTreatment()));
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        severityCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSeverity()));
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        // Form setup
        statusChoiceBox.setItems(FXCollections.observableArrayList("active", "resolved", "inactive"));
        severityChoiceBox.setItems(FXCollections.observableArrayList("mild", "moderate", "severe"));
        statusChoiceBox.setValue("active");
        severityChoiceBox.setValue("moderate");

        loadMedicalHistory();
        loadPatientChoices();
    }

    public void setDoctorId(int id) {
        this.doctorId = id;
    }

    @FXML
    private void handleRefreshTable() {
        loadMedicalHistory();
    }

    private void loadPatientChoices() {
        patientMap.clear();
        ObservableList<String> patientNames = FXCollections.observableArrayList();

        String query = """
            SELECT p.id AS patient_id, u.first_name || ' ' || u.last_name AS name
            FROM patient p
            JOIN users u ON p.user_id = u.id
            ORDER BY name
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String name = rs.getString("name");
                patientMap.put(name, patientId);
                patientNames.add(name);
            }

            patientChoiceBox.setItems(patientNames);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMedicalHistory() {
        ObservableList<MedicalHistory> historyList = FXCollections.observableArrayList();

        String query = """
            SELECT id, patient_id, condition, diagnosis_date, treatment, status, severity, notes
            FROM medical_history
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                historyList.add(new MedicalHistory(
                    rs.getInt("id"),
                    rs.getInt("patient_id"),
                    rs.getString("condition"),
                    rs.getString("diagnosis_date"),
                    rs.getString("treatment"),
                    rs.getString("status"),
                    rs.getString("severity"),
                    rs.getString("notes")
                ));
            }

            historyTable.setItems(historyList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddHistoryEntry() {
        String condition = conditionField.getText().trim();
        LocalDate diagnosisDate = diagnosisDatePicker.getValue();
        String treatment = treatmentField.getText().trim();
        String status = statusChoiceBox.getValue();
        String severity = severityChoiceBox.getValue();
        String notes = notesArea.getText().trim();

        // Validate inputs
        if (condition.isEmpty() || diagnosisDate == null || treatment.isEmpty()
                || status == null || severity == null || doctorId == -1) {
            formStatusLabel.setText("❌ Please fill all required fields.");
            return;
        }

        String selectedPatient = patientChoiceBox.getValue();
        if (selectedPatient == null || !patientMap.containsKey(selectedPatient)) {
            formStatusLabel.setText("❌ Please select a patient.");
            return;
        }

        int patientId = patientMap.get(selectedPatient);

        System.out.println("Doctor ID = " + doctorId);

        String insert = """
            INSERT INTO medical_history (patient_id, doctor_id, condition, diagnosis_date, treatment, status, severity, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(insert)) {

            stmt.setInt(1, patientId);
            stmt.setInt(2, doctorId);
            stmt.setString(3, condition);
            stmt.setString(4, diagnosisDate.toString());
            stmt.setString(5, treatment);
            stmt.setString(6, status);
            stmt.setString(7, severity);
            stmt.setString(8, notes);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                formStatusLabel.setStyle("-fx-text-fill: green;");
                formStatusLabel.setText("✅ Entry added.");
                clearForm();
                loadMedicalHistory();
            } else {
                formStatusLabel.setStyle("-fx-text-fill: red;");
                formStatusLabel.setText("❌ Failed to insert entry.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            formStatusLabel.setStyle("-fx-text-fill: red;");
            formStatusLabel.setText("❌ Error saving entry.");
        }
    }

    private void clearForm() {
        conditionField.clear();
        diagnosisDatePicker.setValue(null);
        treatmentField.clear();
        statusChoiceBox.setValue("active");
        severityChoiceBox.setValue("moderate");
        notesArea.clear();
    }
}