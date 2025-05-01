package com.ephr.controllers;

import com.ephr.helpers.DatabaseHelper;
import com.ephr.models.Appointment;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AppointmentsController {

    @FXML private ChoiceBox<String> statusFilterBox;
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientNameCol;
    @FXML private TableColumn<Appointment, String> doctorNameCol;
    @FXML private TableColumn<Appointment, String> dateCol;
    @FXML private TableColumn<Appointment, String> timeCol;
    @FXML private TableColumn<Appointment, String> statusCol;
    @FXML private Label statusLabel;

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