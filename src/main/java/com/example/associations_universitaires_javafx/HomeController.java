package com.example.associations_universitaires_javafx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;  // Add this import
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.io.IOException;

public class HomeController {
    @FXML private BorderPane borderPane;
    @FXML private Label welcomeLabel;
    @FXML private TreeView<String> treeView;
    @FXML private TextArea newsTextArea;
    @FXML private ListView<String> adminListView;

    public void setUserEmail(String email) {
        welcomeLabel.setText("Welcome, " + email + "!");
        initializeComponents();
    }

    // Remove this method - it doesn't belong in HomeController
    // It should be in HelloController where statusLabel exists
    /*
    private void loadHomePage(String email) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/associations_universitaires_javafx/home-view.fxml"
        ));
        Parent root = loader.load();

        HomeController controller = loader.getController();
        controller.setUserEmail(email);

        // Get the current stage from any node in the current scene
        Node source = (Node) statusLabel; // or any other @FXML component
        Stage stage = (Stage) source.getScene().getWindow();

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("University Clubs Dashboard");
    }
    */

    private void initializeComponents() {
        // Left: TreeView with options
        TreeItem<String> rootItem = new TreeItem<>("Options");
        TreeItem<String> Item1 = new TreeItem<>("Vizual");
        TreeItem<String> Item2 = new TreeItem<>("Pr");
        TreeItem<String> Item3 = new TreeItem<>("Bani");
        rootItem.getChildren().addAll(Item1, Item2, Item3);
        Item1.getChildren().addAll(
                new TreeItem<>("Chat1"),
                new TreeItem<>("Chat 2"),
                new TreeItem<>("Chat 3"),
                new TreeItem<>("Chat 4")
        );
        treeView.setRoot(rootItem);
        treeView.setPrefWidth(150);

        // Center: News (TextArea)
        newsTextArea.setText("Latest News:\n- JavaFX is awesome!\n- New updates available.\n- Stay tuned for more!");
        newsTextArea.setEditable(false);

        // Right: List of admin members
        adminListView.getItems().addAll("Deiluc", "Patrunjel", "Admin 3", "Admin 4");
        adminListView.setPrefWidth(150);

        // Set up tree view selection listener
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String selectedItem = newValue.getValue();
                switch (selectedItem) {
                    case "Vizual":
                        newsTextArea.setText("Vizual Details:\n- This is the Vizual section.\n- You can view visualizations here.");
                        break;
                    case "Pr":
                        newsTextArea.setText("Pr Details:\n- This is the Pr section.\n- You can manage projects here.");
                        break;
                    case "Bani":
                        newsTextArea.setText("Bani Details:\n- This is the Bani section.\n- You can manage finances here.");
                        break;
                    case "Chat1":
                        newsTextArea.setText("Chat1 Details:\n- This is Chat 1.\n- Start chatting with your team.");
                        break;
                    case "Chat 2":
                        newsTextArea.setText("Chat 2 Details:\n- This is Chat 2.\n- Collaborate with your team.");
                        break;
                    case "Chat 3":
                        newsTextArea.setText("Chat 3 Details:\n- This is Chat 3.\n- Discuss important topics.");
                        break;
                    case "Chat 4":
                        newsTextArea.setText("Chat 4 Details:\n- This is Chat 4.\n- Share ideas and feedback.");
                        break;
                    default:
                        newsTextArea.setText("Select an option from the left to see details.");
                        break;
                }
            }
        });
    }

    @FXML
    private void handleLogout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/associations_universitaires_javafx/hello-view.fxml"
        ));
        Parent root = loader.load();
        Stage stage = (Stage) borderPane.getScene().getWindow();
        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("University Clubs Login");
    }
}