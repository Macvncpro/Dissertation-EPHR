package com.ephr;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
        } else if (DatabaseHelper.validateLogin(username, password)) {
            errorLabel.setText("");
            System.out.println("Login successful!");
            // Navigate to another screen or display dashboard
        } else {
            errorLabel.setText("Invalid username or password.");
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill out all fields to register.");
        } else if (DatabaseHelper.insertUser(username, password)) {
            errorLabel.setText("Registration successful! You can now log in.");
        } else {
            errorLabel.setText("Registration failed. Username might already exist.");
        }
    }
}