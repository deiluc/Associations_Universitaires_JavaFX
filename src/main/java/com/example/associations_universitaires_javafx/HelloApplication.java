package com.example.associations_universitaires_javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Create database file if it doesn't exist
        try {
            File dbFile = new File("DataBase.txt");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
                System.out.println("Created new DataBase.txt file");
            }
        } catch (IOException e) {
            System.err.println("Error creating database file:");
            e.printStackTrace();
        }

        // 2. Load the FXML file with absolute path
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/example/associations_universitaires_javafx/hello-view.fxml"
                )
        );

        // Debug output
        System.out.println("FXML Loader location: " + fxmlLoader.getLocation());

        // 3. Create and show the scene
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("University Clubs Management");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        System.out.println("Launching application...");
        launch(args);
    }
}