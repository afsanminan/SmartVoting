

package com.example.votesmartly;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class clientStudentRunningController {


    @FXML public Label dateLabel;
    @FXML public Label timeLabel;
    @FXML public Label nameError;
    @FXML public Label stdIdError;
    @FXML public Label deptError;
    @FXML public TextField stdIdField;
    @FXML public TextField nameField;
    @FXML public ComboBox<String> deptBox;
    @FXML public Label postLabel;
    @FXML public Label selectLabel;
    @FXML public Button voteBtn;
    @FXML public ComboBox<String> postBox;
    @FXML public Button endBtn;
    @FXML public Button startVoting;

    private static final String DEPT_PROMPT = "Select department";
    private static final String POST_PROMPT = "A better campus starts with your wise decision!";

    private List<String> postList      = new ArrayList<>();
    private int currentPostIndex       = 0;
    private String currentVoterId      = "";
    private int currentElectionId;


    public void initialize() {
        startClock();
        dateLabel.setText(LocalDate.now().toString());
        applyDeptBoxPromptFix();
        applyPostBoxPromptFix();
    }

    public void setElectionInfo(int electionId, int voterId, String type) {
        this.currentElectionId = electionId;
        loadDepartments();
    }


    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> timeLabel.setText(LocalTime.now().format(fmt))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }



    private void applyDeptBoxPromptFix() {
        deptBox.setPromptText(DEPT_PROMPT);
        deptBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(deptBox.getPromptText());
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }

    private void applyPostBoxPromptFix() {
        postBox.setPromptText(POST_PROMPT);
        postBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(postBox.getPromptText());
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }


    private void loadDepartments() {

        deptBox.setDisable(true);
        deptBox.getItems().clear();

        Thread t = new Thread(() -> {
            try {
                String response = SocketClient.sendRequest("{\"type\":\"GET_DEPARTMENTS\"}");
                System.out.println("[Student] GET_DEPARTMENTS response: " + response);

                Platform.runLater(() -> {
                    deptBox.setDisable(false);
                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        int start = response.indexOf("[");
                        int end   = response.lastIndexOf("]");
                        if (start >= 0 && end > start) {
                            String dataSection = response.substring(start + 1, end);
                            if (!dataSection.trim().isEmpty()) {
                                for (String item : dataSection.split(",")) {
                                    String val = item.trim().replace("\"", "");
                                    if (!val.isEmpty()) deptBox.getItems().add(val);
                                }
                                System.out.println("[Student] Loaded " + deptBox.getItems().size() + " departments.");
                            }
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Load Error",
                                "Could not load departments: " + response);
                    }
                    applyDeptBoxPromptFix();
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    deptBox.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Connection Error",
                            "Could not load departments: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onStartVotingPressed() {
        stdIdError.setVisible(false);
        nameError.setVisible(false);
        deptError.setVisible(false);

        String stdId = stdIdField.getText().trim();
        String name  = nameField.getText().trim();
        String dept  = deptBox.getValue();

        boolean anyEmpty = false;
        if (stdId.isEmpty()) {
            stdIdError.setText("The required information must be filled");
            stdIdError.setVisible(true);
            anyEmpty = true;
        }
        if (name.isEmpty()) {
            nameError.setText("The required information must be filled");
            nameError.setVisible(true);
            anyEmpty = true;
        }
        if (dept == null) {
            showAlert(Alert.AlertType.WARNING, "Department Required",
                    "Your department must be selected!");
            return;
        }
        if (anyEmpty) return;

        if (startVoting != null) startVoting.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                String json = "{\"type\":\"VERIFY_STD_VOTER\","
                        + "\"stdId\":\"" + stdId + "\","
                        + "\"name\":\""  + name  + "\","
                        + "\"dept\":\""  + dept  + "\"}";

                String response = SocketClient.sendRequest(json);
                System.out.println("[Student] VERIFY_STD_VOTER response: " + response);

                Platform.runLater(() -> {
                    if (startVoting != null) startVoting.setDisable(false);

                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        String voteCasted = extractField(response, "voteCasted");
                        if ("1".equals(voteCasted)) {
                            showAlert(Alert.AlertType.WARNING, "Already Voted",
                                    "This student has already cast their vote!");
                            clearForm();
                            return;
                        }
                        currentVoterId = stdId;
                        loadPostsAndBeginVoting();

                    } else {
                        String field   = extractField(response, "field");
                        String message = extractField(response, "message");

                        if ("stdId".equals(field)) {
                            stdIdError.setText(message);
                            stdIdError.setVisible(true);
                        } else if ("name".equals(field)) {
                            nameError.setText(message);
                            nameError.setVisible(true);
                        } else if ("dept".equals(field)) {
                            stdIdError.setText(message);
                            stdIdError.setVisible(true);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Error", message);
                        }
                    }
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (startVoting != null) startVoting.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Connection Error",
                            "Could not verify voter: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }


    private void loadPostsAndBeginVoting() {
        postList.clear();
        currentPostIndex = 0;

        Thread t = new Thread(() -> {
            try {
                String response = SocketClient.sendRequest("{\"type\":\"GET_STD_POSTS\"}");
                System.out.println("[Student] GET_STD_POSTS response: " + response);

                List<String> loaded = new ArrayList<>();
                if (response.contains("\"status\":\"SUCCESS\"")) {
                    int start = response.indexOf("[");
                    int end   = response.lastIndexOf("]");
                    if (start >= 0 && end > start) {
                        String dataSection = response.substring(start + 1, end);
                        if (!dataSection.trim().isEmpty()) {
                            for (String item : dataSection.split(",")) {
                                String val = item.trim().replace("\"", "");
                                if (!val.isEmpty()) loaded.add(val);
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (loaded.isEmpty()) {
                        showAlert(Alert.AlertType.INFORMATION, "No Posts",
                                "No posts found for this election.");
                        return;
                    }
                    postList.addAll(loaded);
                    showCurrentPost();
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Connection Error",
                                "Could not load posts: " + ex.getMessage())
                );
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showCurrentPost() {

        if (currentPostIndex >= postList.size()) {
            finishVoting();
            return;
        }

        String post = postList.get(currentPostIndex);

        postBox.getItems().clear();
        postBox.setValue(null);
        postBox.setDisable(true);
        applyPostBoxPromptFix();

        Thread t = new Thread(() -> {
            try {
                String response = SocketClient.sendRequest(
                        "{\"type\":\"GET_STD_CANDIDATES\","
                                + "\"post\":\"" + post + "\"}");
                System.out.println("[Student] GET_STD_CANDIDATES response: " + response);

                Platform.runLater(() -> {
                    postBox.setDisable(false);

                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        int start = response.indexOf("[");
                        int end   = response.lastIndexOf("]");
                        if (start >= 0 && end > start) {
                            String data = response.substring(start + 1, end);
                            if (!data.trim().isEmpty()) {
                                for (String entry : splitJsonArray(data)) {
                                    String name = extractField(entry, "name");
                                    String sign = extractField(entry, "sign");
                                    postBox.getItems().add(name + " - " + sign);
                                }
                            }
                        }
                    }

                    if (postBox.getItems().isEmpty()) {
                        currentPostIndex++;
                        showCurrentPost();
                        return;
                    }

                    postLabel.setText("Please put your vote for the post : " + post);
                    postLabel.setVisible(true);
                    selectLabel.setVisible(true);
                    postBox.setVisible(true);
                    voteBtn.setVisible(true);
                    applyPostBoxPromptFix();
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    postBox.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Connection Error",
                            "Could not load candidates: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onVotePressed() {
        String selected = postBox.getValue();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Candidate Selected",
                    "You must select a candidate!");
            return;
        }

        String post          = postList.get(currentPostIndex);
        String candidateName = selected.split(" - ")[0];

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Vote");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure? For the post \"" + post
                + "\" you want to vote for " + candidateName + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        if (voteBtn != null) voteBtn.setDisable(true);

        final String finalVoterId       = currentVoterId;
        final String finalPost          = post;
        final String finalCandidateName = candidateName;

        Thread t = new Thread(() -> {
            try {
                String json = "{\"type\":\"CAST_STD_VOTE\","
                        + "\"stdId\":\""         + finalVoterId       + "\","
                        + "\"post\":\""          + finalPost          + "\","
                        + "\"candidateName\":\"" + finalCandidateName + "\"}";

                String response = SocketClient.sendRequest(json);
                System.out.println("[Student] CAST_STD_VOTE response: " + response);

                Platform.runLater(() -> {
                    if (voteBtn != null) voteBtn.setDisable(false);

                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        currentPostIndex++;
                        if (currentPostIndex < postList.size()) {
                            showCurrentPost();
                        } else {
                            finishVoting();
                        }
                    } else {
                        String message = extractField(response, "message");
                        showAlert(Alert.AlertType.ERROR, "Vote Error",
                                message.isEmpty() ? "Could not cast vote. Please try again." : message);
                    }
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (voteBtn != null) voteBtn.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Connection Error",
                            "Could not send vote to server: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }


    private void finishVoting() {
        postLabel.setVisible(false);
        selectLabel.setVisible(false);
        postBox.setVisible(false);
        voteBtn.setVisible(false);

        final String finalVoterId = currentVoterId;

        Thread t = new Thread(() -> {
            try {
                String json = "{\"type\":\"FINISH_STD_VOTING\","
                        + "\"stdId\":\"" + finalVoterId + "\"}";
                SocketClient.sendRequest(json);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Vote Complete",
                        "Your vote has been cast successfully, thank you.");
                clearForm();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void clearForm() {
        stdIdField.clear();
        nameField.clear();

        deptBox.setValue(null);
        applyDeptBoxPromptFix();

        postBox.getItems().clear();
        postBox.setValue(null);
        applyPostBoxPromptFix();

        postLabel.setVisible(false);
        selectLabel.setVisible(false);
        postBox.setVisible(false);
        voteBtn.setVisible(false);
        stdIdError.setVisible(false);
        nameError.setVisible(false);
        deptError.setVisible(false);
        currentVoterId   = "";
        currentPostIndex = 0;
        postList.clear();
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
        } else {
            int end = start;
            while (end < json.length()
                    && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    private String[] splitJsonArray(String data) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) parts.add(data.substring(start, i + 1)); }
        }
        return parts.toArray(new String[0]);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}