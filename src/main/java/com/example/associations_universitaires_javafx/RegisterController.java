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

import java.io.*;

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
        // Initialize only if fields are properly injected
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
        } else {
            System.err.println("FXML injection failed - visiblePasswordField or passwordField is null");
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
        boolean upperValid = !password.equals(password.toLowerCase());
        boolean lowerValid = !password.equals(password.toUpperCase());
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
            System.err.println("One or more required fields are null");
            return;
        }

        String firstName = sanitize(firstNameField.getText());
        String lastName = sanitize(lastNameField.getText());
        String email = sanitize(emailField.getText());
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
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

        try {
            if (isEmailRegistered(email)) {
                statusLabel.setText("Email already registered!");
                return;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            saveUserToDatabase(firstName, lastName, email, hashedPassword);
            statusLabel.setText("Registration successful! Redirecting...");

            // Redirect to login after short delay
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(event -> handleBackToLogin());
            delay.play();
        } catch (IOException e) {
            if (statusLabel != null) {
                statusLabel.setText("Registration error!");
            }
            e.printStackTrace();
        }
    }

    private boolean isEmailRegistered(String email) throws IOException {
        File file = new File("users.txt");
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[2].equalsIgnoreCase(email)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveUserToDatabase(String firstName, String lastName,
                                    String email, String hashedPassword) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt", true))) {
            writer.write(String.format("%s:%s:%s:%s:user",
                    firstName, lastName, email, hashedPassword));
            writer.newLine();
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
            stage.setTitle("University Clubs Login");
        } catch (IOException e) {
            if (statusLabel != null) {
                statusLabel.setText("Error loading login page");
            }
            e.printStackTrace();
        }
    }
}