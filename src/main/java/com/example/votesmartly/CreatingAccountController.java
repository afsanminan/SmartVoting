//package com.example.votesmartly;
//
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.scene.control.Label;
//import javafx.scene.control.PasswordField;
//import javafx.scene.control.TextField;
//import javafx.scene.layout.StackPane;
//import javafx.util.Duration;
//import javafx.animation.FadeTransition;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import javafx.scene.Node;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//
//public class CreatingAccountController {
//
//    @FXML
//    public TextField username;
//    @FXML
//    public PasswordField password;
//    @FXML
//    public PasswordField confirmingPass;
//    @FXML
//    public Label usernameError;
//    @FXML
//    public Label passwordError;
//    @FXML
//    public Label confirmError;
//    @FXML
//    public StackPane popupPane;
//    @FXML
//    private Label popupMessage;
//
//    // 🔹 Popup animation (unchanged)
//    private void showPopup(String message) {
//        popupMessage.setText(message);
//        popupPane.setVisible(true);
//
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), popupPane);
//        fadeIn.setFromValue(0.0);
//        fadeIn.setToValue(1.0);
//        fadeIn.play();
//
//        fadeIn.setOnFinished(event -> {
//            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popupPane);
//            fadeOut.setFromValue(1.0);
//            fadeOut.setToValue(0.0);
//            fadeOut.setDelay(Duration.seconds(1));
//            fadeOut.play();
//            fadeOut.setOnFinished(e -> popupPane.setVisible(false));
//        });
//    }
//
//    // 🔹 Create Account
//    public void onCreateAccount(ActionEvent event) {
//
//        String user = username.getText();
//        String pass = password.getText();
//        String confirm = confirmingPass.getText();
//
//        usernameError.setText("");
//        passwordError.setText("");
//        confirmError.setText("");
//
//        boolean hasError = false;
//
//        // 🔹 Validation
//        if (user == null || user.trim().isEmpty()) {
//            usernameError.setText("Username cannot be empty");
//            hasError = true;
//        }
//        if (pass == null || pass.trim().isEmpty()) {
//            passwordError.setText("Password cannot be empty");
//            hasError = true;
//        }
//        if (confirm == null || confirm.trim().isEmpty()) {
//            confirmError.setText("Please confirm your password");
//            hasError = true;
//        }
//        if (!hasError && !pass.equals(confirm)) {
//            confirmError.setText("Passwords do not match!");
//            hasError = true;
//        }
//
//        // 🔹 Database operations
//        if (!hasError) {
//            try {
//                Connection conn = DatabaseConnection.getConnection();
//
//                // 🔴 Check if username already exists
//                String checkQuery = "SELECT 1 FROM password WHERE username = ?";
//                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
//                checkStmt.setString(1, user);
//                ResultSet rs = checkStmt.executeQuery();
//
//                if (rs.next()) {
//                    usernameError.setText("Username already exists!");
//                    hasError = true;
//                }
//
//                rs.close();
//                checkStmt.close();
//
//                // 🔴 Insert only if no error
//                if (!hasError) {
//                    String insertQuery = "INSERT INTO password(username, pass, logged_in) VALUES (?, ?, 0)";
//                    PreparedStatement pstmt = conn.prepareStatement(insertQuery);
//                    pstmt.setString(1, user);
//                    pstmt.setString(2, pass);
//
//                    pstmt.executeUpdate();
//                    pstmt.close();
//
//                    System.out.println("Account created successfully for: " + user);
//                    showPopup("Account created successfully for: " + user);
//
//                    // 🔹 Navigate to next page
//                    FXMLLoader loader = new FXMLLoader(getClass().getResource("newElection.fxml"));
//                    Parent root = loader.load();
//
//                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//                    stage.setScene(new Scene(root));
//                    stage.show();
//                }
//
//                conn.close();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}

package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CreatingAccountController {

    @FXML public TextField username;
    @FXML public PasswordField password;
    @FXML public PasswordField confirmingPass;
    @FXML public Label usernameError;
    @FXML public Label passwordError;
    @FXML public Label confirmError;
    @FXML public StackPane popupPane;
    @FXML private Label popupMessage;

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
            fadeOut.setDelay(Duration.seconds(1));
            fadeOut.play();
            fadeOut.setOnFinished(e -> popupPane.setVisible(false));
        });
    }

    public void onCreateAccount(ActionEvent event) {

        String user = username.getText();
        String pass = password.getText();
        String confirm = confirmingPass.getText();

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
            try {
                Connection conn = DatabaseConnection.getConnection();


                String checkQuery = "SELECT 1 FROM password WHERE username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, user);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    usernameError.setText("Username already exists!");
                    hasError = true;
                }

                rs.close();
                checkStmt.close();

                if (!hasError) {
                    String insertQuery = "INSERT INTO password(username, pass, logged_in) VALUES (?, ?, 0)";
                    PreparedStatement pstmt = conn.prepareStatement(insertQuery);
                    pstmt.setString(1, user);
                    pstmt.setString(2, pass);
                    pstmt.executeUpdate();
                    pstmt.close();

                    System.out.println("Account created successfully for: " + user);
                    showPopup("Account created successfully for: " + user);

                    // Delete last row from election_info
                    String deleteLastElection = "DELETE FROM election_info WHERE id_db = (SELECT MAX(id_db) FROM election_info)";
                    PreparedStatement delElection = conn.prepareStatement(deleteLastElection);
                    delElection.executeUpdate();
                    delElection.close();

                    // Delete all rows from these tables
                    String[] tablesToClear = {
                            "national",
                            "student",
                            "candidate_national",
                            "voter_national",
                            "candidate_std",
                            "voter_std"
                    };

                    for (String table : tablesToClear) {
                        PreparedStatement delStmt = conn.prepareStatement("DELETE FROM " + table);
                        delStmt.executeUpdate();
                        delStmt.close();
                    }

                    System.out.println("Old election data cleared.");

                    // Navigate to next page
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("newElection.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                }

                conn.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}