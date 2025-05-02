package com.ephr.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.ephr.helpers.DatabaseHelper;
import com.ephr.models.Appointment;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AppointmentsController {

    // Main Appointments Table
    @FXML private ChoiceBox<String> statusFilterBox;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientNameCol;
    @FXML private TableColumn<Appointment, String> doctorNameCol;
    @FXML private TableColumn<Appointment, String> dateCol;
    @FXML private TableColumn<Appointment, String> timeCol;
    @FXML private TableColumn<Appointment, String> statusCol;
    @FXML private Label statusLabel;

    // Add New Appointment Fields
    @FXML private ChoiceBox<String> patientChoiceBox;
    @FXML private ChoiceBox<String> doctorChoiceBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField reasonField;
    @FXML private ChoiceBox<String> typeChoiceBox;
    @FXML private Label formStatusLabel;

    private Map<String, Integer> patientMap = new HashMap<>();
    private Map<String, Integer> doctorMap = new HashMap<>();

    @FXML
    private void initialize() {

        appointmentTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Appointment appt, boolean empty) {
                super.updateItem(appt, empty);
        
                if (appt == null || empty) {
                    setStyle("");
                } else {
                    String status = appt.getStatus().toLowerCase();
                    if (status.equals("cancelled")) {
                        setStyle("-fx-background-color: #ffe6e6; -fx-text-fill: red;");
                    } else if (status.equals("completed")) {
                        setStyle("-fx-background-color: #e6ffea; -fx-text-fill: green;");
                    } else {
                        setStyle(""); // default style for scheduled etc.
                    }
                }
            }
        });
        
        statusFilterBox.setItems(FXCollections.observableArrayList("All", "Scheduled", "Cancelled", "Completed"));
        statusFilterBox.setValue("All");

        patientNameCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        doctorNameCol.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        statusFilterBox.setValue("All"); // default selection
        loadAppointments("All");

        typeChoiceBox.setItems(FXCollections.observableArrayList("face_to_face", "phone", "video"));
        typeChoiceBox.setValue("face_to_face");

        loadPatientAndDoctorChoices();
    }

    private void loadAppointments(String statusFilter) {
        ObservableList<Appointment> all = DatabaseHelper.getAllAppointments();
    
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("All")) {
            all = all.filtered(appt ->
                appt.getStatus() != null &&
                appt.getStatus().equalsIgnoreCase(statusFilter)
            );
        }
    
        appointmentTable.setItems(all);
    }
    
    private void loadPatientAndDoctorChoices() {
        ObservableList<String> patients = FXCollections.observableArrayList();
        ObservableList<String> doctors = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/database/users.db")) {
            // Patients
            PreparedStatement ps = conn.prepareStatement("""
                SELECT p.id AS patient_id, u.first_name || ' ' || u.last_name AS name
                FROM patient p JOIN users u ON p.user_id = u.id
            """);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("patient_id");
                String name = rs.getString("name");
                patientMap.put(name, id);
                patients.add(name);
            }
            rs.close(); ps.close();

            // Doctors
            ps = conn.prepareStatement("""
                SELECT d.id AS doctor_id, u.first_name || ' ' || u.last_name AS name
                FROM doctor d JOIN users u ON d.user_id = u.id
            """);
            rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("doctor_id");
                String name = rs.getString("name");
                doctorMap.put(name, id);
                doctors.add(name);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        patientChoiceBox.setItems(patients);
        doctorChoiceBox.setItems(doctors);
    }
    
    @FXML
    private void handleStatusFilter() {
        String selected = statusFilterBox.getValue();
        loadAppointments(selected);
    }

    @FXML
    private void handleNewAppointment() {
        statusLabel.setText("➕ Appointment creation not yet implemented.");
    }

    @FXML
    private void handleCreateAppointment() {
        String patientName = patientChoiceBox.getValue();
        String doctorName = doctorChoiceBox.getValue();
        String time = timeField.getText();
        LocalDate date = datePicker.getValue();
        String reason = reasonField.getText();
        String type = typeChoiceBox.getValue();

        if (patientName == null || doctorName == null || date == null || time.isBlank()) {
            formStatusLabel.setText("❌ Please fill all required fields.");
            return;
        }

        int patientId = patientMap.get(patientName);
        int doctorId = doctorMap.get(doctorName);
        String dateTime = date.toString() + " " + time;

        boolean success = DatabaseHelper.createAppointment(patientId, doctorId, dateTime, "scheduled", reason, 10, type);
        if (success) {
            formStatusLabel.setText("✅ Appointment created.");
            loadAppointments(statusFilterBox.getValue());
        } else {
            formStatusLabel.setText("❌ Failed to create appointment.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadAppointments("All");
        statusLabel.setText("🔄 Refreshed.");
    }

    @FXML
    private void handleCancel() {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("❌ Select an appointment to cancel.");
            return;
        }

        boolean updated = DatabaseHelper.updateAppointmentStatus(selected, "cancelled");
        if (updated) {
            loadAppointments(statusFilterBox.getValue());
            statusLabel.setText("✅ Appointment cancelled.");
        } else {
            statusLabel.setText("❌ Failed to cancel appointment.");
        }
    }
    
}