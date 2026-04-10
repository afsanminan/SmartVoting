//package com.example.votesmartly;
//
//import javafx.application.Platform;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.PasswordField;
//import javafx.scene.control.TextField;
//import java.util.Scanner;
//import java.io.File;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import javafx.scene.Node;
//import java.io.IOException;
//import javafx.fxml.Initializable;
//import java.net.URL;
//import java.util.ResourceBundle;
//
//import javafx.scene.layout.StackPane;
//import javafx.util.Duration;
//import javafx.animation.FadeTransition;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//
//public class PassController implements Initializable {
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        Platform.runLater(() -> {
//            username.getParent().requestFocus();
//        });
//    }
//
//    @FXML
//    public TextField username;
//    @FXML
//    public PasswordField password;
//    @FXML
//    public Label userError;
//    @FXML
//    public Label passError;
//    @FXML
//    public StackPane popupPane;
//    @FXML
//    public Label popupMessage;
//    @FXML public Button deleteAccount;
//
//
//    private void showPopup(String message) {
//        popupMessage.setText(message);
//        popupPane.setVisible(true);
//
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), popupPane);
//        fadeIn.setFromValue(0.0);
//        fadeIn.setToValue(1.0);
//        fadeIn.play();
//
//        fadeIn.setOnFinished(e -> {
//            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popupPane);
//            fadeOut.setFromValue(1.0);
//            fadeOut.setToValue(0.0);
//            fadeOut.setDelay(Duration.seconds(1));
//            fadeOut.play();
//            fadeOut.setOnFinished(ev -> popupPane.setVisible(false));
//        });
//    }
//
//
//    public void onLogin(ActionEvent event) {
//        System.out.println("Button Clicked");
//
//        String user = username.getText().trim();
//        String pass = password.getText().trim();
//
//        userError.setText("");
//        passError.setText("");
//
//        try {
//            Connection conn = DatabaseConnection.getConnection();
//
//            String query = "SELECT pass FROM password WHERE username = ?";
//            PreparedStatement pstmt = conn.prepareStatement(query);
//            pstmt.setString(1, user);
//
//            ResultSet rs = pstmt.executeQuery();
//
//            if (!rs.next()) {
//                userError.setText("Invalid Username!");
//            } else {
//                String dbPass = rs.getString("pass");
//
//                if (!pass.equals(dbPass)) {
//                    passError.setText("Invalid Password!");
//                } else {
//                    System.out.println("Login Successful!");
//                    showPopup("Login Successful!");
//
//                    String updateQuery = "UPDATE password SET logged_in = 1 WHERE username = ?";
//                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
//                    updateStmt.setString(1, user);
//                    updateStmt.executeUpdate();
//                    updateStmt.close();
//
//                    try {
//                        FXMLLoader loader = new FXMLLoader(getClass().getResource("logindone.fxml"));
//                        Parent root = loader.load();
//
//                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//                        stage.setScene(new Scene(root));
//                        stage.show();
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            rs.close();
//            pstmt.close();
//            conn.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    public void onDelete(ActionEvent event) {
//
//    }
//}


package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PassController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            username.getParent().requestFocus();
        });
    }

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public Label userError;
    @FXML public Label passError;
    @FXML public StackPane popupPane;
    @FXML public Label popupMessage;
    @FXML public Button deleteAccount;

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

    public void onLogin(ActionEvent event) {
        System.out.println("Button Clicked");

        String user = username.getText().trim();
        String pass = password.getText().trim();

        userError.setText("");
        passError.setText("");

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "SELECT pass FROM password WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, user);

            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                userError.setText("Invalid Username!");
            } else {
                String dbPass = rs.getString("pass");

                if (!pass.equals(dbPass)) {
                    passError.setText("Invalid Password!");
                } else {
                    System.out.println("Login Successful!");
                    showPopup("Login Successful!");

                    String updateQuery = "UPDATE password SET logged_in = 1 WHERE username = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, user);
                    updateStmt.executeUpdate();
                    updateStmt.close();

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("logindone.fxml"));
                        Parent root = loader.load();

                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            rs.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDelete(ActionEvent event) {
        String user = username.getText().trim();
        String pass = password.getText().trim();

        // Validation check
        if (user.isEmpty() || pass.isEmpty()) {
            Alert emptyAlert = new Alert(Alert.AlertType.WARNING);
            emptyAlert.setTitle("Missing Information");
            emptyAlert.setHeaderText(null);
            emptyAlert.setContentText("You must fill up the information!");
            emptyAlert.showAndWait();
            return;
        }

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "SELECT pass FROM password WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                Alert mismatch = new Alert(Alert.AlertType.ERROR);
                mismatch.setTitle("Error");
                mismatch.setHeaderText(null);
                mismatch.setContentText("Information does not match!");
                mismatch.showAndWait();
            } else {
                String dbPass = rs.getString("pass");

                if (!pass.equals(dbPass)) {
                    Alert mismatch = new Alert(Alert.AlertType.ERROR);
                    mismatch.setTitle("Error");
                    mismatch.setHeaderText(null);
                    mismatch.setContentText("Information does not match!");
                    mismatch.showAndWait();
                } else {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Account");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Are you sure you want to delete your account?Current election data will be deleted");

                    Optional<ButtonType> result = confirm.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String deleteQuery = "DELETE FROM password WHERE username = ?";
                        PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                        deleteStmt.setString(1, user);
                        deleteStmt.executeUpdate();
                        deleteStmt.close();

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("creatingAccount.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    }
                }
            }

            rs.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}