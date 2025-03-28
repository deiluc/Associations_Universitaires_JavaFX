package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private ListView<String> clubsListView;
    @FXML private VBox clubDetailsVBox;

    public void setWelcomeMessage(String email) {
        welcomeLabel.setText("Welcome, " + email + "!");
        loadSampleClubs();
    }

    private void loadSampleClubs() {
        clubsListView.getItems().addAll(
                "Computer Science Club",
                "Robotics Club",
                "Debate Society",
                "Art Association"
        );

        clubsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showClubDetails(newVal)
        );
    }

    private void showClubDetails(String clubName) {
        clubDetailsVBox.getChildren().clear();

        if (clubName != null) {
            // Sample details - replace with actual data from your system
            clubDetailsVBox.getChildren().addAll(
                    new Label("Club: " + clubName),
                    new Label("President: John Doe"),
                    new Label("Members: 25"),
                    new Label("Next Event: Workshop on 15th May"),
                    new Label("Description: " + getClubDescription(clubName))
            );
        }
    }

    private String getClubDescription(String clubName) {
        // Replace with your actual club descriptions
        switch (clubName) {
            case "Computer Science Club":
                return "For students interested in programming and technology";
            case "Robotics Club":
                return "Building robots and competing in tournaments";
            case "Debate Society":
                return "Developing public speaking and critical thinking skills";
            case "Art Association":
                return "Exploring various forms of visual arts";
            default:
                return "No description available";
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Return to login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}