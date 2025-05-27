package com.example.associations_universitaires_javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

public class AssociationController {
    private static final Logger LOGGER = Logger.getLogger(AssociationController.class.getName());

    @FXML private Label associationNameLabel;
    @FXML private Label leaderLabel;
    @FXML private Label profCoordLabel;
    @FXML private ListView<String> newsListView;
    @FXML private MenuItem manageMembersItem;
    @FXML private MenuItem addNewsItem;
    @FXML private Button deleteAssociationBtn;
    @FXML private ListView<String> departmentsListView;
    @FXML private MenuItem manageEventsItem;
    @FXML private MenuItem viewStatsItem;
    @FXML private ComboBox<String> eventsComboBox;
    @FXML private Button applyBtn;
    @FXML private Button followBtn;
    @FXML private MenuItem viewApplicantsItem;
    @FXML private Button departmentChatsBtn;
    @FXML private MenuButton administrativeMenuBtn;
    @FXML private MenuItem editAssociationItem;
    @FXML private MenuItem profAllocationItem;

    private String associationName;
    private int leaderId;
    private String currentUserEmail;
    private String currentUserRole;
    private int currentUserId;
    private int associationId;

    private BooleanProperty isLeaderProperty = new SimpleBooleanProperty(false);
    private BooleanProperty isAdminProperty = new SimpleBooleanProperty(false);
    private BooleanProperty isProfProperty = new SimpleBooleanProperty(false);

    public void initializeData(String name, int leaderId, String currentUserEmail, String currentUserRole, int currentUserId) {
        // Add logging to debug input parameters
        LOGGER.info(String.format("Initializing AssociationController: name=%s, leaderId=%d, currentUserEmail=%s, currentUserRole=%s, currentUserId=%d",
                name, leaderId, currentUserEmail, currentUserRole, currentUserId));

        // Validate input parameters
        if (name == null || currentUserEmail == null || currentUserRole == null) {
            showAlert("Error", "Invalid data provided for association: name, email, or role is null");
            LOGGER.severe(String.format("Invalid data: name=%s, currentUserEmail=%s, currentUserRole=%s",
                    name, currentUserEmail, currentUserRole));
            return;
        }

        this.associationName = name;
        this.leaderId = leaderId;
        this.currentUserEmail = currentUserEmail;
        this.currentUserRole = currentUserRole;
        this.currentUserId = currentUserId;
        this.associationId = getAssociationId(name);

        associationNameLabel.setText(name);
        leaderLabel.setText(leaderId != 0 ? getUserName(leaderId) : "No Leader");
        loadProfCoordinator();

        // Check roles with case-insensitive comparison
        boolean isLeader = leaderId != 0 && currentUserId == leaderId;
        boolean isAdmin = currentUserRole != null && currentUserRole.trim().toLowerCase().equals("admin");
        boolean isProf = currentUserRole != null && currentUserRole.trim().toLowerCase().equals("prof") && isProfessorAssigned();

        // Log role checks
        LOGGER.info(String.format("Role checks: isLeader=%b, isAdmin=%b, isProf=%b", isLeader, isAdmin, isProf));

        isLeaderProperty.set(isLeader);
        isAdminProperty.set(isAdmin);
        isProfProperty.set(isProf);

        // Set button visibility
        administrativeMenuBtn.setVisible(isLeader || isAdmin || isProf);
        manageMembersItem.setVisible(isLeader || isAdmin);
        addNewsItem.setVisible(isLeader || isAdmin || isProf);
        deleteAssociationBtn.setVisible(isAdmin);
        manageEventsItem.setVisible(isLeader || isAdmin || isProf);
        viewStatsItem.setVisible(isLeader || isAdmin);
        viewApplicantsItem.setVisible(isLeader || isAdmin);
        editAssociationItem.setVisible(isLeader || isAdmin);
        //profAllocationItem.setVisible(isAdmin);
        departmentChatsBtn.setVisible(true);

        boolean isMember = isAssociationMember();
        applyBtn.setVisible(!isMember);
        applyBtn.setManaged(!isMember);
        if (!isMember) {
            updateApplyButton();
        }

        updateFollowButton();

        double parentWidth = ((VBox) departmentsListView.getParent().getParent()).getWidth();
        if (parentWidth > 0) {
            departmentsListView.setPrefWidth(parentWidth / 2 - 5);
            newsListView.setPrefWidth(parentWidth / 2 - 5);
        }

        // Rest of the method remains unchanged
        newsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    private MenuButton menuButton = new MenuButton("…");

                    {
                        MenuItem editItem = new MenuItem("Edit");
                        editItem.setOnAction(e -> handleEditNews(getItem()));
                        MenuItem removeItem = new MenuItem("Remove");
                        removeItem.setOnAction(e -> handleRemoveNews(getItem()));
                        menuButton.getItems().addAll(editItem, removeItem);
                        menuButton.visibleProperty().bind(Bindings.or(isLeaderProperty, isAdminProperty));
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(10);
                            Label label = new Label(item);
                            hbox.getChildren().addAll(label, menuButton);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });

