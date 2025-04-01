package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HelloController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label statusLabel;

    @FXML
    private void handleShowPassword() {
        if (showPasswordCheckBox.isSelected()) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            passwordField.setVisible(false);
        } else {
            passwordField.setText(visiblePasswordField.getText());
            visiblePasswordField.setVisible(false);
            passwordField.setVisible(true);
        }
    }

    @FXML
    public void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        User user = authenticateUser(email, password);
        if (user != null) {
            try {
                loadHomePage(user.getEmail(), user.getFirstName() + " " + user.getLastName(), user.getRole());
            } catch (IOException e) {
                statusLabel.setText("Error loading home page");
                e.printStackTrace();
            }
        } else {
            statusLabel.setText("Invalid email or password");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/register-view.fxml"
            ));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();

            // Set larger window dimensions
            stage.setScene(new Scene(root, 700, 650));  // Width: 700, Height: 650
            stage.setTitle("Student Registration");

            // Optional: Prevent window from being resized smaller than preferred size
            stage.setMinWidth(700);
            stage.setMinHeight(650);
        } catch (IOException e) {
            statusLabel.setText("Could not open registration form!");
            e.printStackTrace();
        }
    }

    private User authenticateUser(String email, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[2].equalsIgnoreCase(email)) {
                    String storedHash = parts[3];
                    if (BCrypt.checkpw(password, storedHash)) {
                        String role = parts.length > 4 ? parts[4] : "user"; // Default to "user" if role not specified
                        return new User(parts[0], parts[1], parts[2], role);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadHomePage(String email, String fullName, String role) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/associations_universitaires_javafx/home-view.fxml"
        ));
        Parent root = loader.load();

        HomeController controller = loader.getController();
        controller.initializeUserData(email, fullName, role);

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root, 700, 650));
        stage.setTitle("UNSTPB Dashboard");
    }

    private static class User {
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String role;

        public User(String firstName, String lastName, String email, String role) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.role = role;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}