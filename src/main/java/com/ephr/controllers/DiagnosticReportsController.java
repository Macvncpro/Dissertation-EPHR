package com.ephr.controllers;

import com.ephr.models.DiagnosticReport;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DiagnosticReportsController {

    @FXML private TableView<DiagnosticReport> reportTable;
    @FXML private TableColumn<DiagnosticReport, String> reportTypeCol;
    @FXML private TableColumn<DiagnosticReport, String> filePathCol;
    @FXML private TableColumn<DiagnosticReport, String> reportDateCol;
    @FXML private TableColumn<DiagnosticReport, String> commentsCol;

    @FXML private ChoiceBox<String> patientChoiceBox;
    @FXML private ChoiceBox<String> doctorChoiceBox;
    @FXML private ChoiceBox<String> reportTypeChoiceBox;
    @FXML private DatePicker reportDatePicker;
    @FXML private TextArea commentsArea;
    @FXML private Label fileLabel;
    @FXML private Label statusLabel;

    private final String DB_URL = "jdbc:sqlite:src/main/resources/database/users.db";
    private final Map<String, Integer> patientMap = new HashMap<>();
    private final Map<String, Integer> doctorMap = new HashMap<>();
    private File selectedFile;

    private final ObservableList<String> allowedReportTypes = FXCollections.observableArrayList(
        "blood_test", "x_ray", "MRI", "CT", "ECG", "ultrasound", "other"
    );

    @FXML
    private void initialize() {
        reportTypeCol.setCellValueFactory(new PropertyValueFactory<>("reportType"));
        filePathCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        reportDateCol.setCellValueFactory(new PropertyValueFactory<>("reportDate"));
        commentsCol.setCellValueFactory(new PropertyValueFactory<>("comments"));

        reportTypeChoiceBox.setItems(allowedReportTypes);
        reportTypeChoiceBox.setValue("blood_test");

        loadPatientChoices();
        loadDoctorChoices();
        loadReports();
    }

    private void loadPatientChoices() {
        patientMap.clear();
        ObservableList<String> names = FXCollections.observableArrayList();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT p.id, u.first_name || ' ' || u.last_name AS name FROM patient p JOIN users u ON p.user_id = u.id");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
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
             PreparedStatement stmt = conn.prepareStatement("SELECT d.id, u.first_name || ' ' || u.last_name AS name FROM doctor d JOIN users u ON d.user_id = u.id");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                doctorMap.put(name, id);
                names.add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        doctorChoiceBox.setItems(names);
    }

    private void loadReports() {
        ObservableList<DiagnosticReport> reports = FXCollections.observableArrayList();

        String query = "SELECT * FROM diagnostic_report ORDER BY report_date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reports.add(new DiagnosticReport(
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getInt("doctor_id"),
                        rs.getString("report_type"),
                        rs.getString("file_path"),
                        rs.getString("report_date"),
                        rs.getString("comments")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        reportTable.setItems(reports);
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            fileLabel.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleOpenFile() {
        DiagnosticReport selected = reportTable.getSelectionModel().getSelectedItem();
        if (selected != null && new File(selected.getFilePath()).exists()) {
            try {
                Desktop.getDesktop().open(new File(selected.getFilePath()));
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("❌ Unable to open file.");
            }
        } else {
            statusLabel.setText("❌ File does not exist.");
        }
    }

    @FXML
    private void handleAddReport() {
        String reportType = reportTypeChoiceBox.getValue();
        LocalDate reportDate = reportDatePicker.getValue();
        String comments = commentsArea.getText();

        String patientName = patientChoiceBox.getValue();
        String doctorName = doctorChoiceBox.getValue();

        if (patientName == null || doctorName == null || selectedFile == null || reportType == null || reportDate == null) {
            statusLabel.setText("❌ Please fill all fields and choose a file.");
            return;
        }

        int patientId = patientMap.get(patientName);
        int doctorId = doctorMap.get(doctorName);

        String insert = "INSERT INTO diagnostic_report (patient_id, doctor_id, report_type, file_path, report_date, comments, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(insert)) {

            stmt.setInt(1, patientId);
            stmt.setInt(2, doctorId);
            stmt.setString(3, reportType);
            stmt.setString(4, selectedFile.getAbsolutePath());
            stmt.setString(5, reportDate.toString());
            stmt.setString(6, comments);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                statusLabel.setText("✅ Report added.");
                loadReports();
                clearForm();
            } else {
                statusLabel.setText("❌ Failed to insert report.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("❌ DB Error.");
        }
    }

    private void clearForm() {
        commentsArea.clear();
        reportDatePicker.setValue(null);
        fileLabel.setText("");
        patientChoiceBox.setValue(null);
        doctorChoiceBox.setValue(null);
        reportTypeChoiceBox.setValue("blood_test");
        selectedFile = null;
    }
}