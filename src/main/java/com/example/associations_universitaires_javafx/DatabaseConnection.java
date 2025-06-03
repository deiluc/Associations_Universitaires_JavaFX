package com.example.associations_universitaires_javafx;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static boolean useMockConnection = false;
    private static Connection mockConnection;

    // Method to inject a mock Connection for testing
    public static void setMockConnection(Connection connection) {
        useMockConnection = true;
        mockConnection = connection;
    }

    // Method to reset the mock Connection after tests
    public static void resetMockConnection() {
        useMockConnection = false;
        mockConnection = null;
    }
    private static final String URL = ""; //your URL for the local host
    private static final String USER = ""; //your username for acces the local host DB(default: root || system)
    private static final String PASSWORD = ""; //your local holst password to connect
    public static Connection getConnection() throws SQLException {
        if (useMockConnection) {
            throw new SQLException("Mock connection used for testing");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // For testing
    public static void setUseMockConnection(boolean useMock) {
        useMockConnection = useMock;
    }/*
    public static Connection getConnection() throws SQLException {
        if (useMockConnection) {
            if (mockConnection == null) {
                throw new SQLException("Mock connection not set");
            }
            return mockConnection;
        }
        String url = "jdbc:mysql://localhost:3306/associations_db";
        String user = "root";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }*/
}
