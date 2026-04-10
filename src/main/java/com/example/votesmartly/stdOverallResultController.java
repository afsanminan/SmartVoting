

package com.example.votesmartly;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class stdOverallResultController {

    @FXML public Label electionIdLabel;
    @FXML public TableView<PostResultRow> overallTable;
    @FXML public TableColumn<PostResultRow, String> post_no_col;
    @FXML public TableColumn<PostResultRow, String> post_name_col;
    @FXML public TableColumn<PostResultRow, String> winner_col;
    @FXML public TableColumn<PostResultRow, String> winner_symbol_col;
    @FXML public TableColumn<PostResultRow, String> total_cand_col;

    @FXML public Label total_voters_label;
    @FXML public Label total_cand_label;
    @FXML public Label vote_casted_label;
    @FXML public Label percent_Label;

    @FXML public PieChart resultChart;
    @FXML public Button backBtn;
    @FXML public Button postResultButton;

    public static class PostResultRow {
        private final SimpleStringProperty postNo;
        private final SimpleStringProperty postName;
        private final SimpleStringProperty totalCandidates;
        private final SimpleStringProperty winner;
        private final SimpleStringProperty winnerSymbol;

        public PostResultRow(String postNo, String postName, String totalCandidates,
                             String winner, String winnerSymbol) {
            this.postNo          = new SimpleStringProperty(postNo);
            this.postName        = new SimpleStringProperty(postName);
            this.totalCandidates = new SimpleStringProperty(totalCandidates);
            this.winner          = new SimpleStringProperty(winner);
            this.winnerSymbol    = new SimpleStringProperty(winnerSymbol);
        }

        public String getPostNo()          { return postNo.get(); }
        public String getPostName()        { return postName.get(); }
        public String getTotalCandidates() { return totalCandidates.get(); }
        public String getWinner()          { return winner.get(); }
        public String getWinnerSymbol()    { return winnerSymbol.get(); }
    }

    public void initialize() {
        overallTable.setStyle("-fx-background-color: black; -fx-control-inner-background: black;");
        setupColumns();
        loadElectionName();
        loadTableData();
        loadSummaryStats();

        Platform.runLater(() -> {
            Stage stage = (Stage) electionIdLabel.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                event.consume();
                handleExit();
            });
        });
    }

    public void handleExit() {
        Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION);
        saveAlert.setTitle("Save to History");
        saveAlert.setHeaderText(null);
        saveAlert.setContentText("Do you want to save this election in history?");
        saveAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = saveAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            saveToHistoryAsync();
        } else {
            showExitConfirmation();
        }
    }

    private void showExitConfirmation() {
        Alert exitAlert = new Alert(Alert.AlertType.CONFIRMATION);
        exitAlert.setTitle("Exit Confirmation");
        exitAlert.setHeaderText(null);
        exitAlert.setContentText("Are you sure you want to exit?");

        if (exitAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }

    private void saveToHistoryAsync() {
        setControlsDisabled(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {

                    String eid = null;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM election_info ORDER BY id_db DESC LIMIT 1");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) eid = rs.getString("id");
                    }
                    if (eid == null) throw new Exception("No election found.");
                    long tv = 0, tc = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT SUM(total_voters), SUM(total_casted) FROM student");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { tv = rs.getLong(1); tc = rs.getLong(2); }
                    }

                    int percent = (tv > 0) ? (int) Math.round(tc * 100.0 / tv) : 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE election_info SET history=1 WHERE id=?")) {
                        ps.setString(1, eid);
                        ps.executeUpdate();
                    }

                    String date = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO history (election_type, id, doe, percentage) VALUES (?,?,?,?)")) {
                        ps.setString(1, "Student Election");
                        ps.setString(2, eid);
                        ps.setString(3, date);
                        ps.setInt(4, percent);
                        ps.executeUpdate();
                    }

                    writePostFile(conn, eid, tv, tc);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setControlsDisabled(false);
            showExitConfirmation();
        });

        task.setOnFailed(e -> {
            setControlsDisabled(false);
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Save Failed");
            err.setHeaderText(null);
            err.setContentText("Could not save election history:\n"
                    + task.getException().getMessage());
            err.showAndWait();
            showExitConfirmation();
        });

        new Thread(task).start();
    }


    private void writePostFile(Connection conn, String eid,
                               long totalVoters, long totalCasted) throws Exception {

        int overallPercent = (totalVoters > 0)
                ? (int) Math.round(totalCasted * 100.0 / totalVoters)
                : 0;

        List<String[]> posts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT posts, total_candidates FROM student ORDER BY post_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                posts.add(new String[]{ rs.getString("posts"),
                        String.valueOf(rs.getInt("total_candidates")) });
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(eid + ".txt"))) {
            for (String[] postInfo : posts) {
                String postName  = postInfo[0];
                String totalCand = postInfo[1];

                int maxVotes = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT MAX(vote_earned) FROM candidate_std WHERE post_for_vote=?")) {
                    ps.setString(1, postName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) maxVotes = rs.getInt(1);
                    }
                }

                String winnerStr, symbolStr;
                if (maxVotes == 0) {
                    winnerStr = "No Winner";
                    symbolStr = "No Winner";
                } else {
                    List<String> names   = new ArrayList<>();
                    List<String> symbols = new ArrayList<>();

                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT name, sign FROM candidate_std WHERE post_for_vote=? AND vote_earned=?")) {
                        ps.setString(1, postName);
                        ps.setInt(2, maxVotes);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                names  .add(rs.getString("name"));
                                symbols.add(rs.getString("sign"));
                            }
                        }
                    }

                    if (names.size() == 1) {
                        winnerStr = names.get(0);
                        symbolStr = symbols.get(0);
                    } else {
                        winnerStr = "[" + String.join("-", names)   + "]";
                        symbolStr = "[" + String.join("-", symbols) + "]";
                    }
                }
                pw.printf("%s,%s,%s,%d,%d,%s%n",
                        postName, winnerStr, symbolStr,
                        overallPercent, totalVoters, totalCand);
            }
        }
    }

    private void setControlsDisabled(boolean disabled) {
        Platform.runLater(() -> {
            backBtn.setDisable(disabled);
            postResultButton.setDisable(disabled);
            overallTable.setDisable(disabled);
        });
    }


    private void setupColumns() {
        post_no_col.setCellValueFactory(new PropertyValueFactory<>("postNo"));
        post_name_col.setCellValueFactory(new PropertyValueFactory<>("postName"));
        total_cand_col.setCellValueFactory(new PropertyValueFactory<>("totalCandidates"));

        winner_col.setCellValueFactory(new PropertyValueFactory<>("winner"));
        winner_col.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: white;");
                lbl.setAlignment(Pos.CENTER_LEFT);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.prefWidthProperty().bind(winner_col.widthProperty().subtract(16));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(item);
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        winner_symbol_col.setCellValueFactory(new PropertyValueFactory<>("winnerSymbol"));
        winner_symbol_col.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: white;");
                lbl.setAlignment(Pos.CENTER_LEFT);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.prefWidthProperty().bind(winner_symbol_col.widthProperty().subtract(16));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(item);
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
    }

    private void loadElectionName() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) electionIdLabel.setText("Result of " + rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }
    }


    private void loadTableData() {
        ObservableList<PostResultRow> rows = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT posts, total_candidates FROM student ORDER BY post_no");
             ResultSet rs = ps.executeQuery()) {

            int rowNum = 1;

            while (rs.next()) {
                String post      = rs.getString("posts");
                String totalCand = String.valueOf(rs.getInt("total_candidates"));

                // Find max votes for this post
                int maxVotes = 0;
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT MAX(vote_earned) FROM candidate_std WHERE post_for_vote=?")) {
                    ps2.setString(1, post);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) maxVotes = rs2.getInt(1);
                    }
                }

                String winnerText, symbolText;

                if (maxVotes == 0) {
                    winnerText = "No Winners";
                    symbolText = "-";
                } else {
                    StringBuilder winnerNames   = new StringBuilder();
                    StringBuilder winnerSymbols = new StringBuilder();

                    try (PreparedStatement ps3 = conn.prepareStatement(
                            "SELECT name, sign FROM candidate_std WHERE post_for_vote=? AND vote_earned=?")) {
                        ps3.setString(1, post);
                        ps3.setInt(2, maxVotes);
                        try (ResultSet rs3 = ps3.executeQuery()) {
                            boolean first = true;
                            while (rs3.next()) {
                                if (!first) { winnerNames.append("\n"); winnerSymbols.append("\n"); }
                                winnerNames  .append(rs3.getString("name"));
                                winnerSymbols.append(rs3.getString("sign"));
                                first = false;
                            }
                        }
                    }

                    winnerText = winnerNames  .length() > 0 ? winnerNames  .toString() : "No Winners";
                    symbolText = winnerSymbols.length() > 0 ? winnerSymbols.toString() : "-";
                }

                rows.add(new PostResultRow(
                        String.valueOf(rowNum++), post, totalCand, winnerText, symbolText));
            }

        } catch (Exception e) { e.printStackTrace(); }

        overallTable.setItems(rows);
    }

    private void loadSummaryStats() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT SUM(total_voters), SUM(total_candidates), SUM(total_casted) FROM student");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                long totalVoters     = rs.getLong(1);
                long totalCandidates = rs.getLong(2);
                long totalCasted     = rs.getLong(3);
                long uncasted        = totalVoters - totalCasted;

                double percentage = totalVoters > 0
                        ? (totalCasted * 100.0 / totalVoters) : 0.0;

                total_voters_label.setText("Total Voters : " + totalVoters);
                total_cand_label  .setText("Total Candidates : " + totalCandidates);
                vote_casted_label .setText("Total Vote Casted : " + totalCasted);
                percent_Label     .setText(String.format("Vote Casting Percentage : %.2f%%", percentage));

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                        new PieChart.Data(String.format("Voted (%.1f%%)", percentage),         totalCasted),
                        new PieChart.Data(String.format("Not Voted (%.1f%%)", 100.0 - percentage), uncasted)
                );

                resultChart.setData(pieData);
                resultChart.setLegendVisible(true);
                resultChart.setLabelsVisible(true);

                // Apply colors AFTER two layout passes so nodes are guaranteed non-null
                Platform.runLater(() -> Platform.runLater(() -> {
                    for (PieChart.Data d : resultChart.getData()) {
                        if (d.getNode() != null) {
                            if (d.getName().startsWith("Voted")) {
                                d.getNode().setStyle("-fx-pie-color: #4caf50;");
                            } else {
                                d.getNode().setStyle("-fx-pie-color: #f44336;");
                            }
                        }
                    }
                }));
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onPostResultButton() {
        PostResultRow selected = overallTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a post first!");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("stdIndividualResult.fxml"));
            Parent root = loader.load();

            stdIndividualResultController controller = loader.getController();
            controller.loadPost(selected.getPostName());

            Stage stage = (Stage) postResultButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onExit(ActionEvent e) {
        handleExit();
    }
}
