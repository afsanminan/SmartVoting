package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LogInDoneController {

        @FXML
        private AnchorPane popupPane;
        @FXML
        private Button yesBtn;
        @FXML
        private Button noBtn;

        public void newElection(ActionEvent event) {
                popupPane.setVisible(true);

                yesBtn.setOnAction(e -> {
                        try {
                                try (Connection conn = DatabaseConnection.getConnection()) {

                                        String[] queries = {
                                                "DELETE FROM election_info WHERE id_db = (SELECT id_db FROM election_info ORDER BY id_db DESC LIMIT 1)",
                                                "DELETE FROM national",
                                                "DELETE FROM student",
                                                "DELETE FROM candidate_national",
                                                "DELETE FROM voter_national",
                                                "DELETE FROM candidate_std",
                                                "DELETE FROM voter_std"
                                        };

                                        for (String q : queries) {
                                                PreparedStatement pst = conn.prepareStatement(q);
                                                pst.executeUpdate();
                                                pst.close();
                                        }

                                } catch (Exception a) {
                                        a.printStackTrace();
                                }

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("newElection.fxml"));
                                Parent root = loader.load();

                                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                stage.setScene(new Scene(root));
                                stage.show();

                        } catch (IOException ex) {
                                ex.printStackTrace();
                        }
                });

                noBtn.setOnAction(e -> popupPane.setVisible(false));
        }

        public void prevElection(ActionEvent event) {

                try (Connection conn = DatabaseConnection.getConnection()) {

                        String query = "SELECT success FROM election_info ORDER BY id_db DESC LIMIT 1";
                        PreparedStatement pst = conn.prepareStatement(query);
                        ResultSet rs = pst.executeQuery();

                        if (rs.next()) {
                                int success = rs.getInt("success");

                                if (success == 0) {
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("addOrRemove.fxml"));
                                        Parent root = loader.load();

                                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                        stage.setScene(new Scene(root));
                                        stage.show();
                                }
                                else {
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("No Ongoing Election");
                                        alert.setHeaderText(null);
                                        alert.setContentText("No election is ongoing right now.");
                                        alert.showAndWait();
                                }
                        }
                        else {

                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("No Election Data");
                                alert.setHeaderText(null);
                                alert.setContentText("No election data found in the system.");
                                alert.showAndWait();
                        }

                } catch (Exception ex) {
                        ex.printStackTrace();
                }
        }
        public void onHistory(ActionEvent e) {
                try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("history.fxml"));
                        Parent root = loader.load();

                        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();

                } catch (IOException ex) {
                        ex.printStackTrace();
                }
        }

        public void onExit() {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit Confirmation");
                alert.setHeaderText("Are you sure you want to exit?");
                alert.setContentText("Press OK to exit, Cancel to stay.");

                alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                                Platform.exit();
                        }
                });
        }
}