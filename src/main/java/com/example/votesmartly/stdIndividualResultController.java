package com.example.votesmartly;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class stdIndividualResultController {

    @FXML public Label postLabel;
    @FXML public Label winnerLabel;
    @FXML public Label symbolLabel;
    @FXML public TableView<CandRow> candTable;
    @FXML public TableColumn<CandRow, String> candNo_col;
    @FXML public TableColumn<CandRow, String> candName_col;
    @FXML public TableColumn<CandRow, String> symbol_col;
    @FXML public TableColumn<CandRow, String> voteEarned_col;
    @FXML public PieChart resultChart;
    @FXML public Label total_cand_label;
    @FXML public Button backBtn;

    public static class CandRow {
        private final SimpleStringProperty candNo;
        private final SimpleStringProperty name;
        private final SimpleStringProperty symbol;
        private final SimpleStringProperty voteEarned;

        public CandRow(String candNo, String name, String symbol, String voteEarned) {
            this.candNo     = new SimpleStringProperty(candNo);
            this.name       = new SimpleStringProperty(name);
            this.symbol     = new SimpleStringProperty(symbol);
            this.voteEarned = new SimpleStringProperty(voteEarned);
        }

        public String getCandNo()     { return candNo.get(); }
        public String getName()       { return name.get(); }
        public String getSymbol()     { return symbol.get(); }
        public String getVoteEarned() { return voteEarned.get(); }
    }

    private String getSliceColor(int index) {
        switch (index) {
            case 0: return "rgb(78,121,167)";   // blue
            case 1: return "rgb(242,142,43)";   // orange
            case 2: return "rgb(225,87,89)";    // red
            case 3: return "rgb(118,183,178)";  // teal
            case 4: return "rgb(89,161,79)";    // green
            case 5: return "rgb(176,122,161)";  // purple
            default: return "rgb(128,128,128)"; // fallback grey
        }
    }

    public void loadPost(String postName) {
        postLabel.setText("Result of Post : " + postName);
        setupColumns();
        loadCandidates(postName);
    }
    private void setupColumns() {
        candNo_col.setCellValueFactory(new PropertyValueFactory<>("candNo"));
        candName_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbol_col.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        voteEarned_col.setCellValueFactory(new PropertyValueFactory<>("voteEarned"));
    }

    private void loadCandidates(String postName) {
        ObservableList<CandRow> rows = FXCollections.observableArrayList();

        List<String> winnerNames   = new ArrayList<>();
        List<String> winnerSymbols = new ArrayList<>();
        int maxVotes = -1;

        List<String> pieNames  = new ArrayList<>();
        List<Long>   pieVotes  = new ArrayList<>();
        long totalCasted = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, sign, vote_earned " +
                             "FROM candidate_std " +
                             "WHERE post_for_vote = ? " +
                             "ORDER BY vote_earned DESC")) {

            ps.setString(1, postName);
            ResultSet rs = ps.executeQuery();

            int rowNum = 1;
            while (rs.next()) {
                String name  = rs.getString("name");
                String sign  = rs.getString("sign");
                int    votes = rs.getInt("vote_earned");

                rows.add(new CandRow(
                        String.valueOf(rowNum++),
                        name,
                        sign,
                        String.valueOf(votes)
                ));

                totalCasted += votes;
                pieNames.add(name);
                pieVotes.add((long) votes);

                if (votes > maxVotes) {
                    maxVotes = votes;
                    winnerNames.clear();
                    winnerSymbols.clear();
                    winnerNames.add(name);
                    winnerSymbols.add(sign);
                } else if (votes == maxVotes) {
                    winnerNames.add(name);
                    winnerSymbols.add(sign);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        candTable.setItems(rows);

        total_cand_label.setText("Total Candidates : " + rows.size());

        if (winnerNames.isEmpty()) {
            winnerLabel.setText("Winner : No votes yet");
            symbolLabel.setText("Symbol : -");
        } else {
            winnerLabel.setText("Winner : " + String.join(", ", winnerNames));
            symbolLabel.setText("Symbol : " + String.join(", ", winnerSymbols));
        }

        buildPieChart(pieNames, pieVotes, totalCasted);
    }

    private void buildPieChart(List<String> names, List<Long> votes, long totalCasted) {

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        if (totalCasted == 0) {
            pieData.add(new PieChart.Data("No votes cast", 1));
            resultChart.setData(pieData);
            return;
        }

        long othersVotes = 0;

        for (int i = 0; i < names.size(); i++) {
            if (i < 5) {
                long   v   = votes.get(i);
                double pct = v * 100.0 / totalCasted;
                String lbl = names.get(i) + " (" + Math.round(pct) + "%)";
                pieData.add(new PieChart.Data(lbl, v));
            } else {
                othersVotes += votes.get(i);
            }
        }

        if (othersVotes > 0) {
            double pct = othersVotes * 100.0 / totalCasted;
            pieData.add(new PieChart.Data("Others (" + Math.round(pct) + "%)", othersVotes));
        }

        resultChart.setData(pieData);
        resultChart.setLabelsVisible(true);
        resultChart.setLegendVisible(true);

        Platform.runLater(() -> {
            for (int i = 0; i < pieData.size(); i++) {
                PieChart.Data slice = pieData.get(i);
                if (slice.getNode() != null) {
                    slice.getNode().setStyle("-fx-pie-color: " + getSliceColor(i) + ";");
                }
            }
        });
    }
    public void onBack(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("stdOverallResult.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}