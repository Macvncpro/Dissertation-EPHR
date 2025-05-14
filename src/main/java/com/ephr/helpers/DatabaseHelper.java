package com.ephr.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.ephr.models.Appointment;
import com.ephr.models.DiagnosticReport;
import com.ephr.models.PatientRecord;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/users.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String getUserRoleByEmail(String email) {
        String role = null;
        String query = "SELECT role FROM users WHERE email = ?";
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                role = rs.getString("role");
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return role;
    }

    public static boolean insertNewUser(String firstName, String lastName, String email, String dob, String gender, String role) {
        String query = "INSERT INTO users (first_name, last_name, email, date_of_birth, gender, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, dob);
            stmt.setString(5, gender);
            stmt.setString(6, role);
    
            return stmt.executeUpdate() > 0;
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }    

    public static ObservableList<PatientRecord> getAllPatientRecords() {
        ObservableList<PatientRecord> list = FXCollections.observableArrayList();
    
        String query = """
            SELECT u.first_name, u.last_name, u.email, u.gender, u.date_of_birth,
                   u.phone_number, u.preferred_contact,
                   u.address_line1, u.address_line2, u.postcode,
                   p.nhs_number, p.status, p.data_sharing_consent, p.scr_consent
            FROM users u
            JOIN patient p ON u.id = p.user_id
        """;
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
    
            while (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                String gender = rs.getString("gender");
                String dob = rs.getString("date_of_birth");
                String phone = rs.getString("phone_number");
                String preferredContact = rs.getString("preferred_contact");
                String address1 = rs.getString("address_line1");
                String address2 = rs.getString("address_line2");
                String postcode = rs.getString("postcode");
    
                String nhsNumber = rs.getString("nhs_number");
                String status = rs.getString("status");
                boolean dataSharing = rs.getBoolean("data_sharing_consent");
                boolean scrConsent = rs.getBoolean("scr_consent");
    
                list.add(new PatientRecord(
                        firstName,
                        lastName,
                        email,
                        gender,
                        dob,
                        nhsNumber,
                        status,
                        dataSharing,
                        scrConsent,
                        phone,
                        preferredContact,
                        address1,
                        address2,
                        postcode
                ));
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return list;
    }

    public static ObservableList<Map<String, Object>> getPrescriptionsForPatient(int patientId) {
        ObservableList<Map<String, Object>> list = FXCollections.observableArrayList();
        String sql = "SELECT id, start_date, end_date, prescription_type, medication_id FROM prescription WHERE patient_id = ?";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("start_date", rs.getString("start_date"));
                row.put("end_date", rs.getString("end_date"));
                row.put("type", rs.getString("prescription_type"));

                // Optionally fetch medication name if you want it shown
                int medId = rs.getInt("medication_id");
                String medName = getMedicationNameById(medId);
                row.put("medication", medName);

                list.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private static String getMedicationNameById(int medId) {
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM medications WHERE id = ?")) {
            stmt.setInt(1, medId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static ObservableList<Map<String, Object>> getMedicalHistoryForPatient(int patientId) {
        ObservableList<Map<String, Object>> list = FXCollections.observableArrayList();
        String sql = "SELECT id, condition, diagnosis_date, severity FROM medical_history WHERE patient_id = ?";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("condition", EncryptionHelper.decrypt(rs.getString("condition")));
                row.put("diagnosis_date", rs.getString("diagnosis_date"));
                row.put("severity", rs.getString("severity"));
                list.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static ObservableList<Map<String, Object>> getDiagnosticReportsForPatient(int patientId) {
        ObservableList<Map<String, Object>> list = FXCollections.observableArrayList();
        String sql = "SELECT id, report_type, report_date FROM diagnostic_report WHERE patient_id = ?";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("report_type", rs.getString("report_type"));
                row.put("report_date", rs.getString("report_date"));
                list.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static ObservableList<Appointment> getAllAppointments() {
        ObservableList<Appointment> appointments = FXCollections.observableArrayList();
    
        String query = """
            SELECT
                p.id AS patient_id, 
                pu.first_name || ' ' || pu.last_name AS patient_name,
                du.first_name || ' ' || du.last_name AS doctor_name,
                a.appointment_date,
                a.status
            FROM appointment a
            JOIN patient p ON a.patient_id = p.id
            JOIN users pu ON p.user_id = pu.id
            JOIN doctor d ON a.doctor_id = d.id
            JOIN users du ON d.user_id = du.id
            ORDER BY a.appointment_date DESC
        """;
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
    
            while (rs.next()) {
                int patientId = rs.getInt("patient_id"); // ✅ correct field
                String patientName = rs.getString("patient_name");
                String doctorName = rs.getString("doctor_name");
                String dateTime = rs.getString("appointment_date");
                String[] parts = dateTime.split("T| ");
                String date = parts[0];
                String time = (parts.length > 1) ? parts[1] : "";
                String status = rs.getString("status");
    
                appointments.add(new Appointment(patientId, patientName, doctorName, date, time, status));
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return appointments;
    }

    public static boolean createAppointment(int patientId, int doctorId, String dateTime, String status, String reason, int duration, String type) {
        String query = """
            INSERT INTO appointment (patient_id, doctor_id, appointment_date, status, reason, duration_minutes, appointment_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """;
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, patientId);
            stmt.setInt(2, doctorId);
            stmt.setString(3, dateTime);
            stmt.setString(4, status);
            stmt.setString(5, reason);
            stmt.setInt(6, duration);
            stmt.setString(7, type);
    
            return stmt.executeUpdate() > 0;
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static int insertUserAndReturnId(String firstName, String lastName, String email,
                                            String dob, String gender, String role,
                                            String phone, String preferredContact,
                                            String address1, String address2, String postcode) {

        String query = """
            INSERT INTO users (
                first_name, last_name, email, date_of_birth, gender, role,
                phone_number, preferred_contact, address_line1, address_line2, postcode,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """;

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, dob);
            stmt.setString(5, gender);
            stmt.setString(6, role);
            stmt.setString(7, phone);
            stmt.setString(8, preferredContact);
            stmt.setString(9, address1);
            stmt.setString(10, address2);
            stmt.setString(11, postcode);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static boolean insertPatientDetails(int userId, String nhsNumber, String status, Integer doctorId, boolean dataSharing, boolean scrConsent) {
        String query = "INSERT INTO patient (user_id, nhs_number, status, assigned_doctor_id, data_sharing_consent, scr_consent, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, userId);
            stmt.setString(2, nhsNumber);
            stmt.setString(3, status);
            if (doctorId != null) {
                stmt.setInt(4, doctorId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setBoolean(5, dataSharing);
            stmt.setBoolean(6, scrConsent);
    
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ObservableList<DiagnosticReport> getAllDiagnosticReports() {
        ObservableList<DiagnosticReport> list = FXCollections.observableArrayList();
        String query = "SELECT * FROM diagnostic_report";

        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new DiagnosticReport(
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

        return list;
    }
    
    public static Integer getDoctorIdByEmail(String email) {
        String getUserQuery = "SELECT id FROM users WHERE email = ? AND role = 'doctor'";
        String getDoctorQuery = "SELECT id FROM doctor WHERE user_id = ?";
    
        try (Connection conn = getConnection()) {
            try (PreparedStatement userStmt = conn.prepareStatement(getUserQuery)) {
                userStmt.setString(1, email);
                ResultSet userRs = userStmt.executeQuery();
    
                if (userRs.next()) {
                    int userId = userRs.getInt("id");
    
                    try (PreparedStatement doctorStmt = conn.prepareStatement(getDoctorQuery)) {
                        doctorStmt.setInt(1, userId);
                        ResultSet doctorRs = doctorStmt.executeQuery();
    
                        if (doctorRs.next()) {
                            return doctorRs.getInt("id");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return null; // not found
    }    

    public static boolean deletePatientByEmail(String email) {
        String getUserIdQuery = "SELECT id FROM users WHERE email = ?";
        String deletePatientQuery = "DELETE FROM patient WHERE user_id = ?";
        String deleteUserQuery = "DELETE FROM users WHERE id = ?";
    
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Start transaction
    
            try (PreparedStatement getUserIdStmt = conn.prepareStatement(getUserIdQuery)) {
                getUserIdStmt.setString(1, email);
                ResultSet rs = getUserIdStmt.executeQuery();
    
                if (!rs.next()) return false; // No user found
    
                int userId = rs.getInt("id");
    
                try (
                    PreparedStatement deletePatientStmt = conn.prepareStatement(deletePatientQuery);
                    PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserQuery)
                ) {
                    deletePatientStmt.setInt(1, userId);
                    deleteUserStmt.setInt(1, userId);
    
                    deletePatientStmt.executeUpdate();
                    deleteUserStmt.executeUpdate();
    
                    conn.commit();
                    return true;
                }
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateAppointmentStatus(Appointment appt, String newStatus) {
        String query = "UPDATE appointment SET status = ?, updated_at = datetime('now') WHERE appointment_date = ? AND patient_id = (SELECT id FROM patient WHERE user_id = ?)";
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setString(1, newStatus);
            stmt.setString(2, appt.getDate() + " " + appt.getTime());
            stmt.setInt(3, appt.getPatientId());  // You'll need patientId in your Appointment model
            return stmt.executeUpdate() > 0;
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getPatientIdByEmail(String email) {
        String sql = """
            SELECT p.id FROM patient p
            JOIN users u ON p.user_id = u.id
            WHERE u.email = ?
        """;
    
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return -1; // Not found or error
    }

    public static boolean insertPatientGrantedAccess(int userId, String resourceType, int resourceId, String permission, int grantedBy) {
        String sql = "INSERT INTO access_control (user_id, resource_type, resource_id, permission, granted_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, resourceType);
            stmt.setInt(3, resourceId);
            stmt.setString(4, permission);
            stmt.setInt(5, grantedBy);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ObservableList<String> getDoctorAndNurseEmails() {
        ObservableList<String> emails = FXCollections.observableArrayList();
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE role IN ('doctor', 'nurse')");
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) emails.add(rs.getString("email"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public static int getUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // return -1 if not found or error
    }

}