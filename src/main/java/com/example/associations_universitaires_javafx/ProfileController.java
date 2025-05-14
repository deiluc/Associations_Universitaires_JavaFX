package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ProfileController {
    private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private String currentUserEmail;
    private String currentUserName;
    private String currentUserRole;
    private String currentPasswordHash;
    private String currentFirstName;
    private String currentLastName;
    private String currentPhone;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    public void initializeUserData(String email, String fullName, String role) {
        this.currentUserEmail = email;
        this.currentUserName = fullName;
        this.currentUserRole = role;

        logFieldInitializationStatus("firstNameField", firstNameField);
        logFieldInitializationStatus("lastNameField", lastNameField);
        logFieldInitializationStatus("emailField", emailField);
        logFieldInitializationStatus("phoneField", phoneField);
        logFieldInitializationStatus("currentPasswordField", currentPasswordField);
        logFieldInitializationStatus("newPasswordField", newPasswordField);
        logFieldInitializationStatus("confirmPasswordField", confirmPasswordField);

        if (firstNameField == null || lastNameField == null || emailField == null ||
                phoneField == null || currentPasswordField == null || newPasswordField == null ||
                confirmPasswordField == null) {
            LOGGER.severe("One or more FXML fields are not initialized. Aborting initialization.");
            showAlert("Error", "Application error: UI components not properly initialized. Please check the FXML file.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT first_name, last_name, phone, password_hash FROM users WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentFirstName = rs.getString("first_name");
                    currentLastName = rs.getString("last_name");
                    firstNameField.setText(currentFirstName != null ? currentFirstName : "");
                    lastNameField.setText(currentLastName != null ? currentLastName : "");
                    emailField.setText(email != null ? email : "");
                    currentPhone = rs.getString("phone");
                    phoneField.setText(currentPhone != null ? currentPhone : "");
                    this.currentPasswordHash = rs.getString("password_hash");
                } else {
                    LOGGER.warning("No user found with email: " + email);
                    showAlert("Error", "User data not found.");
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load user data: " + e.getMessage());
            showAlert("Error", "Failed to load user data: " + e.getMessage());
        }
    }

    private void logFieldInitializationStatus(String fieldName, Object field) {
        if (field == null) {
            LOGGER.severe("Field " + fieldName + " is null during initialization.");
        } else {
            LOGGER.info("Field " + fieldName + " initialized successfully.");
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (firstNameField == null || lastNameField == null || emailField == null || phoneField == null ||
                currentPasswordField == null || newPasswordField == null || confirmPasswordField == null) {
            LOGGER.severe("One or more FXML fields are null in handleSaveProfile");
            showAlert("Error", "Application error: UI components not properly initialized.");
            return;
        }

        String newFirstName = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String newLastName = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        String newEmail = emailField.getText() != null ? emailField.getText().trim() : "";
        String newPhone = phoneField.getText() != null ? phoneField.getText().trim() : "";

        if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty()) {
            showAlert("Error", "First name, last name, and email cannot be empty!");
            return;
        }

        // Only validate email if it has changed
        if (!newEmail.equals(currentUserEmail) && !EMAIL_PATTERN.matcher(newEmail).matches()) {
            showAlert("Error", "Please enter a valid email address!");
            return;
        }

        boolean changingPassword = !newPasswordField.getText().isEmpty();
        if (changingPassword) {
            String currentPassword = currentPasswordField.getText();
            if (currentPassword == null || !BCrypt.checkpw(currentPassword, currentPasswordHash)) {
                showAlert("Error", "Current password is incorrect!");
                return;
            }
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            if (!newPassword.equals(confirmPassword)) {
                showAlert("Error", "New passwords don't match!");
                return;
            }
            if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$")) {
                showAlert("Error", "New password must be at least 8 characters with uppercase, lowercase, and special characters!");
                return;
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder updateSql = new StringBuilder("UPDATE users SET ");
            boolean hasUpdates = false;
            List<Object> params = new ArrayList<>();
            int paramIndex = 1;

            if (!newFirstName.equals(currentFirstName)) {
                updateSql.append("first_name = ?, ");
                params.add(newFirstName);
                hasUpdates = true;
            }
            if (!newLastName.equals(currentLastName)) {
                updateSql.append("last_name = ?, ");
                params.add(newLastName);
                hasUpdates = true;
            }
            if (!newEmail.equals(currentUserEmail)) {
                String checkSql = "SELECT 1 FROM users WHERE email = ? AND email != ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, newEmail);
                    stmt.setString(2, currentUserEmail);
                    if (stmt.executeQuery().next()) {
                        showAlert("Error", "This email is already registered!");
                        return;
                    }
                }
                updateSql.append("email = ?, ");
                params.add(newEmail);
                hasUpdates = true;
            }
            if (changingPassword) {
                updateSql.append("password_hash = ?, ");
                params.add(BCrypt.hashpw(newPasswordField.getText(), BCrypt.gensalt()));
                hasUpdates = true;
            }
            if (!newPhone.equals(currentPhone)) {
                updateSql.append("phone = ?, ");
                params.add(newPhone);
                hasUpdates = true;
            }

            if (!hasUpdates) {
                showAlert("Info", "No changes detected to update.");
                return;
            }

            // Remove the trailing ", " and add WHERE clause
            updateSql.setLength(updateSql.length() - 2);
            updateSql.append(" WHERE email = ?");
            params.add(currentUserEmail);

            try (PreparedStatement stmt = conn.prepareStatement(updateSql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    if (!newFirstName.equals(currentFirstName)) currentFirstName = newFirstName;
                    if (!newLastName.equals(currentLastName)) currentLastName = newLastName;
                    if (!newEmail.equals(currentUserEmail)) currentUserEmail = newEmail;
                    if (!newPhone.equals(currentPhone)) currentPhone = newPhone;
                    if (changingPassword) currentPasswordHash = BCrypt.hashpw(newPasswordField.getText(), BCrypt.gensalt());
                    this.currentUserName = currentFirstName + " " + currentLastName;

                    showAlert("Success", "Profile updated successfully!");
                    handleBack();
                } else {
                    showAlert("Error", "Failed to update profile!");
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to update profile: " + e.getMessage());
            showAlert("Error", "Failed to update profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"));
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("UNSTPB Dashboard");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to home: " + e.getMessage());
            showAlert("Error", "Failed to return to home: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}