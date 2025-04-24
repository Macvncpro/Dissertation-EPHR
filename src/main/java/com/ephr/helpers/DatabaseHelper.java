package com.ephr.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.ephr.models.Patient;
import com.ephr.models.PatientRecord;
import com.ephr.models.Users;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:src/main/resources/database/users.db";

    public static String getUserRoleByEmail(String email) {
        String role = null;
        String query = "SELECT role FROM users WHERE email = ?";
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
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
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
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
                   p.nhs_number, p.status, p.data_sharing_consent, p.scr_consent
            FROM users u
            JOIN patient p ON u.id = p.user_id
        """;
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
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
                        preferredContact
                ));
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return list;
    }    

    public static int insertUserAndReturnId(String firstName, String lastName, String email,
                                            String dob, String gender, String role,
                                            String phone, String preferredContact) {
        String query = "INSERT INTO users (first_name, last_name, email, date_of_birth, gender, role, phone_number, preferred_contact, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, dob);
            stmt.setString(5, gender);
            stmt.setString(6, role);
            stmt.setString(7, phone);
            stmt.setString(8, preferredContact);

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
        String query = "INSERT INTO patient (user_id, nhs_number, status, assigned_gp_id, data_sharing_consent, scr_consent, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
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
    

    public static Integer getDoctorIdByUserId(int userId) {
        String query = "SELECT id FROM doctor WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }    

}