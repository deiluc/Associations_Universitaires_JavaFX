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
import javafx.util.Callback;

import java.sql.*;

public class ListAssociationsController {
    @FXML private TableView<Association> associationsTable;
    @FXML private TableColumn<Association, String> nameColumn;
    @FXML private TableColumn<Association, String> abbrevColumn;
    @FXML private TableColumn<Association, String> leaderColumn;
    @FXML private TableColumn<Association, Void> applyColumn;
    @FXML private TableColumn<Association, Void> followColumn;
    @FXML private Button backBtn;
    private String currentUserRole;
    private String currentUserEmail;
    private int currentUserId;

    public static class Association {
        private final String name;
        private final String abbreviation;
        private final String leaderEmail;
        private final String leaderName;
        private final int associationId;

        public Association(int associationId, String name, String abbreviation, String leaderEmail, String leaderName) {
            this.associationId = associationId;
            this.name = name;
            this.abbreviation = abbreviation;
            this.leaderEmail = leaderEmail;
            this.leaderName = leaderName;
        }

        public int getAssociationId() { return associationId; }
        public String getName() { return name; }
        public String getAbbreviation() { return abbreviation; }
        public String getLeaderEmail() { return leaderEmail; }
        public String getLeaderName() { return leaderName; }
    }

    public void initializeUserData(String email, String role) {
        this.currentUserEmail = email;
        this.currentUserRole = role;
        this.currentUserId = getUserId(email);
        loadAssociations();
        setupTable();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        abbrevColumn.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
        leaderColumn.setCellValueFactory(new PropertyValueFactory<>("leaderName"));

        applyColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Association, Void> call(TableColumn<Association, Void> param) {
                return new TableCell<>() {
                    private final Button button = new Button();

                    {
                        button.setOnAction(event -> {
                            Association assoc = getTableView().getItems().get(getIndex());
                            handleApplyOrWithdraw(assoc);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Association assoc = getTableView().getItems().get(getIndex());
                            boolean hasApplied = hasApplied(assoc.getAssociationId());
                            button.setText(hasApplied ? "Withdraw" : "Apply");
                            button.setStyle("-fx-font-size: 14px; -fx-padding: 8 20; -fx-background-color: #3498db; -fx-text-fill: white;");
                            setGraphic(button);
                        }
                    }
                };
            }
        });

        followColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Association, Void> call(TableColumn<Association, Void> param) {
                return new TableCell<>() {
                    private final Button button = new Button();

                    {
                        button.setOnAction(event -> {
                            Association assoc = getTableView().getItems().get(getIndex());
                            handleFollowOrUnfollow(assoc);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Association assoc = getTableView().getItems().get(getIndex());
                            boolean isFollowing = isFollowing(assoc.getAssociationId());
                            button.setText(isFollowing ? "Unfollow" : "Follow");
                            button.setStyle("-fx-font-size: 14px; -fx-padding: 8 20; -fx-background-color: #3498db; -fx-text-fill: white;");
                            setGraphic(button);
                        }
                    }
                };
            }
        });

        // Check if user is a member of any association
        boolean hasAnyMembership = false;
        String sql = "SELECT 1 FROM members WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            hasAnyMembership = stmt.executeQuery().next();
        } catch (SQLException e) {
            showAlert("Error", "Failed to check membership: " + e.getMessage());
        }

        // Set columns, excluding applyColumn if user is a member
        if (hasAnyMembership) {
            associationsTable.getColumns().setAll(nameColumn, abbrevColumn, leaderColumn, followColumn);
        } else {
            associationsTable.getColumns().setAll(nameColumn, abbrevColumn, leaderColumn, followColumn, applyColumn);
        }

        associationsTable.setRowFactory(tv -> {
            TableRow<Association> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openAssociationDetails(row.getItem());
                }
            });
            return row;
        });
    }

    public void loadAssociations() {
        ObservableList<Association> associations = FXCollections.observableArrayList();
        String sql = "SELECT a.association_id, a.name, a.abbreviation, u.email as leader_email, " +
                "CONCAT(u.first_name, ' ', u.last_name) as leader_name " +
                "FROM associations a LEFT JOIN users u ON a.leader_id = u.user_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                associations.add(new Association(
                        rs.getInt("association_id"),
                        rs.getString("name"),
                        rs.getString("abbreviation"),
                        rs.getString("leader_email"),
                        rs.getString("leader_name") != null ? rs.getString("leader_name") : "No leader"
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load associations: " + e.getMessage());
        }
        associationsTable.setItems(associations);
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
                    getLeaderId(association.getLeaderEmail()),
                    currentUserEmail,
                    currentUserRole,
                    currentUserId
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle(association.getName());
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to open association details: " + e.getMessage());
        }
    }

    private void handleApplyOrWithdraw(Association assoc) {
        int assocId = assoc.getAssociationId();
        if (hasApplied(assocId)) {
            String sql = "DELETE FROM applications WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                showAlert("Success", "Application withdrawn from " + assoc.getName());
                loadAssociations();
            } catch (SQLException e) {
                showAlert("Error", "Failed to withdraw application: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO applications (user_id, association_id, status, applied_at) VALUES (?, ?, 'PENDING', NOW())";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                addNotification(currentUserId, "Apply successful at the association " + assoc.getName());
                showAlert("Success", "Applied to " + assoc.getName());
                loadAssociations();
            } catch (SQLException e) {
                showAlert("Error", "Failed to apply: " + e.getMessage());
            }
        }
    }

    private void handleFollowOrUnfollow(Association assoc) {
        int assocId = assoc.getAssociationId();
        if (isFollowing(assocId)) {
            String sql = "DELETE FROM follows WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                showAlert("Success", "Unfollowed " + assoc.getName());
                loadAssociations();
            } catch (SQLException e) {
                showAlert("Error", "Failed to unfollow: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO follows (user_id, association_id) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                showAlert("Success", "Followed " + assoc.getName());
                loadAssociations();
            } catch (SQLException e) {
                showAlert("Error", "Failed to follow: " + e.getMessage());
            }
        }
    }

    private boolean hasApplied(int associationId) {
        String sql = "SELECT 1 FROM applications WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isFollowing(int associationId) {
        String sql = "SELECT 1 FROM follows WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isAssociationMember(int associationId) {
        String sql = "SELECT 1 FROM members WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            showAlert("Error", "Failed to check membership: " + e.getMessage());
            return false;
        }
    }

    private void addNotification(int userId, String content) {
        String sql = "INSERT INTO notifications (user_id, content, created_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, content);
            stmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Error", "Failed to add notification: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();
            HomeController controller = loader.getController();

            String fullName = getUserFullName(currentUserEmail);
            controller.initializeUserData(currentUserEmail, fullName, currentUserRole);

            Stage stage = (Stage) associationsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("UNSTPB Dashboard");
        } catch (Exception e) {
            showAlert("Error", "Failed to return to home: " + e.getMessage());
        }
    }

    private String getUserFullName(String email) {
        String sql = "SELECT CONCAT(first_name, ' ', last_name) as full_name FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("full_name") : "Unknown User";
        } catch (SQLException e) {
            return "Unknown User";
        }
    }

    private int getUserId(String email) {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to retrieve user ID: " + e.getMessage());
        }
        return -1;
    }

    private int getLeaderId(String leaderEmail) {
        if (leaderEmail == null) {
            return -1;
        }
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, leaderEmail);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to retrieve leader ID: " + e.getMessage());
        }
        return -1;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}