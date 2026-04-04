package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javafx.scene.layout.Pane;
import javafx.scene.Parent;

public class OpeningController {


    @FXML
    protected void onExit(ActionEvent event) {
        Platform.exit();
    }
       @FXML
    protected void onStart(ActionEvent event) throws IOException
    {
        File file = new File("password.txt");
         int x=-1;
        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextInt()) {
              x = scanner.nextInt();
                System.out.println("No !");
            } else {
                System.out.println("No integer found in file!");
            }
        } catch (Exception e) {
            System.out.println("File not found!");
        }

        if (x==1) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("pass.fxml"));
                Scene passScene = new Scene(loader.load());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(passScene);
                stage.show();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("creatingAccount.fxml"));
                Scene createScene = new Scene(loader.load());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(createScene);
                stage.show();

            }


    }
}
