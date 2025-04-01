package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssociationController {
    @FXML private Label associationNameLabel;
    @FXML private Label leaderLabel;
    @FXML private ListView<String> newsListView;
    @FXML private Button manageMembersBtn;
    @FXML private Button addNewsBtn;
    @FXML private Button deleteAssociationBtn;

    private String associationName;
    private String leaderEmail;
    private String currentUserEmail;
    private String currentUserRole;

    public void initializeData(String name, String leaderEmail, String currentUserEmail, String currentUserRole) {
        if (name == null || leaderEmail == null || currentUserEmail == null || currentUserRole == null) {
            showAlert("Error", "Invalid data provided for association");
            return;
        }

        this.associationName = name;
        this.leaderEmail = leaderEmail;
        this.currentUserEmail = currentUserEmail;
        this.currentUserRole = currentUserRole;

        associationNameLabel.setText(name);
        leaderLabel.setText(getUserName(leaderEmail));

        // Show management buttons
        boolean isLeader = currentUserEmail.equals(leaderEmail);
        boolean isAdmin = "admin".equals(currentUserRole);

        manageMembersBtn.setVisible(isLeader || isAdmin);
        addNewsBtn.setVisible(isLeader || isAdmin);
        deleteAssociationBtn.setVisible(isAdmin);  // Only show for admin
    }
    @FXML
    private void handleAddNews() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add News");
        dialog.setHeaderText("Add news for " + associationName);
        dialog.setContentText("News content:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            String newsContent = result.get();
            String author = getUserName(currentUserEmail);
            String timestamp = java.time.LocalDateTime.now().toString();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("association_news.txt", true))) {
                // Write to association news file
                writer.write(String.format("%s|%s|%s|%s%n",
                        associationName,
                        newsContent,
                        author,
                        timestamp));

                // Write to home page news file
                try (BufferedWriter homeWriter = new BufferedWriter(new FileWriter("home_news.txt", true))) {
                    homeWriter.write(String.format("%s|%s|%s|%s|%s%n",
                            "ASSOCIATION_NEWS",
                            associationName,
                            newsContent,
                            author,
                            timestamp));
                }

                loadNews(); // Refresh the news list
            } catch (IOException e) {
                showAlert("Error", "Failed to save news: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleManageMembers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/manage-members-view.fxml"
            ));
            Parent root = loader.load();

            ManageMembersController controller = loader.getController();

            // Get the current stage from any existing node (like newsListView)
            Stage currentStage = (Stage) newsListView.getScene().getWindow();

            // Create new stage for members management
            Stage membersStage = new Stage();
            membersStage.initOwner(currentStage); // Set owner for modal behavior

            controller.initializeData(associationName, leaderEmail, currentUserEmail, membersStage);

            membersStage.setScene(new Scene(root, 600, 400));
            membersStage.setTitle("Manage Members - " + associationName);
            membersStage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to load member management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadNews() {
        try (BufferedReader reader = new BufferedReader(new FileReader("association_news.txt"))) {
            newsListView.getItems().clear();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4 && parts[0].equals(associationName)) {
                    newsListView.getItems().add(parts[1] + "\nPosted by: " + parts[2] + " on " + parts[3]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getUserName(String email) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[2].equals(email)) {
                    return parts[0] + " " + parts[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown User";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDeleteAssociation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Association");
        alert.setHeaderText("Are you sure you want to delete this association?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete from associations file
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader("associations.txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length > 0 && !parts[0].equals(associationName)) {
                            lines.add(line);
                        }
                    }
                }

                // Write back to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("associations.txt"))) {
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }

                // Close the window and refresh parent
                Stage stage = (Stage) deleteAssociationBtn.getScene().getWindow();
                refreshParentWindow();
                stage.close();

                showAlert("Success", "Association deleted successfully");
            } catch (IOException e) {
                showAlert("Error", "Failed to delete association: " + e.getMessage());
            }
        }
    }

    private void refreshParentWindow() {
        // Get the current window
        Window window = deleteAssociationBtn.getScene().getWindow();

        // Check if this window has an owner (parent window)
        if (window != null && window instanceof Stage) {
            Stage currentStage = (Stage) window;
            Window ownerWindow = currentStage.getOwner();

            // If there's an owner window and it's a Stage
            if (ownerWindow != null && ownerWindow instanceof Stage) {
                Stage ownerStage = (Stage) ownerWindow;
                Parent root = ownerStage.getScene().getRoot();

                // Get the controller from the root pane's properties
                Object controller = root.getProperties().get("controller");
                if (controller instanceof ListAssociationsController) {
                    ((ListAssociationsController) controller).loadAssociations();
                }
            }
        }
    }
}