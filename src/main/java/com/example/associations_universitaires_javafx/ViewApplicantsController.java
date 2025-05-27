package com.example.associations_universitaires_javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.*;

public class ViewApplicantsController {
    @FXML private Label titleLabel;
    @FXML private TableView<Applicant> applicantsTable;
    private String associationName;
    private int associationId;
    private String currentUserRole;
    private String currentUserEmail;
    private String leaderEmail;

    public static class Applicant {
        private final int userId;
        private final String name;
        private final String email;
        private final String phone;
        private final String status;

        public Applicant(int userId, String name, String email, String phone, String status) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.status = status;
        }

        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getStatus() { return status; }
    }

    public void initializeData(String associationName, int associationId, String currentUserRole, String currentUserEmail, String leaderEmail) {
        this.associationName = associationName;
        this.associationId = associationId;
        this.currentUserRole = currentUserRole;
        this.currentUserEmail = currentUserEmail;
        this.leaderEmail = leaderEmail;
        titleLabel.setText("Applicants for " + associationName);
        setupTable();
        loadApplicants();
    }

    private void setupTable() {
        TableColumn<Applicant, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Applicant, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Applicant, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Applicant, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Applicant, Void> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Applicant, Void> call(TableColumn<Applicant, Void> param) {
                return new TableCell<>() {
                    private final Button rejectBtn = new Button("Reject");
                    private final Button actionBtn = new Button();

                    {
                        rejectBtn.setStyle("-fx-font-size: 10pt; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                        actionBtn.setStyle("-fx-font-size: 10pt; -fx-background-color: #3498db; -fx-text-fill: white;");

                        rejectBtn.setOnAction(event -> {
                            Applicant applicant = getTableView().getItems().get(getIndex());
                            handleReject(applicant);
                        });

                        actionBtn.setOnAction(event -> {
                            Applicant applicant = getTableView().getItems().get(getIndex());
                            if (applicant.getStatus().equals("PENDING")) {
                                handleAcceptForInterview(applicant);
                            } else if (applicant.getStatus().equals("INTERVIEW")) {
                                handleAccept(applicant);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Applicant applicant = getTableView().getItems().get(getIndex());
                            HBox buttons = new HBox(5);
                            if (!applicant.getStatus().equals("REJECTED") && !applicant.getStatus().equals("ACCEPTED")) {
                                rejectBtn.setDisable("student".equals(currentUserRole));
                                actionBtn.setText(applicant.getStatus().equals("PENDING") ? "Accept for Interview" : "Accept");
                                actionBtn.setDisable("student".equals(currentUserRole));
                                buttons.getChildren().addAll(rejectBtn, actionBtn);
                            }
                            setGraphic(buttons.getChildren().isEmpty() ? null : buttons);
                        }
                    }
                };
            }
        });

        applicantsTable.getColumns().setAll(nameColumn, emailColumn, phoneColumn, statusColumn, actionColumn);
    }

    private void loadApplicants() {
        ObservableList<Applicant> applicants = FXCollections.observableArrayList();
        String sql = "SELECT u.user_id, CONCAT(u.first_name, ' ', u.last_name) as name, u.email, u.phone, a.status " +
                "FROM applications a JOIN users u ON a.user_id = u.user_id WHERE a.association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                applicants.add(new Applicant(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone") != null ? rs.getString("phone") : "N/A",
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load applicants: " + e.getMessage());
        }
        applicantsTable.setItems(applicants);
    }

    private void handleReject(Applicant applicant) {
        String sql = "UPDATE applications SET status = 'REJECTED', updated_at = NOW() WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicant.getUserId());
            stmt.setInt(2, associationId);
            stmt.executeUpdate();
            addNotification(applicant.getUserId(), "Rejected from " + associationName);
            loadApplicants();
            showAlert("Success", "Applicant rejected.");
        } catch (SQLException e) {
            showAlert("Error", "Failed to reject applicant: " + e.getMessage());
        }
    }

    private void handleAcceptForInterview(Applicant applicant) {
        String sql = "UPDATE applications SET status = 'INTERVIEW', updated_at = NOW() WHERE user_id = ? AND association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicant.getUserId());
            stmt.setInt(2, associationId);
            stmt.executeUpdate();
            addNotification(applicant.getUserId(), "Accepted for interview at " + associationName);
            loadApplicants();
            showAlert("Success", "Applicant accepted for interview.");
        } catch (SQLException e) {
            showAlert("Error", "Failed to accept applicant for interview: " + e.getMessage());
        }
    }

    private void handleAccept(Applicant applicant) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Update application status
            String sql = "UPDATE applications SET status = 'ACCEPTED', updated_at = NOW() WHERE user_id = ? AND association_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, applicant.getUserId());
                stmt.setInt(2, associationId);
                stmt.executeUpdate();
            }

            // Add to members table
            String memberSql = "INSERT INTO members (user_id, association_id, is_leader) VALUES (?, ?, 0)";
            try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                memberStmt.setInt(1, applicant.getUserId());
                memberStmt.setInt(2, associationId);
                memberStmt.executeUpdate();
            }

            // Notify user
            addNotification(applicant.getUserId(), "Accepted to " + associationName);

            conn.commit();
            loadApplicants();
            showAlert("Success", "Applicant accepted as member.");
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    showAlert("Error", "Rollback failed: " + rollbackEx.getMessage());
                }
            }
            showAlert("Error", "Failed to accept applicant: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    showAlert("Error", "Failed to close connection: " + closeEx.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) applicantsTable.getScene().getWindow();
        stage.close();
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}