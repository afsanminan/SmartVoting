package com.example.votesmartly;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        DatabaseConnection.getConnection();
        Application.launch(VotingApplication.class, args);
    }
}
