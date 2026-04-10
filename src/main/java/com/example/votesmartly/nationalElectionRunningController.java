
package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.sql.*;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class nationalElectionRunningController {

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
    @FXML public Label electionId;
    @FXML public Label date;
    @FXML public Label dobError;

    private static final String CONST_PROMPT  = "Select constituency";
    private static final String CANDI_PROMPT  = "Select candidate";
    private static final String DATE_PROMPT   = "Date of Birth";

    public void initialize() {
        loadConstituencies();
        setupConstituencyListener();
        loadLatestElectionId();
        startClock();
        blockWindowClose();
        applyConstBoxPromptFix();
        applyCandiBoxPromptFix();
        applyDatePickerFixes();
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
                javafx.application.Platform.runLater(() -> {
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
        yearField.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-color: -fx-background;" +
                        "-fx-border-color: -fx-box-border;" +
                        "-fx-border-radius: 3;" +
                        "-fx-background-radius: 3;"
        );

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
                yearField.setText(String.valueOf(
                        cur != null ? cur.getYear() : LocalDate.now().getYear()));
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
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-mid-text-color;");

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

    private void blockWindowClose() {
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) timeLabel.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent event) -> {
                event.consume();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Action Not Allowed");
                alert.setHeaderText(null);
                alert.setContentText("You cannot exit before finishing the election.\nPlease end the election properly using the 'End Vote' button.");
                alert.showAndWait();
            });
        });
    }

    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> timeLabel.setText(LocalTime.now().format(formatter))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void loadLatestElectionId() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                electionId.setText("Election ID : " + rs.getString("id"));
            }
            date.setText(LocalDate.now().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConstituencies() {
        constBox.getItems().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT constituency FROM national ORDER BY constituency ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                constBox.getItems().add(rs.getString("constituency"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupConstituencyListener() {
        constBox.setOnAction(event -> {
            candiBox.getItems().clear();
            applyCandiBoxPromptFix();

            String selected = constBox.getValue();
            if (selected == null) return;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT name, sign FROM candidate_national WHERE constituency=?")) {
                ps.setString(1, selected);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    candiBox.getItems().add(rs.getString("name") + " - " + rs.getString("sign"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void onVotePressed() {
        nameError.setVisible(false);
        idError.setVisible(false);
        dobError.setVisible(false);

        String givenName    = nameField.getText().trim();
        String givenVoterId = voterIdField.getText().trim();
        LocalDate selectedDate = dobField.getValue();
        String givenDob     = (selectedDate != null) ? selectedDate.toString() : "";
        String constituency = constBox.getValue();
        String selectedCandidate = candiBox.getValue();
        boolean hasEmptyError = false;
        String msg = "You must provide the required information.";

        if (givenName.isEmpty())    { nameError.setText(msg); nameError.setVisible(true); hasEmptyError = true; }
        if (givenVoterId.isEmpty()) { idError.setText(msg);   idError.setVisible(true);   hasEmptyError = true; }
        if (givenDob.isEmpty())     { dobError.setText(msg);  dobError.setVisible(true);  hasEmptyError = true; }

        if (constituency == null || selectedCandidate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select constituency and candidate.");
            alert.show();
            hasEmptyError = true;
        }
        if (hasEmptyError) return;

        String dbName = null, dbDob = null;
        int voteCasted = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, birthday, vote_casted FROM voter_national WHERE voter_id=? AND constituency=?")) {
            ps.setString(1, givenVoterId);
            ps.setString(2, constituency);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                idError.setText("No Voter with this ID exists in this constituency!");
                idError.setVisible(true);
                return;
            }
            dbName     = rs.getString("name");
            dbDob      = rs.getString("birthday");
            voteCasted = rs.getInt("vote_casted");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        boolean hasError = false;
        if (!dbName.equalsIgnoreCase(givenName)) {
            nameError.setText("Your name does not match with registered information!!");
            nameError.setVisible(true);
            hasError = true;
        }
        if (!dbDob.equalsIgnoreCase(givenDob)) {
            dobError.setText("Date of birth does not match with registered information!");
            dobError.setVisible(true);
            hasError = true;
        }
        if (hasError) return;

        if (voteCasted == 1) {
            idError.setText("This voter has already casted his vote!");
            idError.setVisible(true);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Vote");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Do you want to vote for: " + selectedCandidate);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String candidateName = selectedCandidate.split(" - ")[0];

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE candidate_national SET vote_earned = vote_earned + 1 WHERE name=? AND constituency=?");
            ps1.setString(1, candidateName); ps1.setString(2, constituency);
            ps1.executeUpdate(); ps1.close();

            PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE voter_national SET vote_casted=1 WHERE voter_id=?");
            ps2.setString(1, givenVoterId);
            ps2.executeUpdate(); ps2.close();

            PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE national SET vote_casted = vote_casted + 1 WHERE constituency=?");
            ps3.setString(1, constituency);
            ps3.executeUpdate(); ps3.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Success");
        done.setHeaderText("Vote casted successfully!");
        done.show();

        nameField.clear();
        voterIdField.clear();
        dobField.setValue(null);
        javafx.application.Platform.runLater(() -> {
            dobField.getEditor().clear();
            dobField.getEditor().setPromptText(DATE_PROMPT);
        });
        constBox.setValue(null);
        applyConstBoxPromptFix();
        candiBox.getItems().clear();
        applyCandiBoxPromptFix();
    }

    @FXML
    public void onEndBtn(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End Election");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to end the vote?\nOnly controller can end vote.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Authentication Required");

        Label idLabel   = new Label("Election ID:");
        TextField idField = new TextField();
        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        VBox vbox = new VBox(10, idLabel, idField, passLabel, passField);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> inputResult = dialog.showAndWait();
        if (inputResult.isEmpty() || inputResult.get() != ButtonType.OK) return;

        String givenId   = idField.getText().trim();
        String givenPass = passField.getText().trim();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, pass FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String dbId   = rs.getString("id");
                String dbPass = rs.getString("pass");

                if (dbId.equals(givenId) && dbPass.equals(givenPass)) {
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE election_info SET election_running = 0, success = 1 WHERE id = ?")) {
                        update.setString(1, dbId);
                        int rowsUpdated = update.executeUpdate();
                        if (rowsUpdated == 0) {
                            try (PreparedStatement fallback = conn.prepareStatement(
                                    "UPDATE election_info SET election_running = 0, success = 1 " +
                                            "WHERE id_db = (SELECT MAX(id_db) FROM election_info)")) {
                                fallback.executeUpdate();
                            }
                        }
                    }

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("showNationalResultOptions.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText(null);
                    error.setContentText("Election ID or Password does not match!");
                    error.show();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
