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
    private String currentUserRole;
    private String currentUserEmail;

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
        loadAssociations();
        setupTable();
    }

    private void setupTable() {
        TableColumn<Association, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Association, String> abbrevColumn = new TableColumn<>("Abbreviation");
        abbrevColumn.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));

        TableColumn<Association, String> leaderColumn = new TableColumn<>("Leader");
        leaderColumn.setCellValueFactory(new PropertyValueFactory<>("leaderName"));

        TableColumn<Association, Void> applyColumn = new TableColumn<>("Apply");
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
                            button.setStyle("-fx-font-size: 10pt; -fx-background-color: #3498db; -fx-text-fill: white;");
                            setGraphic(button);
                        }
                    }
                };
            }
        });

        TableColumn<Association, Void> followColumn = new TableColumn<>("Follow");
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
                            button.setStyle("-fx-font-size: 10pt; -fx-background-color: #3498db; -fx-text-fill: white;");
                            setGraphic(button);
                        }
                    }
                };
            }
        });

        associationsTable.getColumns().setAll(nameColumn, abbrevColumn, leaderColumn, applyColumn, followColumn);

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
                    association.getLeaderEmail(),
                    currentUserEmail,
                    currentUserRole,
                    getUserId(currentUserEmail)
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
        int userId = getUserId(currentUserEmail);
        int assocId = assoc.getAssociationId();
        if (hasApplied(assocId)) {
            String sql = "DELETE FROM applications WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                showAlert("Success", "Application withdrawn from " + assoc.getName());
                loadAssociations(); // Refresh table
            } catch (SQLException e) {
                showAlert("Error", "Failed to withdraw application: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO applications (user_id, association_id, status, applied_at) VALUES (?, ?, 'PENDING', NOW())";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, assocId);
                stmt.executeUpdate();
                addNotification(userId, "Apply successful at the association " + assoc.getName());
                showAlert("Success", "Applied to " + assoc.getName());
                loadAssociations();
            } catch (SQLException e) {
                showAlert("Error", "Failed to apply: " + e.getMessage());
            }
        }
    }

    private void handleFollowOrUnfollow(Association assoc) {
        int userId = getUserId(currentUserEmail);
        int assocId = assoc.getAssociationId();
        if (isFollowing(assocId)) {
            String sql = "DELETE FROM follows WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
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
                stmt.setInt(1, userId);
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
            stmt.setInt(1, getUserId(currentUserEmail));
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
            stmt.setInt(1, getUserId(currentUserEmail));
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}