package com.example.votesmartly;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public static Connection getConnection() {
        Connection connection = null;
        try {
            // ফাইলটি 'Database' ফোল্ডারের ভেতরে থাকায় এই পাথটি ব্যবহার করা হয়েছে
            String url = "jdbc:sqlite:Database/electionInformation.sqlite";
            connection = DriverManager.getConnection(url);
            System.out.println("successful");
        } catch (SQLException e) {
            System.out.println("error " + e.getMessage());
        }
        return connection;
    }
}