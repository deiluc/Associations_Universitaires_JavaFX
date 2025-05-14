package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.format.DateTimeFormatter;

public class NotificationsController {
    @FXML private ListView<String> notificationsList;
    private String currentUserEmail;
    private int currentUserId;

    public void initializeData(String email, int userId) {
        this.currentUserEmail = email;
        this.currentUserId = userId;
    }

    public void loadNotifications() {
        notificationsList.getItems().clear();
        String sql = "SELECT content, created_at, is_read FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            while (rs.next()) {
                String content = rs.getString("content");
                String timestamp = rs.getTimestamp("created_at").toLocalDateTime().format(formatter);
                boolean isRead = rs.getBoolean("is_read");
                notificationsList.getItems().add((isRead ? "" : "[NEW] ") + content + " (" + timestamp + ")");
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load notifications: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkAllRead() {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.executeUpdate();
            loadNotifications();
        } catch (SQLException e) {
            showAlert("Error", "Failed to mark notifications as read: " + e.getMessage());
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