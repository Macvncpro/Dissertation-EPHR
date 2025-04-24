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
            SELECT u.id as user_id, u.first_name, u.last_name, u.email, u.gender, u.role, u.date_of_birth,
                p.id as patient_id, p.medical_history, p.allergies, p.insurance_provider, p.insurance_number
            FROM users u
            JOIN patient p ON u.id = p.user_id
            WHERE u.role = 'patient'
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Users user = new Users(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("gender"),
                    rs.getString("role"),
                    rs.getString("date_of_birth")
                );

                Patient details = new Patient(
                    rs.getInt("patient_id"),
                    rs.getInt("user_id"),
                    rs.getString("medical_history"),
                    rs.getString("allergies"),
                    rs.getString("insurance_provider"),
                    rs.getString("insurance_number")
                );

                list.add(new PatientRecord(user, details));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static int insertUserAndReturnId(String firstName, String lastName, String email, String dob, String gender, String role) {
        String query = "INSERT INTO users (first_name, last_name, email, date_of_birth, gender, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
    
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, dob);
            stmt.setString(5, gender);
            stmt.setString(6, role);
    
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean insertPatientDetails(int userId, String medicalHistory, String allergies, String insuranceCompany, String insuranceNumber, Integer doctorId) {
        String query = "INSERT INTO patient (user_id, medical_history, allergies, insurance_provider, insurance_number, doctor_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
    
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query)) {
    
            stmt.setInt(1, userId);
            stmt.setString(2, medicalHistory);
            stmt.setString(3, allergies);
            stmt.setString(4, insuranceCompany);
            stmt.setString(5, insuranceNumber);
            
            if (doctorId != null) {
                stmt.setInt(6, doctorId);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
    
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