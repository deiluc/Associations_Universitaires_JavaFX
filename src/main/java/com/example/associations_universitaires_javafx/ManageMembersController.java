package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Optional;

public class ManageMembersController {
    @FXML private ListView<String> membersListView;
    @FXML private TextField emailField;
    @FXML private Button setLeaderBtn;

    private String associationName;
    private String leaderEmail;
    private String currentUserEmail;
    private String currentUserRole;
    private Stage stage;

    public void initializeData(String associationName, String leaderEmail, String currentUserEmail, Stage stage) {
        this.associationName = associationName;
        this.leaderEmail = leaderEmail; // Allow null
        this.currentUserEmail = currentUserEmail;
        this.stage = stage;
        this.currentUserRole = getUserRole(currentUserEmail);

        loadMembers();
        setupLeaderSelection();
    }

    private void setupLeaderSelection() {
        boolean canSetLeader = (leaderEmail != null && currentUserEmail.equals(leaderEmail)) || "admin".equals(currentUserRole);
        setLeaderBtn.setVisible(canSetLeader);
    }

    private String getUserRole(String email) {
        String sql = "SELECT role FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("role") : "user";
        } catch (SQLException e) {
            showAlert("Error", "Failed to retrieve user role: " + e.getMessage());
            return "user";
        }
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
            String sql = "UPDATE associations SET leader_id = NULL WHERE name = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, associationName);
                stmt.executeUpdate();
                this.leaderEmail = null;
                loadMembers();
                showAlert("Success", "Leader removed successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to remove leader: " + e.getMessage());
            }
        }
    }

    private void loadMembers() {
        membersListView.getItems().clear();
        String sql = "SELECT u.email, u.first_name, u.last_name FROM members m " +
                "JOIN users u ON m.user_id = u.user_id " +
                "WHERE m.association_id = (SELECT association_id FROM associations WHERE name = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, associationName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String email = rs.getString("email");
                String displayText = email.equals(leaderEmail) ? email + " ★ (Leader)" : email;
                membersListView.getItems().add(displayText);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load members: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetLeader() {
        String selected = membersListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a member to set as leader");
            return;
        }

        String newLeaderEmail = selected.replace(" ★ (Leader)", "").trim();
        if (newLeaderEmail.equals(leaderEmail)) {
            showAlert("Info", "This member is already the leader");
            return;
        }

        String sql = "UPDATE associations SET leader_id = (SELECT user_id FROM users WHERE email = ?) " +
                "WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newLeaderEmail);
            stmt.setString(2, associationName);
            stmt.executeUpdate();
            this.leaderEmail = newLeaderEmail;
            loadMembers();
            showAlert("Success", "New leader set successfully!");
        } catch (SQLException e) {
            showAlert("Error", "Failed to update leader: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddMember() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showAlert("Error", "Please enter a member's email");
            return;
        }

        if (!userExists(email)) {
            showAlert("Error", "No user found with this email");
            return;
        }

        if (isAlreadyMember(email)) {
            showAlert("Error", "This user is already a member");
            return;
        }

        String sql = "INSERT INTO members (user_id, association_id) " +
                "VALUES ((SELECT user_id FROM users WHERE email = ?), " +
                "(SELECT association_id FROM associations WHERE name = ?))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, associationName);
            stmt.executeUpdate();
            emailField.clear();
            loadMembers();
            showAlert("Success", "Member added successfully!");
        } catch (SQLException e) {
            showAlert("Error", "Failed to add member: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveMember() {
        String selected = membersListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a member to remove");
            return;
        }

        String email = selected.replace(" ★ (Leader)", "").trim();
        if (email.equals(leaderEmail)) {
            showAlert("Error", "Cannot remove the leader. Remove leader status first.");
            return;
        }

        String sql = "DELETE FROM members WHERE user_id = (SELECT user_id FROM users WHERE email = ?) " +
                "AND association_id = (SELECT association_id FROM associations WHERE name = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, associationName);
            stmt.executeUpdate();
            loadMembers();
            showAlert("Success", "Member removed successfully!");
        } catch (SQLException e) {
            showAlert("Error", "Failed to remove member: " + e.getMessage());
        }
    }

    private boolean userExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            showAlert("Error", "Failed to check user existence: " + e.getMessage());
            return false;
        }
    }

    private boolean isAlreadyMember(String email) {
        String sql = "SELECT 1 FROM members m JOIN users u ON m.user_id = u.user_id " +
                "WHERE u.email = ? AND m.association_id = " +
                "(SELECT association_id FROM associations WHERE name = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, associationName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            showAlert("Error", "Failed to check membership: " + e.getMessage());
            return false;
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