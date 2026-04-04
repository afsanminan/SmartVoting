package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.io.FileWriter;
import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;




public class CreatingAccountController {
        @FXML
        public TextField username;
        @FXML
        public PasswordField password;
        @FXML
        public PasswordField confirmingPass;
        @FXML
        public Label usernameError;
        @FXML public Label passwordError;
         @FXML public Label confirmError;
         @FXML public StackPane popupPane;
     @FXML
         private Label popupMessage;
    private void showPopup(String message) {
        popupMessage.setText(message);
        popupPane.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), popupPane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        fadeIn.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popupPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(1)); // ১ সেকেন্ড visible থাকবে
            fadeOut.play();
            fadeOut.setOnFinished(e -> popupPane.setVisible(false));
        });
    }

    public void onCreateAccount(ActionEvent event)
        {
            String user=username.getText();
            String pass=password.getText();
            String confirm=confirmingPass.getText();
            usernameError.setText("");
            passwordError.setText("");
            confirmError.setText("");
            boolean hasError = false;
            if (user == null || user.trim().isEmpty()) {
                usernameError.setText("Username cannot be empty");
                hasError = true;
            }
            if (pass == null || pass.trim().isEmpty()) {
                passwordError.setText("Password cannot be empty");
                hasError = true;
            }
            if (confirm == null || confirm.trim().isEmpty()) {
                confirmError.setText("Please confirm your password");
                hasError = true;
            }
            if (!hasError && !pass.equals(confirm)) {
                confirmError.setText("Passwords do not match!");
                hasError = true;
            }
            if (!hasError) {
                System.out.println("Account created successfully for: " + user);
                try (FileWriter writer = new FileWriter("password.txt")) {
                    writer.write("1\n");
                    writer.write(user + "\n");
                    writer.write(pass + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                showPopup("Account created successfully for: " + user);
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("newElection.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
}
