package com.example.associations_universitaires_javafx;

import com.example.associations_universitaires_javafx.Announcement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Node;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HomeController {
    @FXML private Label welcomeLabel;
    @FXML private MenuItem addAsociatieItem;
    @FXML private Button addAnnouncementBtn;
    @FXML private VBox announcementsContainer;

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

        // Show add announcement button only for admin
        addAnnouncementBtn.setVisible("admin".equals(role));
        addAnnouncementBtn.setManaged("admin".equals(role));
    }
    private void configureMenuForUserRole() {
        if (addAsociatieItem != null) {
            addAsociatieItem.setVisible("admin".equals(currentUserRole));
        }
    }

    private void loadAnnouncements() {
        announcementsContainer.getChildren().clear();

        // Create sample announcements if file doesn't exist
        if (!new File("announcements.txt").exists()) {
            createSampleAnnouncements();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("announcements.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Announcement announcement = new Announcement(parts[0], parts[1], parts[2], parts[3]);
                    announcementsContainer.getChildren().add(createAnnouncementCard(announcement));
                }
            }
        } catch (IOException e) {
            showAlert("Eroare", "Nu s-au putut încărca anunțurile!");
        }
    }

    private void createSampleAnnouncements() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("announcements.txt"))) {
            writer.write("Bine ați venit!|Acesta este sistemul de gestionare a asociațiilor universitare.|Admin|" + LocalDateTime.now() + "\n");
            writer.write("Întâlnire generală|Vă așteptăm la întâlnirea generală vineri, ora 14:00.|Admin|" + LocalDateTime.now().minusDays(1) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Node createAnnouncementCard(Announcement announcement) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-border-width: 1;");
        card.setPadding(new Insets(15));
        card.setMaxWidth(Double.MAX_VALUE); // Important for proper sizing

        // Prevent the card from growing beyond the scroll pane's width
        card.setMaxWidth(Region.USE_COMPUTED_SIZE);
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(announcement.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if ("admin".equals(currentUserRole)) {
            // Create the three-dots menu button
            MenuButton optionsMenu = new MenuButton("⋯"); // Using Unicode ellipsis character
            optionsMenu.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-font-size: 18px; " +  // Make the dots bigger
                            "-fx-padding: 0 5 0 5; " +  // Adjust padding
                            "-fx-mark-color: transparent;"  // Hide the dropdown arrow
            );

            // Remove the dropdown arrow completely
            optionsMenu.setPopupSide(null);

            // Create menu items
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> handleEditAnnouncement(announcement));

            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> handleDeleteAnnouncement(announcement));

            optionsMenu.getItems().addAll(editItem, deleteItem);
            headerBox.getChildren().addAll(titleLabel, spacer, optionsMenu);
        } else {
            headerBox.getChildren().addAll(titleLabel);
        }

        // Rest of your card creation code...
        Label contentLabel = new Label(announcement.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14;");

        HBox footerBox = new HBox(10);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("Posted by: " + announcement.getAuthor());
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
        dialog.setTitle("Adaugă Anunț Nou");
        dialog.setHeaderText("Creați un nou anunț");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Titlu");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Conținut");
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);

        grid.add(new Label("Titlu:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Conținut:"), 0, 1);
        grid.add(contentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType postButtonType = new ButtonType("Postează", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);

        // Process result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == postButtonType) {
                return new Announcement(
                        titleField.getText(),
                        contentArea.getText(),
                        currentUserName,
                        LocalDateTime.now()
                );
            }
            return null;
        });

        Optional<Announcement> result = dialog.showAndWait();
        result.ifPresent(announcement -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("announcements.txt", true))) {
                writer.write(String.format("%s|%s|%s|%s%n",
                        announcement.getTitle(),
                        announcement.getContent(),
                        announcement.getAuthor(),
                        announcement.getDatePosted().toString()));
                loadAnnouncements();
            } catch (IOException e) {
                showAlert("Eroare", "Nu s-a putut salva anunțul!");
            }
        });
    }
    private void handleEditAnnouncement(Announcement announcement) {
        Dialog<Announcement> dialog = new Dialog<>();
        dialog.setTitle("Editează Anunț");
        dialog.setHeaderText("Editează conținutul anunțului");

        // Set up form with existing data
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField(announcement.getTitle());
        TextArea contentArea = new TextArea(announcement.getContent());
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);

        grid.add(new Label("Titlu:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Conținut:"), 0, 1);
        grid.add(contentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Salvează", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Process result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new Announcement(
                        titleField.getText(),
                        contentArea.getText(),
                        announcement.getAuthor(),
                        announcement.getDatePosted()
                );
            }
            return null;
        });

        Optional<Announcement> result = dialog.showAndWait();
        result.ifPresent(updatedAnnouncement -> {
            try {
                // Read all announcements
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader("announcements.txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 4 && parts[0].equals(announcement.getTitle())) {
                            // Replace with updated announcement
                            line = String.format("%s|%s|%s|%s",
                                    updatedAnnouncement.getTitle(),
                                    updatedAnnouncement.getContent(),
                                    updatedAnnouncement.getAuthor(),
                                    updatedAnnouncement.getDatePosted().toString());
                        }
                        lines.add(line);
                    }
                }

                // Write back to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("announcements.txt"))) {
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }

                loadAnnouncements();
            } catch (IOException e) {
                showAlert("Eroare", "Nu s-a putut actualiza anunțul!");
            }
        });
    }

    private void handleDeleteAnnouncement(Announcement announcement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Șterge Anunț");
        alert.setHeaderText("Sigur doriți să ștergeți acest anunț?");
        alert.setContentText(announcement.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Read all announcements except the one to delete
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader("announcements.txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\|");
                        if (!parts[0].equals(announcement.getTitle())) {
                            lines.add(line);
                        }
                    }
                }

                // Write back to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("announcements.txt"))) {
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }

                loadAnnouncements();
            } catch (IOException e) {
                showAlert("Eroare", "Nu s-a putut șterge anunțul!");
            }
        }
    }

    @FXML
    private void handleAddAsociatie() {
        try {
            System.out.println("Add Association button clicked!"); // Debug line

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/add-association-view.fxml"
            ));
            Parent root = loader.load();

            // Get current stage
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            // Set new scene
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("Create New Association");
            stage.show();

        } catch (IOException e) {
            System.err.println("Error loading add-association-view.fxml:");
            e.printStackTrace();
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
        } catch (IOException e) {
            e.printStackTrace();
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
            controller.initializeUserData(currentUserEmail, currentUserRole);  // Pass both email and role

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Associations List");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load associations list: " + e.getMessage());
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
    private void handleViewProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/associations_universitaires_javafx/profile-view.fxml"
            ));
            Parent root = loader.load();

            // Pass current user data to profile controller
            ProfileController controller = loader.getController();
            controller.initializeUserData(currentUserEmail, currentUserName, currentUserRole);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("My Profile");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load profile page!");
        }
    }
}