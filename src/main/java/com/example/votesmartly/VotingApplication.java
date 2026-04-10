package com.example.votesmartly;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class VotingApplication extends Application {

    public static void main(String[] args) {
        if (DatabaseConnection.getConnection() == null) {
            System.err.println("ERROR: Could not connect to database. Check that the Database folder exists.");
            System.exit(1);
        }
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        ElectionServer.start();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("opening.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("VoteSmartly — Server");
        stage.show();
    }
}
