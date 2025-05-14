package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class HomeController {
    @FXML private Label welcomeLabel;
    @FXML private MenuItem addAsociatieItem;
    @FXML private Button addAnnouncementBtn;
    @FXML private VBox announcementsContainer;
    @FXML private Button manageEventsBtn;
    @FXML private MenuItem viewApprovedEventsItem;
    @FXML private Button manageEventStatusBtn;
    @FXML private Button notificationBtn;

    private String currentUserRole;
    private String currentUserEmail;
    private String currentUserName;

    public void initializeUserData(String email, String fullName, String role) {
        this.currentUserEmail = email;
        this.currentUserName = fullName;
        this.currentUserRole = role;

        welcomeLabel.setText("Welcome, " + fullName + "!");
        configureMenuForUserRole();
        loadAnnouncements();
        updateNotificationButton();

        addAnnouncementBtn.setVisible("admin".equals(role));
        addAnnouncementBtn.setManaged("admin".equals(role));
        manageEventsBtn.setVisible("admin".equals(role) || "professor".equals(role));
        manageEventStatusBtn.setVisible("admin".equals(role) || "professor".equals(role));
        viewApprovedEventsItem.setVisible(true);
    }

    private void configureMenuForUserRole() {
        if (addAsociatieItem != null) {
            addAsociatieItem.setVisible("admin".equals(currentUserRole));
        }
        if (viewApprovedEventsItem != null) {
            viewApprovedEventsItem.setVisible(true);
        }
    }

    private void loadAnnouncements() {
        announcementsContainer.getChildren().clear();
        String sql = "SELECT a.announcement_id, a.title, a.content, a.author_id, u.first_name, u.last_name, a.created_at " +
                "FROM announcements a JOIN users u ON a.author_id = u.user_id " +
                "ORDER BY a.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Announcement announcement = new Announcement(
                        rs.getInt("announcement_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getInt("author_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                announcementsContainer.getChildren().add(createAnnouncementCard(announcement));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load announcements: " + e.getMessage());
        }
    }

    private Node createAnnouncementCard(Announcement announcement) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-border-width: 1;");
        card.setPadding(new Insets(15));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(announcement.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if ("admin".equals(currentUserRole)) {
            MenuButton optionsMenu = new MenuButton("⋯");
            optionsMenu.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-padding: 0 5 0 5; -fx-mark-color: transparent;");

            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> handleEditAnnouncement(announcement));

            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> handleDeleteAnnouncement(announcement));

            optionsMenu.getItems().addAll(editItem, deleteItem);
            headerBox.getChildren().addAll(titleLabel, spacer, optionsMenu);
        } else {
            headerBox.getChildren().addAll(titleLabel);
        }

        Label contentLabel = new Label(announcement.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14;");

        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("Posted by: " + announcement.getAuthorName());
        authorLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        Label dateLabel = new Label(announcement.getFormattedDate());
        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        footerBox.getChildren().addAll(authorLabel, dateLabel);
        card.getChildren().addAll(headerBox, contentLabel, footerBox);
        return card;
    }

    @FXML
    private void handleAddAnnouncement() {
        Dialog<Announcement> dialog = new Dialog<>();
        dialog.setTitle("Add Announcement");
        dialog.setHeaderText("Create new announcement");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Content");
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Content:"), 0, 1);
        grid.add(contentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                int authorId = getUserId(currentUserEmail);
                return new Announcement(
                        titleField.getText(),
                        contentArea.getText(),
                        authorId,
                        currentUserName,
                        LocalDateTime.now()
                );
            }
            return null;
        });

        Optional<Announcement> result = dialog.showAndWait();
        result.ifPresent(announcement -> {
            String sql = "INSERT INTO announcements (title, content, author_id, created_at) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, announcement.getTitle());
                stmt.setString(2, announcement.getContent());
                stmt.setInt(3, announcement.getAuthorId());
                stmt.setObject(4, announcement.getDatePosted());
                stmt.executeUpdate();
                loadAnnouncements();
            } catch (SQLException e) {
                showAlert("Error", "Failed to save announcement: " + e.getMessage());
            }
        });
    }

    private void handleEditAnnouncement(Announcement announcement) {
        Dialog<Announcement> dialog = new Dialog<>();
        dialog.setTitle("Edit Announcement");
        dialog.setHeaderText("Edit announcement content");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField(announcement.getTitle());
        TextArea contentArea = new TextArea(announcement.getContent());
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Content:"), 0, 1);
        grid.add(contentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new Announcement(
                        announcement.getId(),
                        titleField.getText(),
                        contentArea.getText(),
                        announcement.getAuthorId(),
                        announcement.getAuthorName(),
                        announcement.getDatePosted()
                );
            }
            return null;
        });

        Optional<Announcement> result = dialog.showAndWait();
        result.ifPresent(updatedAnnouncement -> {
            String sql = "UPDATE announcements SET title = ?, content = ? WHERE announcement_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, updatedAnnouncement.getTitle());
                stmt.setString(2, updatedAnnouncement.getContent());
                stmt.setInt(3, updatedAnnouncement.getId());
                stmt.executeUpdate();
                loadAnnouncements();
            } catch (SQLException e) {
                showAlert("Error", "Failed to update announcement: " + e.getMessage());
            }
        });
    }

    private void handleDeleteAnnouncement(Announcement announcement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Announcement");
        alert.setHeaderText("Are you sure you want to delete this announcement?");
        alert.setContentText(announcement.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM announcements WHERE announcement_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, announcement.getId());
                stmt.executeUpdate();
                loadAnnouncements();
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete announcement: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddAsociatie() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/add-association-view.fxml"
            ));
            Parent root = loader.load();

            AddAssociationController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("Create New Association");
        } catch (Exception e) {
            showAlert("Error", "Failed to load add association page: " + e.getMessage());
        }
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
            stage.setTitle("UNSTPB Login");
        } catch (Exception e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    @FXML
    private void handleListAssociations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/list-associations-view.fxml"
            ));
            Parent root = loader.load();

            ListAssociationsController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserRole);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Associations List");
        } catch (Exception e) {
            showAlert("Error", "Could not load associations list: " + e.getMessage());
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
            int userId = getUserId(currentUserEmail);
            if (controller != null) {
                controller.initializeData(null, currentUserEmail, currentUserRole, userId, true);
            } else {
                throw new IllegalStateException("EventController is null after loading FXML");
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Manage Events");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to load event management: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/profile-view.fxml"
            ));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("My Profile");
        } catch (Exception e) {
            showAlert("Error", "Could not load profile page: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewApprovedEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/approved-events-view.fxml"
            ));
            Parent root = loader.load();
            ApprovedEventsController controller = loader.getController();
            controller.loadApprovedEvents();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Approved Events");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to load approved events view: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageEventStatus() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/event-status-view.fxml"
            ));
            Parent root = loader.load();
            EventStatusController controller = loader.getController();
            controller.loadPendingEvents();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Approve/Reject Events");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to load event status view: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/notifications-view.fxml"
            ));
            Parent root = loader.load();
            NotificationsController controller = loader.getController();
            controller.initializeData(currentUserEmail, getUserId(currentUserEmail));
            controller.loadNotifications();

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Notifications");
            stage.setOnHidden(e -> updateNotificationButton());
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to load notifications view: " + e.getMessage());
        }
    }

    private void updateNotificationButton() {
        int unreadCount = getUnreadNotificationCount();
        notificationBtn.setText("🔔" + (unreadCount > 0 ? " (" + unreadCount + ")" : ""));
    }

    private int getUnreadNotificationCount() {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, getUserId(currentUserEmail));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to count notifications: " + e.getMessage());
        }
        return 0;
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

    @FXML
    private void addUser() {
        String email = "a";
        String password = "a";
        String role = "admin";
        String firstName = "Admin";
        String lastName = "User";

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (first_name, last_name, email, password_hash, role, phone) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, passwordHash);
            stmt.setString(5, role);
            stmt.setNull(6, Types.VARCHAR);
            stmt.executeUpdate();
            showAlert("Success", "User added successfully with email: " + email);
        } catch (SQLException e) {
            showAlert("Error", "Failed to add user: " + e.getMessage());
        }
    }
}