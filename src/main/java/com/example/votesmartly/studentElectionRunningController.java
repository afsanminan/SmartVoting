package com.example.votesmartly;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
public class studentElectionRunningController {

    @FXML public Label electionIdLabel;
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

    // Holds all posts fetched from student table for the current voter's university
    private List<String> postList = new ArrayList<>();
    private int currentPostIndex = 0;
    private String currentVoterId = "";  // stores the std_id of the voter currently voting

    // ========================== INITIALIZE ==========================
    public void initialize() {
        startClock();
        loadDate();
        loadElectionId();
        loadDepartments();
    }

    // ========================== CLOCK ==========================
    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm:ss a");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> timeLabel.setText(LocalTime.now().format(fmt))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // ========================== DATE ==========================
    private void loadDate() {
        dateLabel.setText(LocalDate.now().toString());
    }

    // ========================== ELECTION ID ==========================
    private void loadElectionId() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                electionIdLabel.setText("Election ID : " + rs.getString("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================== LOAD DEPARTMENTS ==========================
    private void loadDepartments() {
        deptBox.getItems().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT dept FROM dept_for_std ORDER BY dept_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                deptBox.getItems().add(rs.getString("dept"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================== START VOTING BUTTON ==========================
    @FXML
    public void onStartVotingPressed() {
        // Reset all error labels
        stdIdError.setVisible(false);
        nameError.setVisible(false);
        deptError.setVisible(false);

        String stdId   = stdIdField.getText().trim();
        String name    = nameField.getText().trim();
        String dept    = deptBox.getValue();

        // ---- 1. Null / empty checks ----
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
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Department Required");
            alert.setHeaderText(null);
            alert.setContentText("Your department must be selected!");
            alert.showAndWait();
            return;
        }
        if (anyEmpty) return;

        // ---- 2. Check student ID exists ----
        String dbName = null;
        String dbDept = null;
        int voteCasted = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, dept, vote_casted FROM voter_std WHERE std_id = ?")) {
            ps.setString(1, stdId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                stdIdError.setText("This student Id does not exist!");
                stdIdError.setVisible(true);
                return;
            }
            dbName     = rs.getString("name");
            dbDept     = rs.getString("dept");
            voteCasted = rs.getInt("vote_casted");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // ---- 3. Name check ----
        boolean hasError = false;
        if (!dbName.equalsIgnoreCase(name)) {
            nameError.setText("The name does not match with the registered informations!");
            nameError.setVisible(true);
            hasError = true;
        }

        // ---- 4. Department check ----
        if (!dbDept.equalsIgnoreCase(dept)) {
            stdIdError.setText("No student exists with this id in this department!");
            stdIdError.setVisible(true);
            hasError = true;
        }

        if (hasError) return;

        // ---- 5. Already voted? ----
        if (voteCasted == 1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Already Voted");
            alert.setHeaderText(null);
            alert.setContentText("This student has already casted his vote!");
            alert.showAndWait();
            clearForm();
            return;
        }

        // ---- 6. All good — begin post-by-post voting ----
        currentVoterId = stdId;
        loadPostsAndBeginVoting(dept);
    }

    // ========================== LOAD POSTS ==========================
    private void loadPostsAndBeginVoting(String dept) {
        postList.clear();
        currentPostIndex = 0;

        // The `student` table has `posts` and `university` columns.
        // We fetch all posts available (university-wide, not dept-specific).
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT posts FROM student ORDER BY post_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                postList.add(rs.getString("posts"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (postList.isEmpty()) {
            showInfoAlert("No posts found for this election.");
            return;
        }

        showCurrentPost();
    }

    // ========================== SHOW CURRENT POST ==========================
    private void showCurrentPost() {
        if (currentPostIndex >= postList.size()) {
            // All posts done
            finishVoting();
            return;
        }

        String post = postList.get(currentPostIndex);
        postLabel.setText("Please put your vote for the post : " + post);
        postLabel.setVisible(true);
        selectLabel.setVisible(true);
        postBox.setVisible(true);
        voteBtn.setVisible(true);

        // Load candidates for this post
        postBox.getItems().clear();
        postBox.setValue(null);
        postBox.setPromptText("A better campus starts with your wise decision!");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, sign FROM candidate_std WHERE post_for_vote = ?")) {
            ps.setString(1, post);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                postBox.getItems().add(rs.getString("name") + " - " + rs.getString("sign"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================== VOTE BUTTON ==========================
    @FXML
    public void onVotePressed() {
        String selected = postBox.getValue();

        // Must select a candidate
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Candidate Selected");
            alert.setHeaderText(null);
            alert.setContentText("You must select a candidate!");
            alert.showAndWait();
            return;
        }

        String post          = postList.get(currentPostIndex);
        String candidateName = selected.split(" - ")[0];

        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Vote");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure? For the post \"" + post +
                "\" you want to vote for " + candidateName + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Cast vote for this candidate
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE candidate_std SET vote_earned = vote_earned + 1 " +
                             "WHERE name = ? AND post_for_vote = ?")) {
            ps.setString(1, candidateName);
            ps.setString(2, post);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Move to next post
        currentPostIndex++;
        if (currentPostIndex < postList.size()) {
            showCurrentPost();
        } else {
            finishVoting();
        }
    }

    // ========================== FINISH VOTING ==========================
    private void finishVoting() {
        // Hide voting section
        postLabel.setVisible(false);
        selectLabel.setVisible(false);
        postBox.setVisible(false);
        voteBtn.setVisible(false);

        // Mark voter as having voted
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE voter_std SET vote_casted = 1 WHERE std_id = ?")) {
            ps.setString(1, currentVoterId);
            ps.executeUpdate();
            PreparedStatement updateStudent = conn.prepareStatement(
                    "UPDATE student SET total_casted = total_casted + 1");
            updateStudent.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Success message
        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Vote Complete");
        done.setHeaderText(null);
        done.setContentText("Your vote has been casted successfully, thank you.");
        done.showAndWait();

        clearForm();
    }

    // ========================== CLEAR FORM ==========================
    private void clearForm() {
        stdIdField.clear();
        nameField.clear();
        deptBox.setValue(null);
        postBox.getItems().clear();
        postLabel.setVisible(false);
        selectLabel.setVisible(false);
        postBox.setVisible(false);
        voteBtn.setVisible(false);
        stdIdError.setVisible(false);
        nameError.setVisible(false);
        deptError.setVisible(false);
        currentVoterId = "";
        currentPostIndex = 0;
        postList.clear();
    }

    // ========================== HELPER ==========================
    private void showInfoAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10, idLabel, idField, passLabel, passField);
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
                    javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node)e.getSource()).getScene().getWindow();
                    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("showNationalResultOptions.fxml"));

                    stage.setScene(new javafx.scene.Scene(root));
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