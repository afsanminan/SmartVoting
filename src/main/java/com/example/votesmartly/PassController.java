package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.Scanner;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.IOException;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.FadeTransition;

public class PassController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            username.getParent().requestFocus();
        });
    }

    @FXML
    public TextField username;
    @FXML
    public PasswordField password;
    @FXML
    public Label userError;
    @FXML
    public Label passError;
    @FXML
    public StackPane popupPane;
    @FXML
    public Label popupMessage;


    private void showPopup(String message) {
        popupMessage.setText(message);
        popupPane.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), popupPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        fadeIn.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popupPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(1));
            fadeOut.play();
            fadeOut.setOnFinished(ev -> popupPane.setVisible(false));
        });
    }


    public void onLogin(ActionEvent event)
    {   System.out.println("Button Clicked");
        String user = username.getText().trim();
        String pass = password.getText().trim();
        userError.setText("");
        passError.setText("");

        File file = new File("password.txt");
        try (Scanner scanner = new Scanner(file)) {
            if (!scanner.hasNextInt()) {
                System.out.println("File format error!");
                return;
            }

            int x = scanner.nextInt();
            scanner.nextLine();
            System.out.println(x);
            if (!scanner.hasNext()) {
                System.out.println("Username missing!");
                return;
            }

            String user1 = scanner.nextLine().trim();
            System.out.println(user1);
            if (!scanner.hasNext()) {
                System.out.println("Password missing!");
                return;
            }
            String pass1 = scanner.nextLine().trim();
            System.out.println(pass1);
            if (!user.equals(user1)) {
                userError.setText("Invalid Username!");
            } else if (!pass.equals(pass1)) {
                passError.setText("Invalid Password!");
            } else {
                System.out.println("Login Successful!");
                showPopup("Login Successful!");

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("logindone.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}