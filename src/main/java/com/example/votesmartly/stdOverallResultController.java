package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;

import java.sql.*;
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

    // ========================== ROW MODEL ==========================
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

    // ========================== INITIALIZE ==========================
    public void initialize() {
        setupColumns();
        loadElectionName();
        loadTableData();
        loadSummaryStats();
    }

    // ========================== COLUMN SETUP ==========================
    private void setupColumns() {
        post_no_col.setCellValueFactory(
                new PropertyValueFactory<>("postNo"));

        post_name_col.setCellValueFactory(
                new PropertyValueFactory<>("postName"));

        total_cand_col.setCellValueFactory(
                new PropertyValueFactory<>("totalCandidates"));

        // Winner column — white text, wrapped
        winner_col.setCellValueFactory(
                new PropertyValueFactory<>("winner"));
        winner_col.setCellFactory(col -> new TableCell<PostResultRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setWrapText(true);
                    lbl.setStyle("-fx-text-fill: white;");
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(lbl);
                    setText(null);
                    // auto row height
                    lbl.prefWidthProperty().bind(winner_col.widthProperty().subtract(10));
                }
            }
        });

        // Winner symbol column — white text, wrapped
        winner_symbol_col.setCellValueFactory(
                new PropertyValueFactory<>("winnerSymbol"));
        winner_symbol_col.setCellFactory(col -> new TableCell<PostResultRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setWrapText(true);
                    lbl.setStyle("-fx-text-fill: white;");
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(lbl);
                    setText(null);
                    lbl.prefWidthProperty().bind(winner_symbol_col.widthProperty().subtract(10));
                }
            }
        });

        // Make rows auto-size height
        overallTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(PostResultRow item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setPrefHeight(USE_COMPUTED_SIZE);
            }
        });
    }

    // ========================== ELECTION NAME ==========================
    private void loadElectionName() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                electionIdLabel.setText("Result of " + rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================== TABLE DATA ==========================
    private void loadTableData() {
        ObservableList<PostResultRow> rows = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT posts, total_candidates FROM student ORDER BY post_no");
             ResultSet rs = ps.executeQuery()) {

            int rowNum = 1;

            while (rs.next()) {
                String post           = rs.getString("posts");
                String totalCandidates = String.valueOf(rs.getInt("total_candidates"));

                // Find max votes for this post
                int maxVotes = 0;
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT MAX(vote_earned) FROM candidate_std WHERE post_for_vote = ?")) {
                    ps2.setString(1, post);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) maxVotes = rs2.getInt(1);
                }

                // Collect all winners (tie support)
                StringBuilder winnerNames   = new StringBuilder();
                StringBuilder winnerSymbols = new StringBuilder();

                try (PreparedStatement ps3 = conn.prepareStatement(
                        "SELECT name, sign FROM candidate_std WHERE post_for_vote = ? AND vote_earned = ?")) {
                    ps3.setString(1, post);
                    ps3.setInt(2, maxVotes);
                    ResultSet rs3 = ps3.executeQuery();

                    boolean first = true;
                    while (rs3.next()) {
                        if (!first) {
                            winnerNames.append("\n");
                            winnerSymbols.append("\n");
                        }
                        winnerNames.append(rs3.getString("name"));
                        winnerSymbols.append(rs3.getString("sign"));
                        first = false;
                    }
                }

                // If no votes cast yet, show placeholder
                String winnerText  = winnerNames.length() > 0 ? winnerNames.toString() : "No votes yet";
                String symbolText  = winnerSymbols.length() > 0 ? winnerSymbols.toString() : "-";

                rows.add(new PostResultRow(
                        String.valueOf(rowNum++),
                        post,
                        totalCandidates,
                        winnerText,
                        symbolText
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        overallTable.setItems(rows);
    }

    // ========================== SUMMARY STATS + PIE CHART ==========================
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
                        ? (totalCasted * 100.0 / totalVoters)
                        : 0.0;

                total_voters_label.setText(
                        "Total Voters : " + totalVoters);
                total_cand_label.setText(
                        "Total Candidates : " + totalCandidates);
                vote_casted_label.setText(
                        "Total Vote Casted : " + totalCasted);
                percent_Label.setText(
                        String.format("Vote Casting Percentage : %.2f%%", percentage));

                // Pie chart
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                        new PieChart.Data(
                                String.format("Voted (%.2f%%)", percentage), totalCasted),
                        new PieChart.Data(
                                String.format("Not Voted (%.2f%%)", 100.0 - percentage), uncasted)
                );
                resultChart.setData(pieData);
                resultChart.setLegendVisible(true);
                resultChart.setLabelsVisible(true);

                // Apply distinct colors after data is set
                javafx.application.Platform.runLater(() -> {
                    if (pieData.size() >= 2) {
                        pieData.get(0).getNode().setStyle("-fx-pie-color: #4caf50;"); // green = voted
                        pieData.get(1).getNode().setStyle("-fx-pie-color: #f44336;"); // red  = not voted
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        String postName = selected.getPostName();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("stdIndividualResult.fxml"));
            Parent root = loader.load();

            stdIndividualResultController controller = loader.getController();
            controller.loadPost(postName); // 🔥 pass post name

            Stage stage = (Stage) postResultButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}