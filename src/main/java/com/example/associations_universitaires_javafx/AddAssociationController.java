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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AddAssociationController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField abbreviationField;
    @FXML private VBox departmentsContainer;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    private int departmentCount = 3;

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
        if (departmentCount > 1) { // Keep at least one department
            departmentsContainer.getChildren().remove(departmentCount - 1);
            departmentCount--;
        }
    }

    @FXML
    private void handleCreateAssociation() {
        try {
            // Validate required fields
            if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty()) {
                showAlert("Error", "Name and Description are required fields!");
                return;
            }

            // Collect all data
            String name = nameField.getText();
            String description = descriptionField.getText();
            String abbreviation = abbreviationField.getText();
            String email = emailField.getText();
            String phone = phoneField.getText();

            // Collect departments
            StringBuilder departments = new StringBuilder();
            departmentsContainer.getChildren().forEach(node -> {
                if (node instanceof HBox) {
                    HBox box = (HBox) node;
                    TextField deptField = (TextField) box.getChildren().get(0);
                    String dept = deptField.getText();
                    if (!dept.isEmpty()) {
                        if (departments.length() > 0) departments.append(":");
                        departments.append(dept);
                    }
                }
            });

            // Save to associations.txt
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("associations.txt", true))) {
                writer.write(String.format("%s:%s:%s:%s:%s:%s\n",
                        name, description, abbreviation, departments.toString(), email, phone));
            }

            // Show success message
            showAlert("Success", "Association created successfully!");

            // Return to home page
            handleCancel();

        } catch (IOException e) {
            showAlert("Error", "Failed to save association: " + e.getMessage());
            e.printStackTrace();
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
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("University Clubs Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}