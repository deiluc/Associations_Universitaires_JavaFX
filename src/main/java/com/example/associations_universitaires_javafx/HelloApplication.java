package com.example.associations_universitaires_javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Initialize text database
        initializeTextDatabase();

        // 2. Load the FXML
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/example/associations_universitaires_javafx/hello-view.fxml"
                ));

        Scene scene = new Scene(fxmlLoader.load(), 700, 650);

        // Temporarily comment out this line to disable CSS
        // scene.getStylesheets().add(getClass().getResource("/com/example/associations_universitaires_javafx/styles.css").toExternalForm());
        // 4. Stage setup (keeping your title and size constraints)
        stage.setTitle("University Clubs Management");
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(650);
        stage.show();
    }


    private void initializeTextDatabase() throws IOException {
        // Users database
        File usersFile = new File("users.txt");
        if (!usersFile.exists()) {
            try {
                usersFile.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(usersFile, true));
                // Add default admin user (password: admin123)
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                writer.write("Admin:User:admin@university.com:" + hashedPassword + ":admin");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.err.println("Error creating users file:");
                e.printStackTrace();
            }
        }

        // Add this to HelloApplication.java in initializeTextDatabase()
        File announcementsFile = new File("announcements.txt");
        if (!announcementsFile.exists()) {
            announcementsFile.createNewFile();
        }

        // Associations database
        File associationsFile = new File("associations.txt");
        if (!associationsFile.exists()) {
            try {
                associationsFile.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(associationsFile, true));
                // Add sample association
                writer.write("1:Computer Science Club:A club for CS students:2020-01-15:1");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.err.println("Error creating associations file:");
                e.printStackTrace();
            }
        }

        // Departments database
        File departmentsFile = new File("departments.txt");
        if (!departmentsFile.exists()) {
            try {
                departmentsFile.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(departmentsFile, true));
                // Add sample department
                writer.write("1:1:Web Development:Department for web dev:2");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.err.println("Error creating departments file:");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}