package com.example.associations_universitaires_javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.util.logging.Logger;

public class ProfAllocationController {
    private static final Logger LOGGER = Logger.getLogger(ProfAllocationController.class.getName());

    @FXML private TableView<User> profTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> associationColumn;
    @FXML private TableColumn<User, Void> actionColumn;
    @FXML private TextField departmentField;
    @FXML private TextField titleField;
    @FXML private TextField officeLocationField;
    @FXML private TextField websiteUrlField;
    @FXML private ComboBox<String> associationComboBox;
    @FXML private Button allocateBtn;
    @FXML private Button removeAllocationBtn;

    private String currentUserEmail;

    public static class User {
        private final int userId;
        private final String name;
        private final String email;
        private final String role;
        private final String associations;
        private final boolean isLeader;

        public User(int userId, String name, String email, String role, String associations, boolean isLeader) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.associations = associations;
            this.isLeader = isLeader;
        }

        public int getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getAssociations() { return associations; }
        public boolean isLeader() { return isLeader; }
    }

    public void initializeData(String currentUserEmail) {
        this.currentUserEmail = currentUserEmail;
        setupTable();
        loadAssociations();
        loadUsers();
        setupButtonActions();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        associationColumn.setCellValueFactory(new PropertyValueFactory<>("associations"));

        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button profBtn = new Button();

            {
                profBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    toggleProfRole(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user.isLeader()) {
                        profBtn.setDisable(true);
                        profBtn.setText("Leader");
                    } else {
                        profBtn.setDisable(false);
                        profBtn.setText("prof".equals(user.getRole()) ? "Remove Prof" : "Make Prof");
                    }
                    setGraphic(profBtn);
                }
            }
        });
    }

    private void loadAssociations() {
        ObservableList<String> associations = FXCollections.observableArrayList();
        String sql = "SELECT name FROM associations";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                associations.add(rs.getString("name"));
            }
            associationComboBox.setItems(associations);
        } catch (SQLException e) {
            LOGGER.severe("Failed to load associations: " + e.getMessage());
            showAlert("Error", "Failed to load associations: " + e.getMessage());
        }
    }

    private void loadUsers() {
        profTable.getItems().clear();
        String sql = "SELECT u.user_id, CONCAT(u.first_name, ' ', u.last_name) AS name, u.email, u.role, " +
                "GROUP_CONCAT(a.name) AS associations, MAX(m.is_leader) AS is_leader " +
                "FROM users u LEFT JOIN professor_associations pa ON u.user_id = pa.professor_id " +
                "LEFT JOIN associations a ON pa.association_id = a.association_id " +
                "LEFT JOIN members m ON u.user_id = m.user_id " +
                "GROUP BY u.user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                profTable.getItems().add(new User(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("associations") != null ? rs.getString("associations") : "None",
                        rs.getInt("is_leader") == 1
                ));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load users: " + e.getMessage());
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void setupButtonActions() {
        allocateBtn.setOnAction(event -> {
            User selectedUser = profTable.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                handleGlobalAllocate();
            } else {
                showAlert("Error", "Please select a user from the table!");
            }
        });
        removeAllocationBtn.setOnAction(event -> {
            User selectedUser = profTable.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                handleRemoveAllocation();
            } else {
                showAlert("Error", "Please select a user from the table!");
            }
        });

        profTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadProfessorDetails(newSelection.getUserId());
            } else {
                clearFields();
            }
        });
    }

    private void loadProfessorDetails(int userId) {
        String sql = "SELECT department, title, office_location, website_url FROM professors WHERE professor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                departmentField.setText(rs.getString("department"));
                titleField.setText(rs.getString("title"));
                officeLocationField.setText(rs.getString("office_location"));
                websiteUrlField.setText(rs.getString("website_url"));
            } else {
                clearFields();
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load professor details: " + e.getMessage());
            showAlert("Error", "Failed to load professor details: " + e.getMessage());
        }
    }

    private void handleTableAllocate(User user) {
        String association = associationComboBox.getValue();

        // Check if TextFields are initialized
        if (departmentField == null || titleField == null || officeLocationField == null || websiteUrlField == null) {
            LOGGER.severe("One or more TextFields are not initialized: " +
                    "departmentField=" + (departmentField == null ? "null" : "ok") +
                    ", titleField=" + (titleField == null ? "null" : "ok") +
                    ", officeLocationField=" + (officeLocationField == null ? "null" : "ok") +
                    ", websiteUrlField=" + (websiteUrlField == null ? "null" : "ok"));
            showAlert("Error", "Internal error: One or more input fields are not initialized.");
            return;
        }

        // Get text with null-safe handling
        String department = departmentField.getText() != null ? departmentField.getText().trim() : "";
        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String officeLocation = officeLocationField.getText() != null ? officeLocationField.getText().trim() : "";
        String websiteUrl = websiteUrlField.getText() != null ? websiteUrlField.getText().trim() : "";

        if (association == null) {
            showAlert("Error", "Please select an association!");
            return;
        }
        if (!"prof".equals(user.getRole())) {
            showAlert("Error", "User must have the professor role!");
            return;
        }

        Integer associationId = getAssociationId(association);
        if (associationId == null) {
            showAlert("Error", "Association not found!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            String checkProfSql = "SELECT 1 FROM professors WHERE professor_id = ?";
            boolean professorExists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkProfSql)) {
                checkStmt.setInt(1, user.getUserId());
                professorExists = checkStmt.executeQuery().next();
            }

            if (!professorExists) {
                String insertProfSql = "INSERT INTO professors (professor_id, department, title, office_location, website_url) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertProfSql)) {
                    insertStmt.setInt(1, user.getUserId());
                    insertStmt.setString(2, department.isEmpty() ? null : department);
                    insertStmt.setString(3, title.isEmpty() ? null : title);
                    insertStmt.setString(4, officeLocation.isEmpty() ? null : officeLocation);
                    insertStmt.setString(5, websiteUrl.isEmpty() ? null : websiteUrl);
                    insertStmt.executeUpdate();
                }
            } else {
                String updateProfSql = "UPDATE professors SET department = ?, title = ?, office_location = ?, website_url = ? WHERE professor_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateProfSql)) {
                    updateStmt.setString(1, department.isEmpty() ? null : department);
                    updateStmt.setString(2, title.isEmpty() ? null : title);
                    updateStmt.setString(3, officeLocation.isEmpty() ? null : officeLocation);
                    updateStmt.setString(4, websiteUrl.isEmpty() ? null : websiteUrl);
                    updateStmt.setInt(5, user.getUserId());
                    updateStmt.executeUpdate();
                }
            }

            String insertAssocSql = "INSERT INTO professor_associations (professor_id, association_id) VALUES (?, ?)";
            try (PreparedStatement assocStmt = conn.prepareStatement(insertAssocSql)) {
                assocStmt.setInt(1, user.getUserId());
                assocStmt.setInt(2, associationId);
                assocStmt.executeUpdate();
            }

            String addMemberSql = "INSERT INTO members (user_id, association_id, is_leader) VALUES (?, ?, 0)";
            try (PreparedStatement memberStmt = conn.prepareStatement(addMemberSql)) {
                memberStmt.setInt(1, user.getUserId());
                memberStmt.setInt(2, associationId);
                memberStmt.executeUpdate();
            }

            conn.commit();
            loadUsers();
            showAlert("Success", "Professor allocated and added as member successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to allocate professor: " + e.getMessage());
            showAlert("Error", "Failed to allocate professor: " + e.getMessage());
        }
    }

    @FXML
    private void handleGlobalAllocate() {
        User selectedUser = profTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            handleTableAllocate(selectedUser);
        } else {
            showAlert("Error", "Please select a user from the table!");
        }
    }

    @FXML
    private void handleRemoveAllocation() {
        User selectedUser = profTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            handleTableRemove(selectedUser);
        } else {
            showAlert("Error", "Please select a user from the table!");
        }
    }

    private void handleTableRemove(User user) {
        String association = associationComboBox.getValue();
        if (association == null) {
            showAlert("Error", "Please select an association!");
            return;
        }

        Integer associationId = getAssociationId(association);
        if (associationId == null) {
            showAlert("Error", "Association not found!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            String deleteAssocSql = "DELETE FROM professor_associations WHERE professor_id = ? AND association_id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteAssocSql)) {
                deleteStmt.setInt(1, user.getUserId());
                deleteStmt.setInt(2, associationId);
                deleteStmt.executeUpdate();
            }

            String removeMemberSql = "DELETE FROM members WHERE user_id = ? AND association_id = ?";
            try (PreparedStatement removeStmt = conn.prepareStatement(removeMemberSql)) {
                removeStmt.setInt(1, user.getUserId());
                removeStmt.setInt(2, associationId);
                removeStmt.executeUpdate();
            }

            conn.commit();
            loadUsers();
            showAlert("Success", "Professor allocation and membership removed successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to remove allocation: " + e.getMessage());
            showAlert("Error", "Failed to remove allocation: " + e.getMessage());
        }
    }

    private void toggleProfRole(User user) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            String updateSql = "UPDATE users SET role = ? WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, "prof".equals(user.getRole()) ? "user" : "prof");
                stmt.setInt(2, user.getUserId());
                stmt.executeUpdate();
            }
            conn.commit();
            loadUsers(); // Refresh the table to reflect the role change
            showAlert("Success", "User role updated to " + ("prof".equals(user.getRole()) ? "user" : "prof") + " successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to toggle prof role: " + e.getMessage());
            showAlert("Error", "Failed to toggle prof role: " + e.getMessage());
        }
    }

    private Integer getAssociationId(String name) {
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
        return null;
    }

    private void clearFields() {
        departmentField.clear();
        titleField.clear();
        officeLocationField.clear();
        websiteUrlField.clear();
        associationComboBox.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}