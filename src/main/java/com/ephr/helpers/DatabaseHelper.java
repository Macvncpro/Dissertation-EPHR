package com.ephr.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

}