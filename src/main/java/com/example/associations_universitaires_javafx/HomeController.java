package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    @FXML private Label welcomeLabel;
    @FXML private TextArea newsTextArea;
    @FXML private MenuItem addAsociatieItem; // Make sure this matches your FXML fx:id

    private String currentUserRole;

    // Updated method to handle user data
    public void initializeUserData(String email, String fullName, String role) {
        welcomeLabel.setText("Welcome, " + fullName + "!");
        newsTextArea.setText("Latest News:\n\n1. Welcome to our university clubs system!\n2. New events coming soon.");
        this.currentUserRole = role;

        // Set menu item visibility based on role
        configureMenuForUserRole();
    }

    private void configureMenuForUserRole() {
        if (addAsociatieItem != null) {
            // Only show for admin users
            addAsociatieItem.setVisible("admin".equals(currentUserRole));
            // MenuItems don't have setManaged(), so we just use setVisible()
        }
    }

    @FXML
    private void handleAddAsociatie() {
        // Implementation for adding new association
        System.out.println("Admin is adding new association...");
        // Add your logic here
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/hello-view.fxml"
            ));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("University Clubs Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}