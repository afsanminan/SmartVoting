package com.example.votesmartly;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Paths;

public class DatabaseConnection {

    public static Connection getConnection() {
        try {

            String projectRoot = System.getProperty("user.dir");
            String dbPath = Paths.get(projectRoot, "Database", "electionInformation.sqlite").toString();
            String url = "jdbc:sqlite:" + dbPath;
            Connection connection = DriverManager.getConnection(url);
            System.out.println("Database connected: " + dbPath);
            return connection;
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
            return null;
        }
    }
}
