Associations Universitaires JavaFX
Overview
Associations Universitaires JavaFX is a JavaFX desktop application designed for managing university associations. It provides a user-friendly interface for students, professors, and administrators to interact with association-related activities, including membership management, professor allocation, event organization, announcements, and notifications. The application supports role-based access control with three user roles: admin, professor, and regular user.
Key Features

Association Management: Create, edit, and delete university associations with details like name, abbreviation, and leader.
Professor Allocation: Assign professors to associations, including department, title, and contact information.
Membership: Users can join or follow associations, with leaders managing members.
Announcements: Admins can post and manage association-specific or general announcements.
Events: Create, approve, and register for events, with status tracking (pending, approved, rejected, canceled).
News: Share association news authored by members or admins.
Departments: Manage departments within associations for organizing activities.
Chat Messages: Facilitate department-specific communication via chat.
Applications: Handle membership applications with statuses (pending, interview, accepted, rejected).
Notifications: Deliver user-specific updates with read/unread tracking.
Profile Management: View and update user profiles.

Prerequisites
To run the application, ensure the following are installed:

Java JDK: Version 17 or later (recommended: OpenJDK 17).
JavaFX SDK: Version 17.0.2 or later.
Maven: For dependency management.
MySQL: Version 8.0 or later for the database.
IDE: IntelliJ IDEA, Eclipse, or any IDE with JavaFX support (optional but recommended).

Project Structure
associations_universitaires_javafx/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/associations_universitaires_javafx/
│   │   │       ├── HomeController.java
│   │   │       ├── ProfAllocationController.java
│   │   │       ├── AddAssociationController.java
│   │   │       ├── ListAssociationsController.java
│   │   │       ├── EventController.java
│   │   │       ├── ProfileController.java
│   │   │       ├── ApprovedEventsController.java
│   │   │       ├── EventStatusController.java
│   │   │       ├── NotificationsController.java
│   │   │       ├── DatabaseConnection.java
│   │   │       ├── HelloController.java
│   │   │       └── Main.java
│   │   ├── resources/
│   │   │   └── com/example/associations_universitaires_javafx/
│   │   │       ├── home-view.fxml
│   │   │       ├── prof-allocation-view.fxml
│   │   │       ├── add-association-view.fxml
│   │   │       ├── list-associations-view.fxml
│   │   │       ├── event-view.fxml
│   │   │       ├── profile-view.fxml
│   │   │       ├── approved-events-view.fxml
│   │   │       ├── event-status-view.fxml
│   │   │       ├── notifications-view.fxml
│   │   │       └── hello-view.fxml
│   └── test/
├── pom.xml
├── database.sql
└── README.md

Setup Instructions
1. Clone the Repository
git clone https://github.com/your-username/associations_universitaires_javafx.git
cd associations_universitaires_javafx

2. Configure the Database

Install MySQL:

Ensure MySQL is running on localhost:3307 (or update DatabaseConnection.java if using a different host/port).


Create the Database:

Create a database named aujavafx:
CREATE DATABASE aujavafx;




Create the Schema:

Save the following SQL script as database.sql and execute it to create the required tables and relationships:
-- Users table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    role ENUM('admin', 'prof', 'user') DEFAULT 'user',
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Professors table
CREATE TABLE professors (
    professor_id INT PRIMARY KEY,
    department VARCHAR(100),
    title VARCHAR(50),
    office_location VARCHAR(100),
    website_url VARCHAR(255),
    FOREIGN KEY (professor_id) REFERENCES users(user_id)
);

-- Associations table
CREATE TABLE associations (
    association_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    abbreviation VARCHAR(20),
    description TEXT,
    leader_id INT,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (leader_id) REFERENCES users(user_id)
);

