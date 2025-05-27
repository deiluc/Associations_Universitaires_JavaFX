package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.logging.Logger;

public class EditAssociationController {
    private static final Logger LOGGER = Logger.getLogger(EditAssociationController.class.getName());

    @FXML private TextField nameField;
    @FXML private TextField abbreviationField;
    @FXML private TextField newDepartmentField;
    @FXML private TextField editDepartmentField;
    @FXML private ListView<String> departmentsListView;
    @FXML private Button addDepartmentBtn;
    @FXML private Button editDepartmentBtn;
    @FXML private Button removeDepartmentBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private String associationName;
    private int associationId;
    private String updatedAssociationName;

    public void initializeData(String associationName, int associationId) {
        this.associationName = associationName;
        this.associationId = associationId;
        this.updatedAssociationName = associationName;
        loadAssociationDetails();
    }

    public String getUpdatedAssociationName() {
        return updatedAssociationName;
    }

    private void loadAssociationDetails() {
        String sql = "SELECT name, abbreviation FROM associations WHERE association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                abbreviationField.setText(rs.getString("abbreviation"));
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to load association details: " + e.getMessage());
            showAlert("Error", "Failed to load association details: " + e.getMessage());
        }

        loadDepartments();
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

    @FXML
    private void handleAddDepartment() {
        String newDepartment = newDepartmentField.getText().trim();
        if (newDepartment.isEmpty()) {
            showAlert("Error", "Please enter a department name.");
            return;
        }

        if (departmentsListView.getItems().contains(newDepartment)) {
            showAlert("Error", "Department already exists.");
            return;
        }

        String sql = "INSERT INTO departments (association_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            stmt.setString(2, newDepartment);
            stmt.executeUpdate();
            departmentsListView.getItems().add(newDepartment);
            newDepartmentField.clear();
            showAlert("Success", "Department added successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to add department: " + e.getMessage());
            showAlert("Error", "Failed to add department: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditDepartment() {
        String selectedDepartment = departmentsListView.getSelectionModel().getSelectedItem();
        if (selectedDepartment == null) {
            showAlert("Error", "Please select a department to edit.");
            return;
        }

        String newDepartmentName = editDepartmentField.getText().trim();
        if (newDepartmentName.isEmpty()) {
            showAlert("Error", "Please enter a new department name.");
            return;
        }

        if (departmentsListView.getItems().contains(newDepartmentName)) {
            showAlert("Error", "A department with this name already exists.");
            return;
        }

        String sql = "UPDATE departments SET name = ? WHERE association_id = ? AND name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDepartmentName);
            stmt.setInt(2, associationId);
            stmt.setString(3, selectedDepartment);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                departmentsListView.getItems().remove(selectedDepartment);
                departmentsListView.getItems().add(newDepartmentName);
                editDepartmentField.clear();
                showAlert("Success", "Department name updated successfully!");
            } else {
                showAlert("Error", "Failed to update department name.");
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to edit department: " + e.getMessage());
            showAlert("Error", "Failed to edit department: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveDepartment() {
        String selectedDepartment = departmentsListView.getSelectionModel().getSelectedItem();
        if (selectedDepartment == null) {
            showAlert("Error", "Please select a department to remove.");
            return;
        }

        String sql = "DELETE FROM departments WHERE association_id = ? AND name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, associationId);
            stmt.setString(2, selectedDepartment);
            stmt.executeUpdate();
            departmentsListView.getItems().remove(selectedDepartment);
            showAlert("Success", "Department removed successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Failed to remove department: " + e.getMessage());
            showAlert("Error", "Failed to remove department: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        String newName = nameField.getText().trim();
        String newAbbreviation = abbreviationField.getText().trim();

        if (newName.isEmpty() || newAbbreviation.isEmpty()) {
            showAlert("Error", "Name and abbreviation cannot be empty.");
            return;
        }

        String sql = "UPDATE associations SET name = ?, abbreviation = ? WHERE association_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, newAbbreviation);
            stmt.setInt(3, associationId);
            stmt.executeUpdate();
            this.updatedAssociationName = newName;
            showAlert("Success", "Association details updated successfully!");
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();
        } catch (SQLException e) {
            LOGGER.severe("Failed to update association: " + e.getMessage());
            showAlert("Error", "Failed to update association: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        loadAssociationDetails(); // Reload original details to discard changes
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}