

package com.example.votesmartly;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

public class clientOpeningController {

    @FXML public Button castVote;
    @FXML public Button viewHistory;

    public void onExit(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to exit?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.exit(0);
        }
    }


    public void onCast(ActionEvent e) {
        System.out.println("1 — onCast clicked");

        castVote.setDisable(true);

        Thread t = new Thread(() -> {
            System.out.println("2 — background thread started");

            String response;
            try {
                response = SocketClient.getElectionStatus();
                System.out.println("3 — received: " + response);
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    castVote.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Connection Error",
                            "Cannot reach the server:\n" + ex.getMessage());
                });
                return;
            }

            final String resp = response;
            Platform.runLater(() -> {
                castVote.setDisable(false);
                processCastResponse(resp);
            });
        });
        t.setDaemon(true);
        t.start();
    }


    private void processCastResponse(String response) {
        boolean running = response.contains("\"electionRunning\":true");
        if (!running) {
            showTimedAlert(Alert.AlertType.INFORMATION,
                    "No Election Running", "There is no election running right now.");
            return;
        }

        int    electionId   = parseIntField(response, "electionId");
        String electionName = extractField(response, "electionName");

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enter Voter ID");
        dialog.setHeaderText("Please enter your Voter ID:");
        dialog.setContentText("Voter ID:");
        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) return;

        int voterId;
        try {
            voterId = Integer.parseInt(input.get().trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Voter ID must be a number.");
            return;
        }

        String fxml = "Student Election".equalsIgnoreCase(electionName)
                ? "clientStudentRunning.fxml"
                : "clientNationalRunning.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Stage stage = (Stage) castVote.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

            Object ctrl = loader.getController();
            if (ctrl instanceof clientNationalRunningController c) {
                c.setElectionInfo(electionId, voterId, "national");
            } else if (ctrl instanceof clientStudentRunningController c) {
                c.setElectionInfo(electionId, voterId, "student");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load voting screen:\n" + ex.getMessage());
        }
    }


    private String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }

    private int parseIntField(String json, String key) {
        try { return Integer.parseInt(extractField(json, key)); }
        catch (NumberFormatException ex) { return -1; }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showTimedAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(2500), ae -> alert.close()));
        tl.setCycleCount(1);
        tl.play();
        alert.showAndWait();
    }
}

