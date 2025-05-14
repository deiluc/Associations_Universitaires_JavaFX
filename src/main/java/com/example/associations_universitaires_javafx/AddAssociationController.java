package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class AddAssociationController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField abbreviationField;
    @FXML private VBox departmentsContainer;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    private String currentUserEmail;
    private String currentUserName;
    private String currentUserRole;

    private int departmentCount = 3;

    public void initializeUserData(String email, String name, String role) {
        this.currentUserEmail = email;
        this.currentUserName = name;
        this.currentUserRole = role;
    }

    @FXML
    private void handleAddDepartment() {
        HBox newDepartmentBox = new HBox(10);
        TextField newDepartmentField = new TextField();
        newDepartmentField.setPrefWidth(200);
        newDepartmentField.setPromptText("Department " + (++departmentCount));

        Button removeButton = new Button("-");
        removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        removeButton.setOnAction(e -> {
            departmentsContainer.getChildren().remove(newDepartmentBox);
            departmentCount--;
        });

        newDepartmentBox.getChildren().addAll(newDepartmentField, removeButton);
        newDepartmentBox.setAlignment(Pos.CENTER_LEFT);
        departmentsContainer.getChildren().add(newDepartmentBox);
    }

    @FXML
    private void handleRemoveDepartment() {
        if (departmentCount > 1) {
            departmentsContainer.getChildren().remove(departmentCount - 1);
            departmentCount--;
        }
    }

    @FXML
    private void handleCreateAssociation() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty()) {
                showAlert("Error", "Name and Description are required fields!");
                return;
            }

            String name = nameField.getText();
            String description = descriptionField.getText();
            String abbreviation = abbreviationField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();

            String checkSql = "SELECT 1 FROM associations WHERE name = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, name);
                if (checkStmt.executeQuery().next()) {
                    showAlert("Error", "Association name already exists!");
                    return;
                }
            }

            String insertAssocSql = "INSERT INTO associations (name, description, abbreviation, email, phone) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertAssocSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setString(3, abbreviation);
                stmt.setString(4, email);
                stmt.setString(5, phone);
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                int associationId = rs.next() ? rs.getInt(1) : -1;

                String insertDeptSql = "INSERT INTO departments (association_id, name) VALUES (?, ?)";
                try (PreparedStatement deptStmt = conn.prepareStatement(insertDeptSql)) {
                    for (var node : departmentsContainer.getChildren()) {
                        if (node instanceof HBox) {
                            HBox box = (HBox) node;
                            TextField deptField = (TextField) box.getChildren().get(0);
                            String dept = deptField.getText();
                            if (!dept.isEmpty()) {
                                deptStmt.setInt(1, associationId);
                                deptStmt.setString(2, dept);
                                deptStmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            showAlert("Success", "Association created successfully!");
            handleCancel();
        } catch (SQLException e) {
            showAlert("Error", "Failed to save association: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();

            HomeController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) nameField.getScene().getWindow();
            Scene scene = new Scene(root, 700, 650);
            stage.setScene(scene);
            stage.setTitle("UNSTPB Dashboard");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to return to home: " + e.getMessage());
        }
    }
}   