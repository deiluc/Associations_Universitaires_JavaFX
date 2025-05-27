package com.example.associations_universitaires_javafx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class DepartmentChatController {
    private static final Logger LOGGER = Logger.getLogger(DepartmentChatController.class.getName());

    @FXML private Label associationNameLabel;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;

    private String associationName;
    private int associationId;
    private String currentUserEmail;
    private int currentUserId;
    private Integer currentDepartmentId;
    private Timer refreshTimer;
    private Map<String, String> departmentDescriptions;
    private String currentUserRole;

    public void initializeData(String associationName, int associationId, String currentUserEmail, int currentUserId) {
        this.associationName = associationName;
        this.associationId = associationId;
        this.currentUserEmail = currentUserEmail;
        this.currentUserId = currentUserId;
        this.departmentDescriptions = new HashMap<>();
        this.currentUserRole = getUserRole(currentUserId);

        associationNameLabel.setText(associationName);
        loadDepartments();

        // Check if user is a member or professor
        boolean isMember = isAssociationMember();
        boolean isProfAssigned = "prof".equals(currentUserRole) && isProfessorAssigned();
        boolean isAdmin = "admin".equals(currentUserRole);
        if (!isMember && !isProfAssigned && !isAdmin  ) {
            messageInput.setDisable(true);
            sendButton.setDisable(true);
            showAlert("Access Denied", "You must be a member or assigned professor of this association to participate in chats.");
        }

        // Auto-refresh messages every 5 seconds
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentDepartmentId != null) {
                        loadMessages(currentDepartmentId);
                    }
                });
            }
        }, 0, 5000);

        // Auto-scroll to bottom when new messages are added
        messageContainer.heightProperty().addListener((obs, old, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        // Load messages when department is selected
        departmentComboBox.setOnAction(event -> {
            String selectedDepartment = departmentComboBox.getSelectionModel().getSelectedItem();
            if (selectedDepartment != null) {
                currentDepartmentId = getDepartmentId(selectedDepartment);
                loadMessages(currentDepartmentId);
            }
        });

        // Set custom cell factory for ComboBox to show tooltips
        departmentComboBox.setCellFactory(listView -> new ListCell<String>() {
            private final Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    String description = departmentDescriptions.getOrDefault(item, "No description available");
                    tooltip.setText(description);
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void loadDepartments() {
        departmentComboBox.getItems().clear();
        departmentDescriptions.clear();
        String sql = "SELECT department_id, name, description FROM departments WHERE association_id = ?";
        if ("prof".equals(currentUserRole)) {
            sql = "SELECT d.department_id, d.name, d.description FROM departments d " +
                    "JOIN professor_associations pa ON d.association_id = pa.association_id " +
                    "WHERE pa.professor_id = ? AND d.association_id = ?";
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if ("prof".equals(currentUserRole)) {
                stmt.setInt(1, currentUserId);
                stmt.setInt(2, associationId);
            } else {
                stmt.setInt(1, associationId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String description = rs.getString("description") != null ? rs.getString("description") : "No description";
                departmentComboBox.getItems().add(name);
                departmentDescriptions.put(name, description);
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load departments: " + e.getMessage());
            showAlert("Error", "Failed to load departments: " + e.getMessage());
        }
    }

    private void loadMessages(int departmentId) {
        double currentScroll = scrollPane.getVvalue();
        boolean wasAtBottom = currentScroll >= 0.99;

        messageContainer.getChildren().clear();
        String sql = "SELECT cm.message_id, cm.content, cm.sent_at, u.first_name, u.last_name, cm.user_id, u.role, m.is_leader " +
                "FROM chat_messages cm " +
                "JOIN users u ON cm.user_id = u.user_id " +
                "LEFT JOIN members m ON cm.user_id = m.user_id AND m.association_id = ? " +
                "WHERE cm.department_id = ? ORDER BY cm.sent_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            stmt.setInt(2, departmentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String content = rs.getString("content");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String role = rs.getString("role");
                boolean isLeader = rs.getInt("is_leader") == 1;
                String author = firstName + " " + lastName;
                String roleLabel = "";
                if ("prof".equalsIgnoreCase(role)) {
                    roleLabel = "(Prof)";
                } else if ("admin".equalsIgnoreCase(role)) {
                    roleLabel = "(ADMIN)";
                } else if (isLeader) {
                    roleLabel = "(LEADER)";
                }
                String timestamp = rs.getTimestamp("sent_at").toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
                boolean isOwnMessage = rs.getInt("user_id") == currentUserId;
                messageContainer.getChildren().add(createMessageNode(content, author, roleLabel, timestamp, isOwnMessage));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load messages: " + e.getMessage());
            showAlert("Error", "Failed to load messages: " + e.getMessage());
        }

        if (wasAtBottom) {
            scrollPane.setVvalue(1.0);
        } else {
            scrollPane.setVvalue(currentScroll);
        }
    }

    private Node createMessageNode(String content, String author, String roleLabel, String timestamp, boolean isOwnMessage) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(8));
        messageBox.setMaxWidth(400);

        String bubbleStyle = isOwnMessage
                ? "-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-padding: 10;"
                : "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #E0E0E0; -fx-border-width: 1;";
        messageBox.setStyle(bubbleStyle);

        HBox authorBox = new HBox(5);
        Text authorText = new Text(author);
        authorText.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        authorBox.getChildren().add(authorText);
        if (!roleLabel.isEmpty()) {
            Text roleText = new Text(roleLabel);
            roleText.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-fill: #FF0000;");
            authorBox.getChildren().add(roleText);
        }

        Text timeText = new Text(timestamp);
        timeText.setStyle("-fx-font-size: 10; -fx-fill: #666;");

        Text contentText = new Text(content);
        contentText.setWrappingWidth(350);
        contentText.setStyle("-fx-font-size: 14;");

        messageBox.getChildren().addAll(authorBox, contentText, timeText);

        HBox container = new HBox();
        container.setPadding(new Insets(5));
        if (isOwnMessage) {
            container.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(messageBox, Priority.ALWAYS);
        } else {
            container.setAlignment(Pos.CENTER_LEFT);
        }
        container.getChildren().add(messageBox);

        return container;
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentDepartmentId == null) {
            showAlert("Error", "Please select a department and enter a message.");
            return;
        }

        boolean isMember = isAssociationMember();
        boolean isProfAssigned = "prof".equals(currentUserRole) && isProfessorAssigned();
        boolean isAdmin = "admin".equals(currentUserRole);
        if (!isMember && !isProfAssigned && !isAdmin  )  {
            showAlert("Error", "You are not a member or assigned professor of this association.");
            return;
        }

        String sql = "INSERT INTO chat_messages (department_id, user_id, content, sent_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentDepartmentId);
            stmt.setInt(2, currentUserId);
            stmt.setString(3, content);
            stmt.executeUpdate();
            messageInput.clear();
            loadMessages(currentDepartmentId);
        } catch (SQLException e) {
            LOGGER.severe("Failed to send message: " + e.getMessage());
            showAlert("Error", "Failed to send message: " + e.getMessage());
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
            LOGGER.severe("Failed to verify membership: " + e.getMessage());
            showAlert("Error", "Failed to verify membership: " + e.getMessage());
            return false;
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

    private int getDepartmentId(String departmentName) {
        String sql = "SELECT department_id FROM departments WHERE name = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, departmentName);
            stmt.setInt(2, associationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("department_id");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to retrieve department ID: " + e.getMessage());
            showAlert("Error", "Failed to retrieve department ID: " + e.getMessage());
        }
        return -1;
    }

    private String getUserRole(int userId) {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to get user role: " + e.getMessage());
        }
        return "user";
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void stop() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}