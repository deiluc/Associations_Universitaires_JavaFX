package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileController {
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
    private String originalFirstName;
    private String originalLastName;
    private String originalHashedPassword;

    public void initializeUserData(String email, String fullName, String role) {
        this.currentUserEmail = email;
        this.currentUserName = fullName;
        this.currentUserRole = role;

        // Split full name into first and last names
        String[] nameParts = fullName.split(" ");
        this.originalFirstName = nameParts[0];
        this.originalLastName = nameParts.length > 1 ? nameParts[1] : "";

        // Initialize fields with current data
        firstNameField.setText(originalFirstName);
        lastNameField.setText(originalLastName);
        emailField.setText(email);
        loadOriginalPassword(); // Load the original password hash
    }

    private void loadOriginalPassword() {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[2].equals(currentUserEmail)) {
                    this.originalHashedPassword = parts[3];
                    break;
                }
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to load user data");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveProfile() {
        String newFirstName = firstNameField.getText().trim();
        String newLastName = lastNameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPhone = phoneField.getText().trim();
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate required fields
        if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty()) {
            showAlert("Error", "First name, last name and email cannot be empty!");
            return;
        }

        // Validate email format
        if (!newEmail.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Error", "Please enter a valid email address!");
            return;
        }

        // Check if password is being changed
        boolean changingPassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();
        if (changingPassword) {
            if (!BCrypt.checkpw(currentPassword, originalHashedPassword)) {
                showAlert("Error", "Current password is incorrect!");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showAlert("Error", "New passwords don't match!");
                return;
            }
            if (newPassword.length() < 8) {
                showAlert("Error", "Password must be at least 8 characters long!");
                return;
            }
        }

        try {
            // Read all users
            List<String> lines = new ArrayList<>();
            boolean userFound = false;
            boolean emailExists = false;

            try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 4 && parts[2].equals(currentUserEmail)) {
                        // Check if new email already exists (for other users)
                        if (!newEmail.equals(currentUserEmail)) {
                            for (String existingLine : lines) {
                                String[] existingParts = existingLine.split(":");
                                if (existingParts.length >= 4 && existingParts[2].equals(newEmail)) {
                                    emailExists = true;
                                    break;
                                }
                            }
                            if (emailExists) break;
                        }

                        // Update the user's record
                        String updatedPassword = changingPassword ?
                                BCrypt.hashpw(newPassword, BCrypt.gensalt()) : parts[3];

                        line = String.format("%s:%s:%s:%s:%s",
                                newFirstName,
                                newLastName,
                                newEmail,
                                updatedPassword,
                                currentUserRole);
                        userFound = true;
                        currentUserName = newFirstName + " " + newLastName;
                        currentUserEmail = newEmail;
                    }
                    lines.add(line);
                }
            }

            if (emailExists) {
                showAlert("Error", "This email is already registered!");
                return;
            }

            if (!userFound) {
                showAlert("Error", "User not found in database!");
                return;
            }

            // Write all users back to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            showAlert("Success", "Profile updated successfully!");
            handleBack();

        } catch (IOException e) {
            showAlert("Error", "Failed to update profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("UNSTPB Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
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