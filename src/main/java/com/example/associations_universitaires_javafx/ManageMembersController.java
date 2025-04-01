package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ManageMembersController {
    @FXML private ListView<String> membersListView;
    @FXML private TextField emailField;
    @FXML private Button setLeaderBtn;

    private String associationName;
    private String leaderEmail;
    private String currentUserEmail;
    private String membersFileName;
    private String associationsFileName = "associations.txt";
    private Stage stage;

    public void initializeData(String associationName, String leaderEmail, String currentUserEmail, Stage stage) {
        this.associationName = associationName;
        this.leaderEmail = leaderEmail;
        this.currentUserEmail = currentUserEmail;
        this.stage = stage;  // Store the stage reference
        this.membersFileName = "members_" + associationName.toLowerCase().replace(" ", "_") + ".txt";

        loadMembers();
        setupLeaderSelection();
    }

    private void setupLeaderSelection() {
        // Only show set leader button for current leader or admin
        boolean canSetLeader = currentUserEmail.equals(leaderEmail) || "admin".equals(getUserRole(currentUserEmail));
        setLeaderBtn.setVisible(canSetLeader);
    }

    @FXML
    private void handleRemoveLeader() {
        if (leaderEmail == null) {
            showAlert("Error", "No leader to remove");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Leader");
        alert.setHeaderText("Are you sure you want to remove the current leader?");
        alert.setContentText("This will leave the association without a leader.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                List<String> lines = new ArrayList<>();
                boolean found = false;

                // Read all associations
                try (BufferedReader reader = new BufferedReader(new FileReader(associationsFileName))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length >= 4 && parts[0].equals(associationName)) {
                            // Clear the leader email
                            parts[3] = ""; // Empty string for no leader
                            line = String.join(":", parts);
                            found = true;
                        }
                        lines.add(line);
                    }
                }

                if (found) {
                    // Write back to file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(associationsFileName))) {
                        for (String line : lines) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }

                    // Update local leader reference
                    this.leaderEmail = null;
                    loadMembers(); // Refresh the list
                    showAlert("Success", "Leader removed successfully!");
                }
            } catch (IOException e) {
                showAlert("Error", "Failed to remove leader: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String getUserRole(String email) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 5 && parts[2].equalsIgnoreCase(email)) {
                    return parts[4];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "user";
    }

    private void loadMembers() {
        membersListView.getItems().clear();
        File file = new File(membersFileName);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Mark current leader with a star
                    String displayText = line.equals(leaderEmail) ? line + " ★ (Leader)" : line;
                    membersListView.getItems().add(displayText);
                }
            } catch (IOException e) {
                showAlert("Error", "Failed to load members: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSetLeader() {
        String selected = membersListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a member to set as leader");
            return;
        }

        // Remove the leader mark if present
        String newLeaderEmail = selected.replace(" ★ (Leader)", "").trim();

        if (newLeaderEmail.equals(leaderEmail)) {
            showAlert("Info", "This member is already the leader");
            return;
        }

        // Update the association's leader in associations.txt
        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;

            // Read all associations
            try (BufferedReader reader = new BufferedReader(new FileReader(associationsFileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 4 && parts[0].equals(associationName)) {
                        // Update the leader email
                        parts[3] = newLeaderEmail;
                        line = String.join(":", parts);
                        found = true;
                    }
                    lines.add(line);
                }
            }

            if (!found) {
                showAlert("Error", "Association not found in database");
                return;
            }

            // Write back to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(associationsFileName))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            // Update local leader reference
            this.leaderEmail = newLeaderEmail;
            loadMembers(); // Refresh the list to show new leader
            showAlert("Success", "New leader set successfully!");

        } catch (IOException e) {
            showAlert("Error", "Failed to update leader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddMember() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Error", "Please enter a member's email");
            return;
        }

        // Check if user exists in users.txt
        if (!userExists(email)) {
            showAlert("Error", "No user found with this email");
            return;
        }

        // Check if already a member
        if (membersListView.getItems().contains(email)) {
            showAlert("Error", "This user is already a member");
            return;
        }

        // Add to list and save
        membersListView.getItems().add(email);
        saveMembers();
        emailField.clear();
    }

    @FXML
    private void handleRemoveMember() {
        String selected = membersListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            membersListView.getItems().remove(selected);
            saveMembers();
        } else {
            showAlert("Error", "Please select a member to remove");
        }
    }

    private void saveMembers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(membersFileName))) {
            for (String member : membersListView.getItems()) {
                writer.write(member);
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert("Error", "Failed to save members: " + e.getMessage());
        }
    }

    private boolean userExists(String email) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[2].equalsIgnoreCase(email)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}