        loadDepartments();
        loadNews();
        loadEvents();
    }

    private void loadProfCoordinator() {
        String sql = "SELECT u.first_name, u.last_name " +
                "FROM users u JOIN professor_associations pa ON u.user_id = pa.professor_id " +
                "WHERE pa.association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                profCoordLabel.setText(rs.getString("first_name") + " " + rs.getString("last_name"));
            } else {
                profCoordLabel.setText("No Professor Coordinator");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load professor coordinator: " + e.getMessage());
            profCoordLabel.setText("Error loading coordinator");
        }
    }

    private boolean isProfessorAssigned() {
        String sql = "SELECT 1 FROM professor_associations WHERE professor_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            LOGGER.severe("Failed to check professor assignment: " + e.getMessage());
            return false;
        }
    }

    public void refreshEvents() {
        loadEvents();
    }

    public void refreshDepartments() {
        loadDepartments();
    }

    private void loadEvents() {
        eventsComboBox.getItems().clear();
        String sql = "SELECT title, event_date, end_date, start_time, end_time, location " +
                "FROM events WHERE association_id = ? AND status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
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
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load events: " + e.getMessage());
            showAlert("Error", "Failed to load events: " + e.getMessage());
        }
    }

    private void loadNews() {
        newsListView.getItems().clear();
        String sql = "SELECT n.content, u.first_name, u.last_name, n.created_at, n.news_id " +
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

    @FXML
    private void handleAddNews() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add News");
        dialog.setHeaderText("Add news for " + associationName);
        dialog.setContentText("News content:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(content -> {
            String sql = "INSERT INTO news (association_id, content, author_id, created_at) VALUES (?, ?, ?, NOW())";
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

    private void handleEditNews(String newsItem) {
        String content = newsItem.split("\nPosted by:")[0].trim();
        TextInputDialog dialog = new TextInputDialog(content);
        dialog.setTitle("Edit News");
        dialog.setHeaderText("Edit news for " + associationName);
        dialog.setContentText("New content:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newContent -> {
            if (!newContent.equals(content)) {
                String sql = "UPDATE news SET content = ? WHERE association_id = ? AND content = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, newContent);
                    stmt.setInt(2, associationId);
                    stmt.setString(3, content);
                    stmt.executeUpdate();
                    loadNews();
                    notifyFollowers("News updated for " + associationName);
                    showAlert("Success", "News updated successfully!");
                } catch (SQLException e) {
                    LOGGER.severe("Failed to edit news: " + e.getMessage());
                    showAlert("Error", "Failed to edit news: " + e.getMessage());
                }
            }
        });
    }

    private void handleRemoveNews(String newsItem) {
        String content = newsItem.split("\nPosted by:")[0].trim();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove News");
        alert.setHeaderText("Are you sure you want to remove this news?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM news WHERE association_id = ? AND content = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, associationId);
                stmt.setString(2, content);
                stmt.executeUpdate();
                loadNews();
                notifyFollowers("News removed from " + associationName);
                showAlert("Success", "News removed successfully!");
            } catch (SQLException e) {
                LOGGER.severe("Failed to remove news: " + e.getMessage());
                showAlert("Error", "Failed to remove news: " + e.getMessage());
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
            Stage membersStage = new Stage();
            membersStage.initOwner(newsListView.getScene().getWindow());

            controller.initializeData(associationName, String.valueOf(leaderId), currentUserEmail, membersStage);

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
    private void handleEditAssociation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/edit-association-view.fxml"
            ));
            Parent root = loader.load();

            EditAssociationController controller = loader.getController();
            controller.initializeData(associationName, associationId);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("Edit Association - " + associationName);
            stage.setOnHidden(e -> {
                String newName = controller.getUpdatedAssociationName();
                if (newName != null && !newName.equals(associationName)) {
                    this.associationName = newName;
                    associationNameLabel.setText(newName);
                    this.associationId = getAssociationId(newName);
                }
                refreshDepartments();
                notifyFollowers("Association " + associationName + " details updated");
            });
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load edit association view: " + e.getMessage());
            showAlert("Error", "Failed to load edit association view: " + e.getMessage());
        }
    }

    @FXML
    private void handleProfAllocation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/prof-allocation-view.fxml"
            ));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Professor Allocation");
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load professor allocation view: " + e.getMessage());
            showAlert("Error", "Failed to load professor allocation view: " + e.getMessage());
        }
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
            String sql = "INSERT INTO applications (user_id, association_id, status, applied_at, updated_at) VALUES (?, ?, 'PENDING', NOW(), NOW())";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
                addNotification(currentUserId, "Applied successfully to " + associationName);
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
            controller.initializeData(associationName, associationId, currentUserRole, currentUserEmail, String.valueOf(leaderId));

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("View Applicants - " + associationName);
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load applicants view: " + e.getMessage());
            showAlert("Error", "Failed to load applicants view: " + e.getMessage());
        }
    }

    @FXML
    private void handleDepartmentChats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/department-chat-view.fxml"
            ));
            Parent root = loader.load();

            DepartmentChatController controller = loader.getController();
            controller.initializeData(associationName, associationId, currentUserEmail, currentUserId);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Department Chats - " + associationName);
            stage.setOnHidden(e -> controller.stop());
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to load department chats: " + e.getMessage());
            showAlert("Error", "Failed to load department chats: " + e.getMessage());
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

    private boolean isAssociationMember() {
        String sql = "SELECT 1 FROM members WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, associationId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            LOGGER.severe("Failed to check membership: " + e.getMessage());
            return false;
        }
    }

    private void notifyFollowers(String content) {
        String sql = "INSERT INTO notifications (user_id, content, created_at, is_read) " +
                "SELECT user_id, ?, NOW(), 0 FROM follows WHERE association_id = ?";
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
        String sql = "INSERT INTO notifications (user_id, content, created_at, is_read) VALUES (?, ?, NOW(), 0)";
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

    private String getUserName(int userId) {
        if (userId == 0) {
            return "No Leader";
        }
        String sql = "SELECT CONCAT(first_name, ' ', last_name) AS name FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
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