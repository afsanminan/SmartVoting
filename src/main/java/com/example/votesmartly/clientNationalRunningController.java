



package com.example.votesmartly;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class clientNationalRunningController {

    @FXML public TextField nameField;
    @FXML public TextField voterIdField;
    @FXML public DatePicker dobField;
    @FXML public ComboBox<String> constBox;
    @FXML public ComboBox<String> candiBox;
    @FXML public Button voteDone;
    @FXML public Button endVote;
    @FXML public Label nameError;
    @FXML public Label idError;
    @FXML public Label timeLabel;
    //@FXML public Label electionId;
    @FXML public Label date;
    @FXML public Label dobError;

    private static final String CONST_PROMPT = "Select constituency";
    private static final String CANDI_PROMPT = "Select candidate";
    private static final String DATE_PROMPT  = "Date of Birth";

    private int currentElectionId;

    public void initialize() {
        setupConstituencyListener();
        startClock();
        date.setText(LocalDate.now().toString());
        applyConstBoxPromptFix();
        applyCandiBoxPromptFix();
        applyDatePickerFixes();
    }

    public void setElectionInfo(int electionId, int voterId, String type) {
        this.currentElectionId = electionId;
        loadConstituencies();
    }

    private void applyConstBoxPromptFix() {
        constBox.setPromptText(CONST_PROMPT);
        constBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(constBox.getPromptText());
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }

    private void applyCandiBoxPromptFix() {
        candiBox.setPromptText(CANDI_PROMPT);
        candiBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(candiBox.getPromptText());
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }

    private void applyDatePickerFixes() {
        dobField.setPromptText(DATE_PROMPT);

        dobField.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                Platform.runLater(() -> {
                    dobField.getEditor().clear();
                    dobField.getEditor().setPromptText(DATE_PROMPT);
                });
            }
        });

        dobField.setOnShown(event -> injectYearScrollField(dobField));

        dobField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(date.isAfter(LocalDate.now()));
            }
        });
    }

    private void injectYearScrollField(DatePicker picker) {
        javafx.scene.control.skin.DatePickerSkin skin =
                (javafx.scene.control.skin.DatePickerSkin) picker.getSkin();
        javafx.scene.layout.Region content =
                (javafx.scene.layout.Region) skin.getPopupContent();

        if (content.lookup("#yearScrollBar") != null) return;

        int initialYear = (picker.getValue() != null)
                ? picker.getValue().getYear()
                : LocalDate.now().getYear();

        TextField yearField = new TextField(String.valueOf(initialYear));
        yearField.setPrefWidth(72);
        yearField.setAlignment(Pos.CENTER);
        yearField.setStyle("-fx-font-size: 13px;");

        Runnable applyYear = () -> {
            try {
                int y = Integer.parseInt(yearField.getText().trim());
                int maxYear = LocalDate.now().getYear();
                if (y < 1900) y = 1900;
                if (y > maxYear) y = maxYear;
                int finalY = y;
                yearField.setText(String.valueOf(finalY));
                LocalDate base = picker.getValue() != null ? picker.getValue() : LocalDate.now();
                picker.setValue(base.withYear(finalY));
                picker.hide();
                picker.show();
            } catch (NumberFormatException ignored) {
                LocalDate cur = picker.getValue();
                yearField.setText(String.valueOf(cur != null ? cur.getYear() : LocalDate.now().getYear()));
            }
        };

        yearField.setOnScroll(ev -> {
            try {
                int y = Integer.parseInt(yearField.getText().trim());
                y += (ev.getDeltaY() > 0) ? 1 : -1;
                int maxYear = LocalDate.now().getYear();
                if (y < 1900) y = 1900;
                if (y > maxYear) y = maxYear;
                yearField.setText(String.valueOf(y));
                applyYear.run();
            } catch (NumberFormatException ignored) {}
        });

        yearField.setOnAction(ev -> applyYear.run());

        Label hint = new Label("scroll ↑↓ or type + Enter");
        hint.setStyle("-fx-font-size: 11px;");

        HBox bar = new HBox(8, new Label("Year:"), yearField, hint);
        bar.setId("yearScrollBar");
        bar.setPadding(new Insets(7, 10, 5, 10));
        bar.setAlignment(Pos.CENTER_LEFT);

        if (content instanceof VBox vb) {
            vb.getChildren().add(0, bar);
        } else {
            VBox wrapper = new VBox(bar, content);
            skin.getPopupContent().getScene().setRoot(wrapper);
        }
    }


    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e ->
                        timeLabel.setText(LocalTime.now().format(formatter))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }


    private void loadConstituencies() {

        constBox.setDisable(true);
        constBox.getItems().clear();

        Thread t = new Thread(() -> {
            try {
                String response = SocketClient.sendRequest("{\"type\":\"GET_CONSTITUENCIES\"}");
                System.out.println("[National] GET_CONSTITUENCIES response: " + response);

                Platform.runLater(() -> {
                    constBox.setDisable(false);
                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        int start = response.indexOf("[");
                        int end   = response.lastIndexOf("]");
                        if (start >= 0 && end > start) {
                            String dataSection = response.substring(start + 1, end);
                            if (!dataSection.trim().isEmpty()) {
                                java.util.List<String> items = new java.util.ArrayList<>();
                                for (String item : dataSection.split(",")) {
                                    String val = item.trim().replace("\"", "");
                                    if (!val.isEmpty()) items.add(val);
                                }
                                java.util.Collections.sort(items);
                                constBox.getItems().addAll(items);
                                System.out.println("[National] Loaded " + items.size() + " constituencies.");
                            }
                        }
                    } else {
                        showAlert(AlertType.ERROR, "Load Error",
                                "Could not load constituencies: " + response);
                    }
                    applyConstBoxPromptFix();
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    constBox.setDisable(false);
                    showAlert(AlertType.ERROR, "Connection Error",
                            "Constituency load failed: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }



    private void setupConstituencyListener() {
        constBox.setOnAction(event -> {
            String selected = constBox.getValue();
            if (selected == null) return;

            candiBox.getItems().clear();
            candiBox.setDisable(true);
            applyCandiBoxPromptFix();

            Thread t = new Thread(() -> {
                try {
                    String response = SocketClient.sendRequest(
                            "{\"type\":\"GET_NATIONAL_CANDIDATES\","
                                    + "\"constituency\":\"" + selected + "\"}");
                    System.out.println("[National] GET_NATIONAL_CANDIDATES response: " + response);

                    Platform.runLater(() -> {
                        candiBox.setDisable(false);
                        if (response.contains("\"status\":\"SUCCESS\"")) {
                            int start = response.indexOf("[");
                            int end   = response.lastIndexOf("]");
                            if (start >= 0 && end > start) {
                                String data = response.substring(start + 1, end);
                                if (!data.trim().isEmpty()) {
                                    for (String entry : splitJsonArray(data)) {
                                        String name = extractField(entry, "name");
                                        String sign = extractField(entry, "sign");
                                        candiBox.getItems().add(name + " - " + sign);
                                    }
                                }
                            }
                        }
                        applyCandiBoxPromptFix();
                    });

                } catch (IOException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        candiBox.setDisable(false);
                        showAlert(AlertType.ERROR, "Connection Error",
                                "Could not load candidates: " + ex.getMessage());
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });
    }


    @FXML
    public void onVotePressed() {
        nameError.setVisible(false);
        idError.setVisible(false);
        dobError.setVisible(false);

        String givenName       = nameField.getText().trim();
        String givenVoterId    = voterIdField.getText().trim();
        LocalDate selectedDate = dobField.getValue();
        String givenDob        = (selectedDate != null) ? selectedDate.toString() : "";
        String constituency    = constBox.getValue();
        String selectedCandidate = candiBox.getValue();

        boolean hasError = false;
        String required  = "You must provide the required information.";

        if (givenName.isEmpty()) {
            nameError.setText(required); nameError.setVisible(true); hasError = true;
        }
        if (givenVoterId.isEmpty()) {
            idError.setText(required); idError.setVisible(true); hasError = true;
        }
        if (givenDob.isEmpty()) {
            dobError.setText(required); dobError.setVisible(true); hasError = true;
        }
        if (constituency == null || selectedCandidate == null) {
            showAlert(AlertType.WARNING, "Missing Selection",
                    "Please select constituency and candidate.");
            hasError = true;
        }
        if (hasError) return;

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Vote");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Do you want to vote for: " + selectedCandidate);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String candidateName = selectedCandidate.split(" - ")[0];

        // Capture effectively-final copies for background thread
        final String finalVoterId       = givenVoterId;
        final String finalConstituency  = constituency;
        final String finalCandidateName = candidateName;
        final String finalName          = givenName;
        final String finalDob           = givenDob;

        if (voteDone != null) voteDone.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                String json = "{\"type\":\"CAST_NATIONAL_VOTE\","
                        + "\"voterId\":\""       + finalVoterId       + "\","
                        + "\"constituency\":\""  + finalConstituency  + "\","
                        + "\"candidateName\":\"" + finalCandidateName + "\","
                        + "\"givenName\":\""     + finalName          + "\","
                        + "\"givenDob\":\""      + finalDob           + "\"}";

                String response = SocketClient.sendRequest(json);
                System.out.println("[National] CAST_NATIONAL_VOTE response: " + response);

                Platform.runLater(() -> {
                    if (voteDone != null) voteDone.setDisable(false);
                    if (response.contains("\"status\":\"SUCCESS\"")) {
                        showAlert(AlertType.INFORMATION, "Success", "Vote cast successfully!");
                        clearForm();
                    } else {
                        String field   = extractField(response, "field");
                        String message = extractField(response, "message");
                        if ("name".equals(field)) {
                            nameError.setText(message); nameError.setVisible(true);
                        } else if ("dob".equals(field)) {
                            dobError.setText(message); dobError.setVisible(true);
                        } else {
                            idError.setText(message); idError.setVisible(true);
                        }
                    }
                });

            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (voteDone != null) voteDone.setDisable(false);
                    showAlert(AlertType.ERROR, "Connection Error",
                            "Could not send vote to server: " + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }


    private void clearForm() {
        nameField.clear();
        voterIdField.clear();
        dobField.setValue(null);
        Platform.runLater(() -> {
            dobField.getEditor().clear();
            dobField.getEditor().setPromptText(DATE_PROMPT);
        });
        constBox.setValue(null);
        applyConstBoxPromptFix();
        candiBox.getItems().clear();
        applyCandiBoxPromptFix();
        nameError.setVisible(false);
        idError.setVisible(false);
        dobError.setVisible(false);
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
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
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

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}