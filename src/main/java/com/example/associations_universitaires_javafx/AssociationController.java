package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

public class AssociationController {
    private static final Logger LOGGER = Logger.getLogger(AssociationController.class.getName());

    @FXML private Label associationNameLabel;
    @FXML private Label leaderLabel;
    @FXML private ListView<String> newsListView;
    @FXML private Button manageMembersBtn;
    @FXML private Button addNewsBtn;
    @FXML private Button deleteAssociationBtn;
    @FXML private ListView<String> departmentsListView;
    @FXML private Button manageEventsBtn;
    @FXML private Button viewStatsBtn;
    @FXML private ComboBox<String> eventsComboBox;
    @FXML private Button applyBtn;
    @FXML private Button followBtn;
    @FXML private Button viewApplicantsBtn;

    private String associationName;
    private String leaderEmail;
    private String currentUserEmail;
    private String currentUserRole;
    private int currentUserId;
    private int associationId;

    public void initializeData(String name, String leaderEmail, String currentUserEmail, String currentUserRole, int currentUserId) {
        if (name == null || currentUserEmail == null || currentUserRole == null) {
            showAlert("Error", "Invalid data provided for association");
            return;
        }

        this.associationName = name;
        this.leaderEmail = leaderEmail;
        this.currentUserEmail = currentUserEmail;
        this.currentUserRole = currentUserRole;
        this.currentUserId = currentUserId;
        this.associationId = getAssociationId(name);

        associationNameLabel.setText(name);
        leaderLabel.setText(leaderEmail != null ? getUserName(leaderEmail) : "No Leader");

        boolean isLeader = leaderEmail != null && currentUserEmail.equals(leaderEmail);
        boolean isAdmin = "admin".equals(currentUserRole);

        manageMembersBtn.setVisible(isLeader || isAdmin);
        addNewsBtn.setVisible(isLeader || isAdmin);
        deleteAssociationBtn.setVisible(isAdmin);
        manageEventsBtn.setVisible(isLeader || isAdmin || "professor".equals(currentUserRole));
        viewStatsBtn.setVisible(isLeader || isAdmin);
        viewApplicantsBtn.setVisible(isLeader || isAdmin);

        updateApplyButton();
        updateFollowButton();

        double parentWidth = ((VBox) departmentsListView.getParent().getParent()).getWidth();
        if (parentWidth > 0) {
            departmentsListView.setPrefWidth(parentWidth / 2 - 5);
            newsListView.setPrefWidth(parentWidth / 2 - 5);
        }

        loadDepartments();
        loadNews();
        loadEvents();
    }

    public void refreshEvents() {
        loadEvents();
    }

    private void loadEvents() {
        eventsComboBox.getItems().clear();
        String sql = "SELECT event_id, title, event_date, end_date, start_time, end_time, location " +
                "FROM events WHERE association_id = ? AND status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            LOGGER.info("Executing loadEvents for association: " + associationName);
            ResultSet rs = stmt.executeQuery();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            int count = 0;
            while (rs.next()) {
                String title = rs.getString("title");
                String dateRange = rs.getTimestamp("event_date") != null && rs.getTimestamp("end_date") != null ?
                        rs.getTimestamp("event_date").toLocalDateTime().format(dateFormatter) + " to " +
                                rs.getTimestamp("end_date").toLocalDateTime().format(dateFormatter) : "No dates";
                String timeRange = rs.getTime("start_time") != null && rs.getTime("end_time") != null ?
                        rs.getTime("start_time").toLocalTime().format(timeFormatter) + " - " +
                                rs.getTime("end_time").toLocalTime().format(timeFormatter) : "Not specified";
                String location = rs.getString("location") != null ? rs.getString("location") : "No location";
                eventsComboBox.getItems().add(title + " (" + dateRange + ", " + timeRange + ", " + location + ")");
                count++;
            }
            LOGGER.info("Loaded " + count + " approved events for association: " + associationName);
        } catch (SQLException e) {
            LOGGER.severe("Failed to load events: " + e.getMessage() + ", SQL State: " + e.getSQLState());
            showAlert("Error", "Failed to load events: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddNews() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add News");
        dialog.setHeaderText("Add news for " + associationName);
        dialog.setContentText("News content:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(content -> {
            String sql = "INSERT INTO news (association_id, content, author_id) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, associationId);
                stmt.setString(2, content);
                stmt.setInt(3, currentUserId);
                stmt.executeUpdate();
                loadNews();
                notifyFollowers("New news posted for " + associationName);
                showAlert("Success", "News added successfully!");
            } catch (SQLException e) {
                LOGGER.severe("Failed to save news: " + e.getMessage());
                showAlert("Error", "Failed to save news: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleManageMembers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/manage-members-view.fxml"
            ));
            Parent root = loader.load();

            ManageMembersController controller = loader.getController();
            Stage membersStage = new Stage();
            membersStage.initOwner(newsListView.getScene().getWindow());

            controller.initializeData(associationName, leaderEmail, currentUserEmail, membersStage);

            membersStage.setScene(new Scene(root, 600, 400));
            membersStage.setTitle("Manage Members - " + associationName);
            membersStage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load member management: " + e.getMessage());
            showAlert("Error", "Failed to load member management: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/event-view.fxml"
            ));
            Parent root = loader.load();

