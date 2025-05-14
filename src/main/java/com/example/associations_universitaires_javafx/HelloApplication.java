package com.example.associations_universitaires_javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Test database connection
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Database connection established successfully");
        } catch (Exception e) {
            System.err.println("Database connection failed");
            e.printStackTrace();
            throw e;
        }

        // Load the main view
        Parent root = FXMLLoader.load(getClass().getResource(
                "/com/example/associations_universitaires_javafx/hello-view.fxml"
        ));

        stage.setScene(new Scene(root, 700, 650));
        stage.setTitle("UNSTPB Management");
        stage.setMinWidth(700);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}