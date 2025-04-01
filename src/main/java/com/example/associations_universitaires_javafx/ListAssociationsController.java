package com.example.associations_universitaires_javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ListAssociationsController {
    @FXML private TableView<Association> associationsTable;
    private String currentUserRole;
    private String currentUserEmail;

    public static class Association {
        private final String name;
        private final String abbreviation;
        private final String leaderEmail;
        private final String leaderName;

        public Association(String name, String abbreviation, String leaderEmail, String leaderName) {
            this.name = name;
            this.abbreviation = abbreviation;
            this.leaderEmail = leaderEmail;
            this.leaderName = leaderName;
        }



        public String getName() { return name; }
        public String getAbbreviation() { return abbreviation; }
        public String getLeaderEmail() { return leaderEmail; }
        public String getLeaderName() { return leaderEmail.isEmpty() ? "No leader" : leaderName;}
    }

    public void initializeUserData(String email, String role) {
        this.currentUserEmail = email;
        this.currentUserRole = role;
        loadAssociations();
        setupTable();
    }

    private void setupTable() {
        associationsTable.setRowFactory(tv -> {
            TableRow<Association> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Association association = row.getItem();
                    openAssociationDetails(association);
                }
            });
            return row;
        });
    }

    private void openAssociationDetails(Association association) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/association-view.fxml"
            ));
            Parent root = loader.load();

            AssociationController controller = loader.getController();
            controller.initializeData(
                    association.getName(),
                    association.getLeaderEmail(),
                    currentUserEmail,  // Make sure this is passed
                    currentUserRole    // Make sure this is passed
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle(association.getName());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open association details: " + e.getMessage());
        }
    }

    public void loadAssociations() {
        ObservableList<Association> associations = FXCollections.observableArrayList();

        try (BufferedReader reader = new BufferedReader(new FileReader("associations.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    String leaderName = parts[3].isEmpty() ? "No leader" : getUserName(parts[3]);
                    associations.add(new Association(parts[0], parts[1], parts[3], leaderName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        associationsTable.setItems(associations);
    }

    private String getUserName(String email) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[2].equals(email)) {
                    return parts[0] + " " + parts[1]; // First + Last name
                }
            }
        }
        return "Unknown";
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();
            HomeController controller = loader.getController();

            // Get the user's full name from users.txt
            String fullName = getUserName(currentUserEmail);
            controller.initializeUserData(currentUserEmail, fullName, currentUserRole);

            Stage stage = (Stage) associationsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("UNSTPB Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to home: " + e.getMessage());
        }
    }

    // Add this helper method if you don't have it
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}