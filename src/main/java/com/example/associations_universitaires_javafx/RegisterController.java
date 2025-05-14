package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

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
        if (visiblePasswordField != null && passwordField != null) {
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
    }

    @FXML
    private void togglePasswordVisibility() {
        if (visiblePasswordField != null && passwordField != null) {
            boolean visible = !visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(visible);
            passwordField.setVisible(!visible);
            showPasswordBtn.setText(visible ? "🔒" : "👁");
        }
    }

    private void validatePassword(String password) {
        if (password == null) return;

        boolean lengthValid = password.length() >= 8;
        boolean upperValid = password.matches(".*[A-Z].*");
        boolean lowerValid = password.matches(".*[a-z].*");
        boolean specialValid = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (lengthReq != null) lengthReq.setFill(lengthValid ? Color.GREEN : Color.RED);
        if (upperReq != null) upperReq.setFill(upperValid ? Color.GREEN : Color.RED);
        if (lowerReq != null) lowerReq.setFill(lowerValid ? Color.GREEN : Color.RED);
        if (specialReq != null) specialReq.setFill(specialValid ? Color.GREEN : Color.RED);

        if (registerBtn != null) {
            registerBtn.setDisable(!(lengthValid && upperValid && lowerValid && specialValid));
        }
    }

    private void checkPasswordMatch() {
        if (passwordField == null || confirmPasswordField == null || passwordMatchLabel == null) return;

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
        if (firstNameField == null || lastNameField == null || emailField == null ||
                passwordField == null || confirmPasswordField == null || statusLabel == null) {
            statusLabel.setText("Form initialization error!");
            return;
        }

        String firstName = sanitize(firstNameField.getText());
        String lastName = sanitize(lastNameField.getText());
        String email = sanitize(emailField.getText());
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("All fields are required!");
            return;
        }

        if (!isValidEmail(email)) {
            statusLabel.setText("Please enter a valid email address!");
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

        try {
            if (isEmailRegistered(email)) {
                statusLabel.setText("Email already registered!");
                return;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            saveUserToDatabase(firstName, lastName, email, hashedPassword);
            statusLabel.setText("Registration successful! Redirecting...");

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(event -> handleBackToLogin());
            delay.play();
        } catch (SQLException e) {
            statusLabel.setText("Registration error: " + e.getMessage());
        }
    }

    private boolean isEmailRegistered(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private void saveUserToDatabase(String firstName, String lastName, String email, String hashedPassword) throws SQLException {
        String sql = "INSERT INTO users (first_name, last_name, email, password_hash, role) VALUES (?, ?, ?, ?, 'user')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.executeUpdate();
        }
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replace(":", "").trim();
    }

    private boolean isValidPassword(String password) {
        return password != null && password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$");
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/hello-view.fxml"
            ));
            Parent root = loader.load();
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setMinWidth(700);
            stage.setMinHeight(650);
            stage.setTitle("UNSTPB Login");
        } catch (Exception e) {
            statusLabel.setText("Error loading login page");
        }
    }
}