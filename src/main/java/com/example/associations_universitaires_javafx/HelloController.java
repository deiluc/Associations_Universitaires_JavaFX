package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

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
        visiblePasswordField.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        User user = authenticateUser(email, password);
        if (user != null) {
            try {
                loadHomePage(user.getEmail(), user.getFullName(), user.getRole());
            } catch (Exception e) {
                statusLabel.setText("Error loading home page");
            }
        } else {
            statusLabel.setText("Invalid email or password");
        }
    }

    private User authenticateUser(String email, String password) {
        String sql = "SELECT user_id, first_name, last_name, email, password_hash, role, phone FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            storedHash,
                            rs.getString("role"),
                            rs.getString("phone")
                    );
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
        return null;
    }

    private void loadHomePage(String email, String fullName, String role) throws Exception {
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

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/register-view.fxml"
            ));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("Student Registration");
            stage.setMinWidth(700);
            stage.setMinHeight(650);
        } catch (Exception e) {
            statusLabel.setText("Could not open registration form!");
        }
    }
}