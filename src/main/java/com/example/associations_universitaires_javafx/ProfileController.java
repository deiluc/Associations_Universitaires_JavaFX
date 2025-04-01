package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    private String currentUserEmail;
    private String currentUserName;
    private String currentUserRole;

    public void initializeUserData(String email, String name, String role) {
        this.currentUserEmail = email;
        this.currentUserName = name;
        this.currentUserRole = role;

        // Initialize fields with current data
        nameField.setText(name);
        emailField.setText(email);
        // You would load phone number from your user database here
    }

    @FXML
    private void handleSaveProfile() {
        // Implement saving logic to your user database
        String newName = nameField.getText();
        String newEmail = emailField.getText();
        String newPhone = phoneField.getText();

        // Update user data in your database
        // Then return to home page
        handleBack();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();

            // Pass the (possibly updated) user data back
            HomeController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("University Clubs Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}