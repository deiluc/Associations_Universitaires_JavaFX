package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.URL;


public class HelloController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        visiblePasswordField.setVisible(false);
    }

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

    private void loadHomePage(String email) throws IOException {
        // Load the home page FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/associations_universitaires_javafx/home-view.fxml"
        ));
        Parent root = loader.load();

        // Get the controller and pass the user email
        HomeController controller = loader.getController();
        controller.setUserEmail(email);

        // Get the current stage
        Stage stage = (Stage) emailField.getScene().getWindow();

        // Set the new scene
        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("University Clubs Dashboard");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (authenticateUser(email, password)) {
            try {
                loadHomePage(email);
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
        URL resourceUrl = HelloApplication.class.getResource("/com/example/associations_universitaires_javafx/register-view.fxml");
        System.out.println("Resource URL: " + resourceUrl); // <-- add this line clearly for testing purposes!

        try {
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Student Registration");
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Could not open registration form!");
        }
    }

    private boolean authenticateUser(String email, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("DataBase.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[2].equals(email) && parts[3].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadDashboard(String email) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/associations_universitaires_javafx/dashboard-view.fxml"));
        Parent root = loader.load();

        DashboardController controller = loader.getController();
        controller.setWelcomeMessage(email);

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setScene(new Scene(root, 800, 600));
    }
}