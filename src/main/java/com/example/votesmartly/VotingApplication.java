package com.example.votesmartly;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class VotingApplication extends Application {
    public static void main(String[] args) {
        Connection connection;
        connection=DatabaseConnection.getConnection();
        if(connection==null) System.exit(1);
        launch();
    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VotingApplication.class.getResource("opening.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
