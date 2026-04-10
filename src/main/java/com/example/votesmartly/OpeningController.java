package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class OpeningController {

    @FXML
    public void onExit(ActionEvent e)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to exit?");

        if(alert.showAndWait().get() == ButtonType.OK)
        {
            System.exit(0);
        }
    }

    @FXML
    protected void onStart(ActionEvent event) throws IOException {
        String mode = AppConfig.get("mode");
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        FXMLLoader loader;

        if ("client".equals(mode)) {
            loader = new FXMLLoader(getClass().getResource("clientOpening.fxml"));
        } else {
            boolean hasRow = false;
            try {
                Connection conn = DatabaseConnection.getConnection();
                ResultSet rs = conn.createStatement().executeQuery("SELECT 1 FROM password LIMIT 1");
                hasRow = rs.next();
                conn.close();
            } catch (Exception e) { e.printStackTrace(); }

            loader = hasRow
                    ? new FXMLLoader(getClass().getResource("pass.fxml"))
                    : new FXMLLoader(getClass().getResource("creatingAccount.fxml"));
        }

        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}