-- Professor Associations table
CREATE TABLE professor_associations (
    professor_id INT,
    association_id INT,
    PRIMARY KEY (professor_id, association_id),
    FOREIGN KEY (professor_id) REFERENCES professors(professor_id),
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Members table
CREATE TABLE members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    association_id INT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_leader BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Follows table
CREATE TABLE follows (
    user_id INT,
    association_id INT,
    PRIMARY KEY (user_id, association_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Announcements table
CREATE TABLE announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    content TEXT,
    author_id INT,
    association_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(user_id),
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Applications table
CREATE TABLE applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    association_id INT,
    status ENUM('PENDING', 'INTERVIEW', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Departments table
CREATE TABLE departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    association_id INT,
    name VARCHAR(100),
    description TEXT,
    FOREIGN KEY (association_id) REFERENCES associations(association_id)
);

-- Chat Messages table
CREATE TABLE chat_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT,
    user_id INT,
    content TEXT,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Events table
CREATE TABLE events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    association_id INT,
    title VARCHAR(255),
    description TEXT,
    event_date DATETIME,
    location VARCHAR(255),
    created_by INT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELED') DEFAULT 'PENDING',
    end_date DATETIME,
    start_time TIME,
    end_time TIME,
    FOREIGN KEY (association_id) REFERENCES associations(association_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- Event Registrations table
CREATE TABLE event_registrations (
    event_id INT,
    user_id INT,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, user_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- News table
CREATE TABLE news (
    news_id INT AUTO_INCREMENT PRIMARY KEY,
    association_id INT,
    content TEXT,
    author_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (association_id) REFERENCES associations(association_id),
    FOREIGN KEY (author_id) REFERENCES users(user_id)
);

-- Notifications table
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    content VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);


Execute the script:
mysql -u root -p aujavafx < database.sql




Update Database Credentials:

Open src/main/java/com/example/associations_universitaires_javafx/DatabaseConnection.java.

Update the connection details:
private static final String URL = "jdbc:mysql://localhost:3307/aujavafx";
private static final String USER = "root";
private static final String PASSWORD = "your-password";



Security Note: For production, move credentials to a configuration file (e.g., application.properties) or environment variables.


3. Configure Dependencies
Ensure pom.xml includes the required dependencies:
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17.0.2</version>
    </dependency>
    <!-- MySQL Connector -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <!-- BCrypt for Password Hashing -->
    <dependency>
        <groupId>org.mindrot</groupId>
        <artifactId>jbcrypt</artifactId>
        <version>0.4</version>
    </dependency>
</dependencies>
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.example.associations_universitaires_javafx.Main</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>

Install dependencies:
mvn clean install

4. Create the Main Class
If missing, create src/main/java/com/example/associations_universitaires_javafx/Main.java:
package com.example.associations_universitaires_javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/associations_universitaires_javafx/hello-view.fxml"));
        primaryStage.setTitle("UNSTPB Login");
        primaryStage.setScene(new Scene(root, 700, 650));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

5. Run the Application

Using an IDE:

Open the project in IntelliJ IDEA or Eclipse.
Configure the JavaFX SDK and MySQL connector in your IDE.
Run the Main class.


Using Maven:

Execute:
mvn javafx:run





6. Test User Credentials

Admin User:
Email: a
Password: a
Role: admin
Note: This user is created via HomeController.addUser(). Remove or update for production.



To create the test user, trigger the addUser method (e.g., via a temporary button) or run this SQL:
INSERT INTO users (first_name, last_name, email, password_hash, role)
VALUES ('Admin', 'User', 'a', '$2a$10$your-hashed-password', 'admin');

Replace $2a$10$your-hashed-password with a BCrypt hash of a (use a BCrypt tool or the application’s addUser logic).
Usage

Login:

Launch the application and log in with an admin user (e.g., email a, password a).
Professors and regular users have restricted access based on their roles.


Home Screen:

View announcements and notifications.
Admins see the Administrative menu for advanced options (e.g., Professor Allocation, Add Association).


Key Features:

Associations: Create/edit associations, assign leaders, and manage departments.
Professor Allocation: Assign professors to associations with details like department and office location.
Membership: Apply to join associations; leaders approve/reject applications.
Events: Create, approve, and register for events.
Announcements/News: Post updates for associations.
Chat: Communicate within department-specific channels.
Notifications: View and mark notifications as read.


Role-Based Access:

Admin: Full access to all features, including association and professor management.
Professor: Manage events and view approved events.
User: Join associations, register for events, and view public content.



Database Schema
The database (aujavafx) consists of 15 tables with the following structure:

users: Stores user details (ID, name, email, password hash, role, phone, creation timestamp).
professors: Stores professor-specific details (linked to users via professor_id).
associations: Stores association details (name, leader, contact info).
professor_associations: Links professors to associations (many-to-many).
members: Tracks association membership with leader status.
follows: Tracks users following associations.
announcements: Stores association or general announcements.
applications: Manages membership applications with status tracking.
departments: Stores association departments.
chat_messages: Stores department-specific chat messages.
events: Stores event details with status and scheduling.
event_registrations: Tracks user event registrations.
news: Stores association news posts.
notifications: Stores user-specific notifications.

See database.sql for the full schema with foreign key constraints.
Troubleshooting

NullPointerException in ProfAllocationController:

Ensure TextField elements in prof-allocation-view.fxml have correct fx:id values (departmentField, titleField, officeLocationField, websiteUrlField).
Verify FXML bindings match ProfAllocationController fields.


Administrative Menu Not Visible:

Check the user’s role in the users table:
SELECT email, role FROM users WHERE email = 'a';


Ensure role is admin (case-sensitive). Update HomeController.configureMenuForUserRole for case-insensitive checks if needed:
boolean isAdmin = currentUserRole != null && currentUserRole.trim().toLowerCase().equals("admin");




Database Connection Errors:

Verify MySQL is running and credentials are correct in DatabaseConnection.java.
Check the database name (aujavafx) and port (3307).


FXML File Not Found:

Ensure FXML files are in src/main/resources/com/example/associations_universitaires_javafx/.
Check fx:controller and fx:id attributes in FXML files.



Contributing

Fork the repository.
Create a feature branch (git checkout -b feature/your-feature).
Commit changes (git commit -m "Add your feature").
Push to the branch (git push origin feature/your-feature).
Open a pull request.

License
This project is licensed under the MIT License. See the LICENSE file for details.
Contact
For issues or questions, contact [your-email@example.com] or open an issue on GitHub.
