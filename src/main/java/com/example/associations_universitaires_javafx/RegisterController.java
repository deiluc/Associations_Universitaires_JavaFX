package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RegisterController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Text lengthReq;
    @FXML private Text upperReq;
    @FXML private Text lowerReq;
    @FXML private Text specialReq;
    @FXML private Button showPasswordBtn;
    @FXML private Label passwordMatchLabel;
    @FXML private Button registerBtn;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        visiblePasswordField.setVisible(false);
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePassword(newVal);
            checkPasswordMatch();
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            checkPasswordMatch();
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean visible = !visiblePasswordField.isVisible();
        visiblePasswordField.setVisible(visible);
        passwordField.setVisible(!visible);
        showPasswordBtn.setText(visible ? "🔒" : "👁");
    }

    private void validatePassword(String password) {
        boolean lengthValid = password.length() >= 8;
        boolean upperValid = !password.equals(password.toLowerCase());
        boolean lowerValid = !password.equals(password.toUpperCase());
        boolean specialValid = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        lengthReq.setFill(lengthValid ? Color.GREEN : Color.RED);
        upperReq.setFill(upperValid ? Color.GREEN : Color.RED);
        lowerReq.setFill(lowerValid ? Color.GREEN : Color.RED);
        specialReq.setFill(specialValid ? Color.GREEN : Color.RED);

        registerBtn.setDisable(!(lengthValid && upperValid && lowerValid && specialValid));
    }

    private void checkPasswordMatch() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (!confirm.isEmpty()) {
            boolean matches = password.equals(confirm);
            passwordMatchLabel.setText(matches ? "✓ Passwords match" : "✗ Passwords don't match");
            passwordMatchLabel.setTextFill(matches ? Color.GREEN : Color.RED);
        }
    }

    @FXML
    private void handleRegister() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // validate fields
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            statusLabel.setText("All fields are required!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords don't match!");
            return;
        }

        if (!isValidPassword(password)) {
            statusLabel.setText("Password criteria not met!");
            return;
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("DataBase.txt", true))) {
            bufferedWriter.write(firstName + ":" + lastName + ":" + email + ":" + password);
            bufferedWriter.newLine();
            statusLabel.setText("Successfully registered!");
        } catch (IOException e) {
            statusLabel.setText("Registration error!");
            e.printStackTrace();
        }
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$");
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("University Clubs Login");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading login page");
        }
    }
}