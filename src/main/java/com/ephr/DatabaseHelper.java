package com.ephr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}