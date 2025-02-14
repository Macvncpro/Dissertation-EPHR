package com.ephr;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Debugging to verify button press and input values
        System.out.println("Login button clicked!");
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        // Authenticate user using the database
        boolean isAuthenticated = DatabaseHelper.authenticateUser(username, password);

        if (isAuthenticated) {
            // Redirect to the main EPHR screen
            System.out.println("Login successful! Redirecting...");
            Main.showEPHRScreen();
        } else {
            // Display error message
            errorLabel.setText("Invalid username or password.");
        }
    }
}