            EventController controller = loader.getController();
            controller.initializeData(associationName, currentUserEmail, currentUserRole, currentUserId, false);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Manage Events - " + associationName);
            stage.setOnHidden(e -> {
                refreshEvents();
                notifyFollowers("New event added for " + associationName);
            });
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load event management: " + e.getMessage());
            showAlert("Error", "Failed to load event management: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewStats() {
        showAlert("Info", "Statistics feature is under development.");
    }

    @FXML
    private void handleApply() {
        if (hasApplied()) {
            String sql = "DELETE FROM applications WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
                updateApplyButton();
                showAlert("Success", "Application withdrawn from " + associationName);
            } catch (SQLException e) {
                showAlert("Error", "Failed to withdraw application: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO applications (user_id, association_id, status, applied_at) VALUES (?, ?, 'PENDING', NOW())";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
                addNotification(currentUserId, "Apply successful at the association " + associationName);
                updateApplyButton();
                showAlert("Success", "Applied to " + associationName);
            } catch (SQLException e) {
                showAlert("Error", "Failed to apply: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleFollow() {
        if (isFollowing()) {
            String sql = "DELETE FROM follows WHERE user_id = ? AND association_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
                updateFollowButton();
                showAlert("Success", "Unfollowed " + associationName);
            } catch (SQLException e) {
                showAlert("Error", "Failed to unfollow: " + e.getMessage());
            }
        } else {
            String sql = "INSERT INTO follows (user_id, association_id) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
                updateFollowButton();
                showAlert("Success", "Followed " + associationName);
            } catch (SQLException e) {
                showAlert("Error", "Failed to follow: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewApplicants() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/view-applicants-view.fxml"
            ));
            Parent root = loader.load();

            ViewApplicantsController controller = loader.getController();
            controller.initializeData(associationName, associationId, currentUserRole, currentUserEmail, leaderEmail);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("View Applicants - " + associationName);
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load applicants view: " + e.getMessage());
            showAlert("Error", "Failed to load applicants view: " + e.getMessage());
        }
    }

    private void updateApplyButton() {
        boolean hasApplied = hasApplied();
        applyBtn.setText(hasApplied ? "Withdraw" : "Apply");
    }

    private void updateFollowButton() {
        boolean isFollowing = isFollowing();
        followBtn.setText(isFollowing ? "Unfollow" : "Follow");
    }

    private boolean hasApplied() {
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

    private boolean isFollowing() {
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

    private void notifyFollowers(String content) {
        String sql = "INSERT INTO notifications (user_id, content, created_at) " +
                "SELECT user_id, ?, NOW() FROM follows WHERE association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, content);
            stmt.setInt(2, associationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Error", "Failed to notify followers: " + e.getMessage());
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

    private int getAssociationId(String name) {
        String sql = "SELECT association_id FROM associations WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("association_id");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to retrieve association ID: " + e.getMessage());
        }
        return -1;
    }

    private void loadNews() {
        newsListView.getItems().clear();
        String sql = "SELECT n.content, u.first_name, u.last_name, n.created_at " +
                "FROM news n JOIN users u ON n.author_id = u.user_id " +
                "WHERE n.association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String content = rs.getString("content");
                String author = rs.getString("first_name") + " " + rs.getString("last_name");
                String timestamp = rs.getTimestamp("created_at").toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
                newsListView.getItems().add(content + "\nPosted by: " + author + " on " + timestamp);
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load news: " + e.getMessage());
            showAlert("Error", "Failed to load news: " + e.getMessage());
        }
    }

    private String getUserName(String email) {
        if (email == null) {
            return "No Leader";
        }
        String sql = "SELECT CONCAT(first_name, ' ', last_name) AS name FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to retrieve user name: " + e.getMessage());
            showAlert("Error", "Failed to retrieve user name: " + e.getMessage());
        }
        return "Unknown User";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
            String sql = "DELETE FROM associations WHERE name = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, associationName);
                stmt.executeUpdate();

                Stage stage = (Stage) deleteAssociationBtn.getScene().getWindow();
                refreshParentWindow();
                stage.close();

                showAlert("Success", "Association deleted successfully");
            } catch (SQLException e) {
                LOGGER.severe("Failed to delete association: " + e.getMessage());
                showAlert("Error", "Failed to delete association: " + e.getMessage());
            }
        }
    }

    private void refreshParentWindow() {
        Window window = deleteAssociationBtn.getScene().getWindow();
        if (window instanceof Stage) {
            Stage currentStage = (Stage) window;
            Window ownerWindow = currentStage.getOwner();
            if (ownerWindow instanceof Stage) {
                Stage ownerStage = (Stage) ownerWindow;
                Parent root = ownerStage.getScene().getRoot();
                Object controller = root.getProperties().get("controller");
                if (controller instanceof ListAssociationsController) {
                    ((ListAssociationsController) controller).loadAssociations();
                }
            }
        }
    }

    private void loadDepartments() {
        departmentsListView.getItems().clear();
        String sql = "SELECT name FROM departments WHERE association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                departmentsListView.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load departments: " + e.getMessage());
            showAlert("Error", "Failed to load departments: " + e.getMessage());
        }
    }
}