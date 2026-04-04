package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.security.PublicKey;
import java.sql.*;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
public class nationalElectionRunningController {

    @FXML public TextField nameField;
    @FXML public TextField voterIdField;
    @FXML public TextField dobField;
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

    // -------------------------- INITIALIZE --------------------------
    public void initialize() {
        loadConstituencies();
        setupConstituencyListener();
        loadLatestElectionId();
        startClock();
    }

    // -------------------------- CLOCK --------------------------
    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");

        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    LocalTime now = LocalTime.now();
                    timeLabel.setText(now.format(formatter));
                }),
                new KeyFrame(Duration.seconds(1))
        );

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // -------------------------- LOAD ELECTION ID --------------------------
    private void loadLatestElectionId() {

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                electionId.setText("Election ID : " + rs.getString("id"));
            }

            // Current date
            java.time.LocalDate today = java.time.LocalDate.now();
            date.setText(today.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // -------------------------- LOAD CONSTITUENCIES --------------------------
    private void loadConstituencies() {
        constBox.getItems().clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT constituency FROM national");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                constBox.getItems().add(rs.getString("constituency"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------- AUTO LOAD CANDIDATES --------------------------
    private void setupConstituencyListener() {
        constBox.setOnAction(event -> {
            candiBox.getItems().clear();
            String selected = constBox.getValue();

            if (selected == null) return;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT name, sign FROM candidate_national WHERE constituency=?")) {

                ps.setString(1, selected);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String name = rs.getString("name");
                    String sign = rs.getString("sign");
                    candiBox.getItems().add(name + " - " + sign);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // -------------------------- AUTO FILL USER INFO WITH VOTER ID --------------------------


    // -------------------------- MAIN VOTE FUNCTION --------------------------
    @FXML
    public void onVotePressed() {

        nameError.setVisible(false);
        idError.setVisible(false);
        dobError.setVisible(false);

        String givenName = nameField.getText().trim();
        String givenVoterId = voterIdField.getText().trim();
        String givenDob = dobField.getText().trim();
        String constituency = constBox.getValue();
        String selectedCandidate = candiBox.getValue();
        boolean hasEmptyError = false;
        String msg = "You must provide the required information.";

        // ---------------------- 🔴 Individual Checks ----------------------

        if (givenName.isEmpty()) {
            nameError.setText(msg);
            nameError.setVisible(true);
            hasEmptyError = true;
        }

        if (givenVoterId.isEmpty()) {
            idError.setText(msg);
            idError.setVisible(true);
            hasEmptyError = true;
        }

        if (givenDob.isEmpty()) {
            dobError.setText(msg);
            dobError.setVisible(true);
            hasEmptyError = true;
        }

        // চাইলে ComboBox এর জন্যও Alert দিতে পারো
        if (constituency == null || selectedCandidate == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select constituency and candidate.");
            alert.show();
            hasEmptyError = true;
        }

        // 👉 সব check শেষে একবারে return
        if (hasEmptyError) return;

        String dbName = null;
        String dbDob = null;
        int voteCasted = 0;

        // ---------------------- 1. Check Voter Exists in Constituency ----------------------
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

            dbName = rs.getString("name");
            dbDob = rs.getString("birthday");
            voteCasted = rs.getInt("vote_casted");

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // ---------------------- 2. Name Matching ----------------------
        boolean hasError = false;

        if (!dbName.equalsIgnoreCase(givenName)) {
            nameError.setText("Your name does not match with registered information!!");
            nameError.setVisible(true);
            hasError = true;
        }

        // ---------------------- 3. DOB Matching ----------------------
        if (!dbDob.equalsIgnoreCase(givenDob)) {
            dobError.setText("Date of birth does not match with registered information!");
            dobError.setVisible(true);
            hasError = true;
        }

        if (hasError) return;

        // ---------------------- 4. Already Voted? ----------------------
        if (voteCasted == 1) {
            idError.setText("This voter has already casted his vote!");
            idError.setVisible(true);
            return;
        }

        // ---------------------- 5. Confirmation Popup ----------------------
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Vote");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Do you want to vote for: " + selectedCandidate);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String candidateName = selectedCandidate.split(" - ")[0];

        // ---------------------- 6. Perform Voting ----------------------
        try (Connection conn = DatabaseConnection.getConnection()) {

            PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE candidate_national SET vote_earned = vote_earned + 1 WHERE name=? AND constituency=?");
            ps1.setString(1, candidateName);
            ps1.setString(2, constituency);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE voter_national SET vote_casted=1 WHERE voter_id=?");
            ps2.setString(1, givenVoterId);
            ps2.executeUpdate();
            ps2.close();

            PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE national SET vote_casted = vote_casted + 1 WHERE constituency=?");
            ps3.setString(1, constituency);
            ps3.executeUpdate();
            ps3.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // ---------------------- 7. Success Popup ----------------------
        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Success");
        done.setHeaderText("Vote casted successfully!");
        done.show();

        // ---------------------- 8. Clear UI ----------------------
        nameField.clear();
        voterIdField.clear();
        dobField.clear();
        constBox.setValue(null);
        candiBox.getItems().clear();
    }
    public void onEndBtn(ActionEvent e)
    {
        // -------------------- 1. First Warning --------------------
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End Election");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to end the vote?\nOnly controller can end vote.");

        Optional<ButtonType> result = confirm.showAndWait();

        if(result.isEmpty() || result.get() != ButtonType.OK)
            return;

        // -------------------- 2. Input Dialog --------------------
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Authentication Required");

        Label idLabel = new Label("Election ID:");
        TextField idField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        VBox vbox = new VBox(10, idLabel, idField, passLabel, passField);
        dialog.getDialogPane().setContent(vbox);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> inputResult = dialog.showAndWait();

        if(inputResult.isEmpty() || inputResult.get() != ButtonType.OK)
            return;

        String givenId = idField.getText().trim();
        String givenPass = passField.getText().trim();

        // -------------------- 3. Match with DB --------------------
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, pass FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery())
        {
            if(rs.next())
            {
                String dbId = rs.getString("id");
                String dbPass = rs.getString("pass");

                if(dbId.equals(givenId) && dbPass.equals(givenPass))
                {
                    // ✅ MATCH → Go to result page
                    Stage stage = (Stage) ((javafx.scene.Node)e.getSource()).getScene().getWindow();
                    Parent root = FXMLLoader.load(getClass().getResource("showNationalResultOptions.fxml"));

                    stage.setScene(new Scene(root));
                    stage.show();
                }
                else
                {
                    // ❌ NOT MATCH
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText(null);
                    error.setContentText("Election ID or Password does not match!");
                    error.show();
                }
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}