package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.util.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class nationalResultOverall {

    @FXML public TableView<OverallModel> overallTable;
    @FXML public TableColumn<OverallModel, Integer> constNoCol;
    @FXML public TableColumn<OverallModel, String>  constNameCol;
    @FXML public TableColumn<OverallModel, String>  winnerCol;
    @FXML public TableColumn<OverallModel, String>  winnerSymbolCol;
    @FXML public TableColumn<OverallModel, Integer> totalVoterCol;
    @FXML public TableColumn<OverallModel, Integer> totalCandCol;
    @FXML public TableColumn<OverallModel, Integer> voteCastedCol;
    @FXML public TableColumn<OverallModel, String>  percentCol;

    @FXML public TableView<SymbolModel> symbolTable;
    @FXML public TableColumn<SymbolModel, String>  symbolName;
    @FXML public TableColumn<SymbolModel, Integer> symbCandCol;
    @FXML public TableColumn<SymbolModel, Integer> symbWinnerCol;
    @FXML public TableColumn<SymbolModel, Integer> symbVoteRcvdCol;
    @FXML public TableColumn<SymbolModel, String>  symbPercentCol;

    @FXML public TextField symbolField;
    @FXML public TextField constField;
    @FXML public Label resultLabel;
    @FXML public Label winnerLabel;
    @FXML public Label totalVoterLabel;
    @FXML public Label totalCandLabel;
    @FXML public PieChart overallChart;
    @FXML public PieChart symbolChart;
    @FXML public Button individualBtn;
    @FXML public Button backBtn;

    // ── FIX 2: keep full backup lists so search is non-destructive ──
    private ObservableList<OverallModel> allOverall = FXCollections.observableArrayList();
    private ObservableList<SymbolModel>  allSymbol  = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ── FIX 1: wire every column to its model property ──
        constNoCol     .setCellValueFactory(new PropertyValueFactory<>("constNo"));
        constNameCol   .setCellValueFactory(new PropertyValueFactory<>("constName"));
        winnerCol      .setCellValueFactory(new PropertyValueFactory<>("winner"));
        winnerSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        totalVoterCol  .setCellValueFactory(new PropertyValueFactory<>("totalVoters"));
        totalCandCol   .setCellValueFactory(new PropertyValueFactory<>("totalCand"));
        voteCastedCol  .setCellValueFactory(new PropertyValueFactory<>("voteCasted"));
        percentCol     .setCellValueFactory(new PropertyValueFactory<>("percent"));

        symbolName     .setCellValueFactory(new PropertyValueFactory<>("symbol"));
        symbCandCol    .setCellValueFactory(new PropertyValueFactory<>("candidates"));
        symbWinnerCol  .setCellValueFactory(new PropertyValueFactory<>("winners"));
        symbVoteRcvdCol.setCellValueFactory(new PropertyValueFactory<>("voteReceived"));
        symbPercentCol .setCellValueFactory(new PropertyValueFactory<>("percent"));

        loadResultLabel();
        loadOverallTable();
        loadSymbolTable();
        applySearchFilters();
    }

    // -------------------------------------------------------
    // 1. RESULT LABEL
    // -------------------------------------------------------
    private void loadResultLabel() {
        String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) resultLabel.setText("Result of " + rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // -------------------------------------------------------
    // 2. OVERALL TABLE
    // -------------------------------------------------------
    private void loadOverallTable() {
        String mainSql   = "SELECT * FROM national";
        String winnerSql = "SELECT name, sign FROM candidate_national WHERE constituency=? ORDER BY vote_earned DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(mainSql);
             ResultSet rs = ps.executeQuery()) {

            int rowNo = 1, sumVoters = 0, sumCasted = 0;
            Map<String, Integer> symbolWins = new HashMap<>();

            while (rs.next()) {
                String constituency = rs.getString("constituency");
                int totalVoters = rs.getInt("total_voters");
                int totalCand   = rs.getInt("total_candidates");
                int voteCasted  = rs.getInt("vote_casted");
                String winner = "N/A", winnerSymbol = "N/A";

                try (PreparedStatement ps2 = conn.prepareStatement(winnerSql)) {
                    ps2.setString(1, constituency);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            winner       = rs2.getString("name");
                            winnerSymbol = rs2.getString("sign");
                            symbolWins.merge(winnerSymbol, 1, Integer::sum);
                        }
                    }
                }

                double percent = (totalVoters > 0) ? (voteCasted * 100.0) / totalVoters : 0;
                allOverall.add(new OverallModel(
                        rowNo++, constituency, winner, winnerSymbol,
                        totalVoters, totalCand, voteCasted,
                        String.format("%.2f%%", percent)));

                sumVoters += totalVoters;
                sumCasted += voteCasted;
            }

            overallTable.setItems(allOverall);
            totalVoterLabel.setText("Total Voters : " + sumVoters);
            totalCandLabel .setText("Total Vote Casted : " + sumCasted);
            updateWinnerLabel(symbolWins);
            loadOverallPie(sumVoters, sumCasted);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // -------------------------------------------------------
    // 3. SYMBOL TABLE
    // -------------------------------------------------------
    private void loadSymbolTable() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            int totalVotesGlobal = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(vote_casted) AS total FROM national");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalVotesGlobal = rs.getInt("total");
            }

            try (PreparedStatement psSym = conn.prepareStatement("SELECT DISTINCT sign FROM candidate_national");
                 ResultSet rsSym = psSym.executeQuery()) {

                while (rsSym.next()) {
                    String symbol = rsSym.getString("sign");

                    int candidates = 0;
                    try (PreparedStatement p = conn.prepareStatement("SELECT COUNT(*) AS c FROM candidate_national WHERE sign=?")) {
                        p.setString(1, symbol);
                        try (ResultSet r = p.executeQuery()) { if (r.next()) candidates = r.getInt("c"); }
                    }

                    int winnersCount = 0;
                    String wq = "SELECT COUNT(*) AS w FROM (" +
                            "  SELECT constituency, MAX(vote_earned) AS mv FROM candidate_national GROUP BY constituency" +
                            ") t JOIN candidate_national c ON c.vote_earned=t.mv AND c.constituency=t.constituency WHERE c.sign=?";
                    try (PreparedStatement p = conn.prepareStatement(wq)) {
                        p.setString(1, symbol);
                        try (ResultSet r = p.executeQuery()) { if (r.next()) winnersCount = r.getInt("w"); }
                    }

                    int voteReceived = 0;
                    try (PreparedStatement p = conn.prepareStatement("SELECT SUM(vote_earned) AS v FROM candidate_national WHERE sign=?")) {
                        p.setString(1, symbol);
                        try (ResultSet r = p.executeQuery()) { if (r.next()) voteReceived = r.getInt("v"); }
                    }

                    double percent = (totalVotesGlobal > 0) ? (voteReceived * 100.0) / totalVotesGlobal : 0;
                    allSymbol.add(new SymbolModel(symbol, candidates, winnersCount, voteReceived,
                            String.format("%.2f%%", percent)));
                }
            }

            allSymbol.sort((a, b) -> Integer.compare(b.getWinners(), a.getWinners()));
            symbolTable.setItems(allSymbol);
            loadSymbolPie(allSymbol);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------
    private void updateWinnerLabel(Map<String, Integer> wins) {
        int max = 0; String best = ""; boolean tie = false;
        for (Map.Entry<String, Integer> e : wins.entrySet()) {
            if      (e.getValue() > max)              { max = e.getValue(); best = e.getKey(); tie = false; }
            else if (e.getValue() == max && max != 0) { tie = true; }
        }
        if      (max == 0) winnerLabel.setText("Winning Political Party : N/A");
        else if (tie)      winnerLabel.setText("Winning Political Party : It's a tie");
        else               winnerLabel.setText("Winning Political Party : " + best);
    }

    private void loadOverallPie(int totalVoters, int totalCasted) {
        if (totalVoters <= 0) return;
        int uncasted = totalVoters - totalCasted;
        overallChart.setData(FXCollections.observableArrayList(
                new PieChart.Data(String.format("Casted (%.2f%%)",   (totalCasted  * 100.0) / totalVoters), totalCasted),
                new PieChart.Data(String.format("Uncasted (%.2f%%)", (uncasted     * 100.0) / totalVoters), uncasted)
        ));
    }

    private void loadSymbolPie(ObservableList<SymbolModel> list) {
        double total = list.stream().mapToDouble(SymbolModel::getVoteReceived).sum();
        if (total <= 0) return;
        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList();
        double other = 0;
        for (int i = 0; i < list.size(); i++) {
            if (i < 5) pie.add(new PieChart.Data(
                    String.format("%s (%.2f%%)", list.get(i).getSymbol(), list.get(i).getVoteReceived() * 100.0 / total),
                    list.get(i).getVoteReceived()));
            else other += list.get(i).getVoteReceived();
        }
        if (other > 0) pie.add(new PieChart.Data(String.format("Others (%.2f%%)", other * 100.0 / total), other));
        symbolChart.setData(pie);
    }

    // ── FIX 2: filter against full backup lists ──
    private void applySearchFilters() {
        constField.textProperty().addListener((obs, old, nw) ->
                overallTable.setItems(allOverall.filtered(
                        m -> m.getConstName().toLowerCase().contains(nw.toLowerCase()))));

        symbolField.textProperty().addListener((obs, old, nw) ->
                symbolTable.setItems(allSymbol.filtered(
                        m -> m.getSymbol().toLowerCase().contains(nw.toLowerCase()))));
    }

    // -------------------------------------------------------
    // MODELS  –  FIX 3: added every missing getter
    // -------------------------------------------------------
    public static class OverallModel {
        private final int    constNo, totalVoters, totalCand, voteCasted;
        private final String constName, winner, symbol, percent;

        public OverallModel(int no, String c, String w, String s, int tv, int tc, int vc, String p) {
            constNo=no; constName=c; winner=w; symbol=s;
            totalVoters=tv; totalCand=tc; voteCasted=vc; percent=p;
        }
        public int    getConstNo()     { return constNo; }
        public String getConstName()   { return constName; }
        public String getWinner()      { return winner; }
        public String getSymbol()      { return symbol; }
        public int    getTotalVoters() { return totalVoters; }   // was missing
        public int    getTotalCand()   { return totalCand; }     // was missing
        public int    getVoteCasted()  { return voteCasted; }
        public String getPercent()     { return percent; }       // was missing
    }

    public static class SymbolModel {
        private final String symbol, percent;
        private final int    candidates, winners, voteReceived;

        public SymbolModel(String s, int c, int w, int v, String p) {
            symbol=s; candidates=c; winners=w; voteReceived=v; percent=p;
        }
        public String getSymbol()       { return symbol; }
        public int    getCandidates()   { return candidates; }   // was missing
        public int    getWinners()      { return winners; }      // was missing
        public int    getVoteReceived() { return voteReceived; }
        public String getPercent()      { return percent; }      // was missing
    }

    // -------------------------------------------------------
    // NAVIGATION
    // -------------------------------------------------------
    @FXML
    public void onIndividualBtnClicked() {
        try {
            OverallModel selected = overallTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Selection");
                alert.setHeaderText(null);
                alert.setContentText("Please select a constituency first!");
                alert.show();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("nationalIndividualResult.fxml"));
            Parent root = loader.load();
            nationalIndividualResultController controller = loader.getController();
            controller.setConstituency(selected.getConstName());
            controller.loadData();
            Stage stage = (Stage) overallTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}