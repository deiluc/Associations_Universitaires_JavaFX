package com.example.associations_universitaires_javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ListAssociationsController {
    @FXML private TableView<Association> associationsTable;
    @FXML private TableColumn<Association, String> actionColumn;
    private String currentUserRole;

    public static class Association {
        private final String name;
        private final String abbreviation;
        private final String description;
        private final String dummy; // For the action column

        public Association(String name, String abbreviation, String description) {
            this.name = name;
            this.abbreviation = abbreviation;
            this.description = description;
            this.dummy = ""; // Dummy value for the action column
        }

        // Getters
        public String getName() { return name; }
        public String getAbbreviation() { return abbreviation; }
        public String getDescription() { return description; }
        public String getDummy() { return dummy; }
    }

    public void initializeUserData(String role) {
        this.currentUserRole = role;
        configureActionColumn();
        loadAssociations();
    }

    private void configureActionColumn() {
        if (!"admin".equals(currentUserRole)) {
            actionColumn.setVisible(false);
            return;
        }

        actionColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Association, String> call(TableColumn<Association, String> param) {
                return new TableCell<>() {
                    private final Button deleteBtn = new Button("Delete");

                    {
                        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        deleteBtn.setOnAction(event -> {
                            Association association = getTableView().getItems().get(getIndex());
                            showDeleteConfirmation(association);
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(deleteBtn);
                        }
                    }
                };
            }
        });
    }
    private void showDeleteConfirmation(Association association) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete Association");
        dialog.setHeaderText("Are you sure you want to delete '" + association.getName() + "'?");

        // Set the button types
        ButtonType yesButtonType = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType noButtonType = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(yesButtonType, noButtonType);

        // Create reason text field
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Enter reason for deletion");
        reasonField.setWrapText(true);
        reasonField.setPrefRowCount(3);

        VBox content = new VBox(10, new Label("Reason for deletion:"), reasonField);
        content.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(content);

        // Enable/disable Yes button based on reason input
        Button yesButton = (Button) dialog.getDialogPane().lookupButton(yesButtonType);
        yesButton.setDisable(true);
        reasonField.textProperty().addListener((obs, oldVal, newVal) -> {
            yesButton.setDisable(newVal.trim().isEmpty());
        });

        // Show dialog and handle response
        dialog.showAndWait().ifPresent(response -> {
            if (response == yesButtonType) {
                deleteAssociation(association, reasonField.getText());
            }
        });
    }

    private void deleteAssociation(Association association, String reason) {
        try {
            // 1. Remove from associations.txt
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader("associations.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (!parts[0].equals(association.getName())) {
                        lines.add(line);
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("associations.txt"))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            // 2. Log the deletion
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("deletions.log", true))) {
                writer.write(String.format("%s:%s:%s%n",
                        association.getName(),
                        LocalDateTime.now(),
                        reason));
            }

            // 3. Reload the table
            loadAssociations();

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Association deleted successfully!");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to delete association!");
            alert.showAndWait();
        }
    }

    private void loadAssociations() {
        ObservableList<Association> associations = FXCollections.observableArrayList();

        try (BufferedReader reader = new BufferedReader(new FileReader("associations.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    associations.add(new Association(parts[0], parts[2], parts[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        associationsTable.setItems(associations);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/home-view.fxml"
            ));
            Parent root = loader.load();
            HomeController controller = loader.getController();
            controller.initializeUserData("", "", currentUserRole);
            Stage stage = (Stage) associationsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("University Clubs Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}