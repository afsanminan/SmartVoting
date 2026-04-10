//package com.example.votesmartly;
//
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.scene.chart.PieChart;
//import javafx.scene.control.*;
//import javafx.scene.control.cell.PropertyValueFactory;
//import java.sql.*;
//import java.util.*;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//
//public class nationalResultOverall {
//
//    @FXML public TableView<OverallModel> overallTable;
//    @FXML public TableColumn<OverallModel, Integer> constNoCol;
//    @FXML public TableColumn<OverallModel, String>  constNameCol;
//    @FXML public TableColumn<OverallModel, String>  winnerCol;
//    @FXML public TableColumn<OverallModel, String>  winnerSymbolCol;
//    @FXML public TableColumn<OverallModel, Integer> totalVoterCol;
//    @FXML public TableColumn<OverallModel, Integer> totalCandCol;
//    @FXML public TableColumn<OverallModel, Integer> voteCastedCol;
//    @FXML public TableColumn<OverallModel, String>  percentCol;
//
//    @FXML public TableView<SymbolModel> symbolTable;
//    @FXML public TableColumn<SymbolModel, String>  symbolName;
//    @FXML public TableColumn<SymbolModel, Integer> symbCandCol;
//    @FXML public TableColumn<SymbolModel, Integer> symbWinnerCol;
//    @FXML public TableColumn<SymbolModel, Integer> symbVoteRcvdCol;
//    @FXML public TableColumn<SymbolModel, String>  symbPercentCol;
//
//    @FXML public TextField symbolField;
//    @FXML public TextField constField;
//    @FXML public Label resultLabel;
//    @FXML public Label winnerLabel;
//    @FXML public Label totalVoterLabel;
//    @FXML public Label totalCandLabel;
//    @FXML public PieChart overallChart;
//    @FXML public PieChart symbolChart;
//    @FXML public Button individualBtn;
//    @FXML public Button backBtn;
//
//    private ObservableList<OverallModel> allOverall = FXCollections.observableArrayList();
//    private ObservableList<SymbolModel>  allSymbol  = FXCollections.observableArrayList();
//
//    @FXML
//    public void initialize() {
//        constNoCol     .setCellValueFactory(new PropertyValueFactory<>("constNo"));
//        constNameCol   .setCellValueFactory(new PropertyValueFactory<>("constName"));
//        winnerCol      .setCellValueFactory(new PropertyValueFactory<>("winner"));
//        winnerSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
//        totalVoterCol  .setCellValueFactory(new PropertyValueFactory<>("totalVoters"));
//        totalCandCol   .setCellValueFactory(new PropertyValueFactory<>("totalCand"));
//        voteCastedCol  .setCellValueFactory(new PropertyValueFactory<>("voteCasted"));
//        percentCol     .setCellValueFactory(new PropertyValueFactory<>("percent"));
//
//        symbolName     .setCellValueFactory(new PropertyValueFactory<>("symbol"));
//        symbCandCol    .setCellValueFactory(new PropertyValueFactory<>("candidates"));
//        symbWinnerCol  .setCellValueFactory(new PropertyValueFactory<>("winners"));
//        symbVoteRcvdCol.setCellValueFactory(new PropertyValueFactory<>("voteReceived"));
//        symbPercentCol .setCellValueFactory(new PropertyValueFactory<>("percent"));
//
//        loadResultLabel();
//        loadOverallTable();
//        loadSymbolTable();
//        applySearchFilters();
//    }
//
//    private void loadResultLabel() {
//        String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            if (rs.next()) resultLabel.setText("Result of " + rs.getString("name"));
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    private void loadOverallTable() {
//        String mainSql   = "SELECT * FROM national";
//        String winnerSql = "SELECT name, sign FROM candidate_national WHERE constituency=? ORDER BY vote_earned DESC LIMIT 1";
//
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement ps = conn.prepareStatement(mainSql);
//             ResultSet rs = ps.executeQuery()) {
//
//            int rowNo = 1, sumVoters = 0, sumCasted = 0;
//            Map<String, Integer> symbolWins = new HashMap<>();
//
//            while (rs.next()) {
//                String constituency = rs.getString("constituency");
//                int totalVoters = rs.getInt("total_voters");
//                int totalCand   = rs.getInt("total_candidates");
//                int voteCasted  = rs.getInt("vote_casted");
//
//                String winner = "N/A", winnerSymbol = "N/A";
//
//                try (PreparedStatement ps2 = conn.prepareStatement(winnerSql)) {
//                    ps2.setString(1, constituency);
//                    try (ResultSet rs2 = ps2.executeQuery()) {
//
//                        // ---- FIX: No votes → No Winner ----
//                        if (voteCasted == 0) {
//                            winner = "No Winner";
//                            winnerSymbol = "No Winner";
//                        }
//                        else if (rs2.next()) {
//                            winner       = rs2.getString("name");
//                            winnerSymbol = rs2.getString("sign");
//                            symbolWins.merge(winnerSymbol, 1, Integer::sum);
//                        }
//                    }
//                }
//
//                double percent = (totalVoters > 0) ? (voteCasted * 100.0) / totalVoters : 0;
//
//                allOverall.add(new OverallModel(
//                        rowNo++, constituency, winner, winnerSymbol,
//                        totalVoters, totalCand, voteCasted,
//                        String.format("%.2f%%", percent)));
//
//                sumVoters += totalVoters;
//                sumCasted += voteCasted;
//            }
//
//            overallTable.setItems(allOverall);
//            totalVoterLabel.setText("Total Voters : " + sumVoters);
//            totalCandLabel .setText("Total Vote Casted : " + sumCasted);
//            updateWinnerLabel(symbolWins);
//            loadOverallPie(sumVoters, sumCasted);
//
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    private void loadSymbolTable() {
//        try (Connection conn = DatabaseConnection.getConnection()) {
//
//            int totalVotesGlobal = 0;
//            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(vote_casted) AS total FROM national");
//                 ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) totalVotesGlobal = rs.getInt("total");
//            }
//
//            try (PreparedStatement psSym = conn.prepareStatement("SELECT DISTINCT sign FROM candidate_national");
//                 ResultSet rsSym = psSym.executeQuery()) {
//
//                while (rsSym.next()) {
//                    String symbol = rsSym.getString("sign");
//
//                    int candidates = 0;
//                    try (PreparedStatement p = conn.prepareStatement("SELECT COUNT(*) AS c FROM candidate_national WHERE sign=?")) {
//                        p.setString(1, symbol);
//                        try (ResultSet r = p.executeQuery()) { if (r.next()) candidates = r.getInt("c"); }
//                    }
//
//                    int winnersCount = 0;
//
//                    // ---- FIX: count only winners with vote > 0 ----
//                    String wq =
//                            "SELECT COUNT(*) AS w FROM (" +
//                                    "   SELECT constituency, MAX(vote_earned) AS mv " +
//                                    "   FROM candidate_national GROUP BY constituency" +
//                                    ") t JOIN candidate_national c " +
//                                    "ON c.vote_earned=t.mv AND c.constituency=t.constituency " +
//                                    "WHERE c.sign=? AND t.mv>0";
//
//                    try (PreparedStatement p = conn.prepareStatement(wq)) {
//                        p.setString(1, symbol);
//                        try (ResultSet r = p.executeQuery()) { if (r.next()) winnersCount = r.getInt("w"); }
//                    }
//
//                    int voteReceived = 0;
//                    try (PreparedStatement p = conn.prepareStatement("SELECT SUM(vote_earned) AS v FROM candidate_national WHERE sign=?")) {
//                        p.setString(1, symbol);
//                        try (ResultSet r = p.executeQuery()) { if (r.next()) voteReceived = r.getInt("v"); }
//                    }
//
//                    double percent = (totalVotesGlobal > 0) ?
//                            (voteReceived * 100.0) / totalVotesGlobal : 0;
//
//                    allSymbol.add(new SymbolModel(symbol, candidates, winnersCount, voteReceived,
//                            String.format("%.2f%%", percent)));
//                }
//            }
//
//            allSymbol.sort((a, b) -> Integer.compare(b.getWinners(), a.getWinners()));
//            symbolTable.setItems(allSymbol);
//            loadSymbolPie(allSymbol);
//
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//
//    // ---- FIXED WINNER LABEL ----
//    private void updateWinnerLabel(Map<String, Integer> wins) {
//
//        boolean allZero = wins.values().stream().allMatch(v -> v == 0);
//        if (wins.isEmpty() || allZero) {
//            winnerLabel.setText("No Winners");
//            return;
//        }
//
//        int max = wins.values().stream().max(Integer::compare).orElse(0);
//
//        List<String> topSymbols = new ArrayList<>();
//        for (var e : wins.entrySet()) {
//            if (Objects.equals(e.getValue(), max))
//                topSymbols.add(e.getKey());
//        }
//
//        if (topSymbols.size() == 1) {
//            winnerLabel.setText("Winning Political Party : " + topSymbols.get(0));
//        }
//        else {
//            winnerLabel.setText("Tie Between : " + String.join(", ", topSymbols));
//        }
//    }
//
//    private void loadOverallPie(int totalVoters, int totalCasted) {
//        if (totalVoters <= 0) return;
//        int uncasted = totalVoters - totalCasted;
//        overallChart.setData(FXCollections.observableArrayList(
//                new PieChart.Data(String.format("Casted (%.2f%%)",   (totalCasted  * 100.0) / totalVoters), totalCasted),
//                new PieChart.Data(String.format("Uncasted (%.2f%%)", (uncasted     * 100.0) / totalVoters), uncasted)
//        ));
//    }
//
//    private void loadSymbolPie(ObservableList<SymbolModel> list) {
//        double total = list.stream().mapToDouble(SymbolModel::getVoteReceived).sum();
//        if (total <= 0) return;
//
//        ObservableList<PieChart.Data> pie = FXCollections.observableArrayList();
//        double other = 0;
//
//        for (int i = 0; i < list.size(); i++) {
//            if (i < 5) pie.add(new PieChart.Data(
//                    String.format("%s (%.2f%%)", list.get(i).getSymbol(), list.get(i).getVoteReceived() * 100.0 / total),
//                    list.get(i).getVoteReceived()));
//            else other += list.get(i).getVoteReceived();
//        }
//
//        if (other > 0)
//            pie.add(new PieChart.Data(String.format("Others (%.2f%%)", other * 100.0 / total), other));
//
//        symbolChart.setData(pie);
//    }
//
//    private void applySearchFilters() {
//        constField.textProperty().addListener((obs, old, nw) ->
//                overallTable.setItems(allOverall.filtered(
//                        m -> m.getConstName().toLowerCase().contains(nw.toLowerCase()))));
//
//        symbolField.textProperty().addListener((obs, old, nw) ->
//                symbolTable.setItems(allSymbol.filtered(
//                        m -> m.getSymbol().toLowerCase().contains(nw.toLowerCase()))));
//    }
//
//    public static class OverallModel {
//        private final int    constNo, totalVoters, totalCand, voteCasted;
//        private final String constName, winner, symbol, percent;
//
//        public OverallModel(int no, String c, String w, String s, int tv, int tc, int vc, String p) {
//            constNo=no; constName=c; winner=w; symbol=s;
//            totalVoters=tv; totalCand=tc; voteCasted=vc; percent=p;
//        }
//        public int    getConstNo()     { return constNo; }
//        public String getConstName()   { return constName; }
//        public String getWinner()      { return winner; }
//        public String getSymbol()      { return symbol; }
//        public int    getTotalVoters() { return totalVoters; }
//        public int    getTotalCand()   { return totalCand; }
//        public int    getVoteCasted()  { return voteCasted; }
//        public String getPercent()     { return percent; }
//    }
//
//    public static class SymbolModel {
//        private final String symbol, percent;
//        private final int    candidates, winners, voteReceived;
//
//        public SymbolModel(String s, int c, int w, int v, String p) {
//            symbol=s; candidates=c; winners=w; voteReceived=v; percent=p;
//        }
//        public String getSymbol()       { return symbol; }
//        public int    getCandidates()   { return candidates; }
//        public int    getWinners()      { return winners; }
//        public int    getVoteReceived() { return voteReceived; }
//        public String getPercent()      { return percent; }
//    }
//
//    @FXML
//    public void onIndividualBtnClicked() {
//        try {
//            OverallModel selected = overallTable.getSelectionModel().getSelectedItem();
//            if (selected == null) {
//                Alert alert = new Alert(Alert.AlertType.WARNING);
//                alert.setTitle("No Selection");
//                alert.setHeaderText(null);
//                alert.setContentText("Please select a constituency first!");
//                alert.show();
//                return;
//            }
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("nationalIndividualResult.fxml"));
//            Parent root = loader.load();
//            nationalIndividualResultController controller = loader.getController();
//            controller.setConstituency(selected.getConstName());
//            controller.loadData();
//            Stage stage = (Stage) overallTable.getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//    public void onExit(ActionEvent e)
//    {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("Exit Confirmation");
//        alert.setHeaderText(null);
//        alert.setContentText("Are you sure you want to exit?");
//
//        if(alert.showAndWait().get() == ButtonType.OK)
//        {
//            System.exit(0);
//        }
//        // Cancel দিলে কিছুই হবে না
//    }
//}


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
import javafx.scene.layout.VBox;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
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

    // Progress overlay — add a StackPane overlay in your FXML named progressOverlay
    // containing a VBox with progressBar + progressLabel, hidden by default.
    // If you don't have it, the code degrades gracefully.
    @FXML public javafx.scene.layout.StackPane progressOverlay;
    @FXML public ProgressBar  progressBar;
    @FXML public Label        progressLabel;

    private ObservableList<OverallModel> allOverall = FXCollections.observableArrayList();
    private ObservableList<SymbolModel>  allSymbol  = FXCollections.observableArrayList();

    // Cached election info loaded once
    private String electionId   = null;
    private String electionName = null;
    private int    totalVotersGlobal = 0;
    private int    totalCastedGlobal = 0;

    @FXML
    public void initialize() {
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

        // Hide progress overlay initially
        if (progressOverlay != null) progressOverlay.setVisible(false);

        loadResultLabel();
        loadOverallTable();
        loadSymbolTable();
        applySearchFilters();
    }

    // =========================================================
    //  EXIT HANDLING  (called by both the X button and backBtn)
    // =========================================================

    /**
     * Call this from your window's close-request handler:
     *   stage.setOnCloseRequest(e -> { e.consume(); handleExit(); });
     * and from the back/exit button's onAction.
     */
    public void handleExit() {
        // Step 1 – ask about saving to history
        Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION);
        saveAlert.setTitle("Save to History");
        saveAlert.setHeaderText(null);
        saveAlert.setContentText("Do you want to save this election in history?");
        saveAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> saveResult = saveAlert.showAndWait();

        if (saveResult.isPresent() && saveResult.get() == ButtonType.YES) {
            // Save history, then show exit confirmation after
            saveToHistoryAsync();
        } else {
            // Skipped saving – go straight to exit confirmation
            showExitConfirmation();
        }
    }

    /** Shows the standard "Are you sure you want to exit?" dialog. */
    private void showExitConfirmation() {
        Alert exitAlert = new Alert(Alert.AlertType.CONFIRMATION);
        exitAlert.setTitle("Exit Confirmation");
        exitAlert.setHeaderText(null);
        exitAlert.setContentText("Are you sure you want to exit?");

        if (exitAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }

    // =========================================================
    //  SAVE TO HISTORY  (async with progress bar)
    // =========================================================

    private void saveToHistoryAsync() {
        // Disable all interactive controls while saving
        setControlsDisabled(true);
        if (progressOverlay != null) {
            progressOverlay.setVisible(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressLabel.setText("Saving election to history…");
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Fetching election data…");
                updateProgress(0, 4);

                // --- Gather data needed for the history record ---
                String  eid          = null;
                double  castPercent  = 0;

                try (Connection conn = DatabaseConnection.getConnection()) {

                    // 1. Get the latest election id & name
                    String infoSql = "SELECT id, name FROM election_info ORDER BY id_db DESC LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(infoSql);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            eid          = rs.getString("id");
                            electionId   = eid;
                            electionName = rs.getString("name");
                        }
                    }

                    if (eid == null) throw new Exception("No election found in election_info.");

                    // Total vote casted percentage
                    int tv = 0, tc = 0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT SUM(total_voters) AS tv, SUM(vote_casted) AS tc FROM national");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) { tv = rs.getInt("tv"); tc = rs.getInt("tc"); }
                    }
                    castPercent = (tv > 0) ? (tc * 100.0) / tv : 0;

                    updateProgress(1, 4);
                    updateMessage("Updating election_info…");

                    // 2. Mark history = 1 in election_info
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE election_info SET history=1 WHERE id=?")) {
                        ps.setString(1, eid);
                        ps.executeUpdate();
                    }

                    updateProgress(2, 4);
                    updateMessage("Inserting history record…");

                    // 3. Insert into history table
                    String doe = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO history (election_type, id, doe, percentage) VALUES (?,?,?,?)")) {
                        ps.setString(1, "National Election");
                        ps.setString(2, eid);
                        ps.setString(3, doe);
                        ps.setInt(4, (int) Math.round(castPercent));
                        ps.executeUpdate();
                    }

                    updateProgress(3, 4);
                    updateMessage("Writing text file…");

                    // 4. Build & write the constituency text file
                    writeConstituencyFile(conn, eid);

                    updateProgress(4, 4);
                    updateMessage("Done!");
                }
                return null;
            }
        };

        // Bind progress bar
        if (progressBar  != null) progressBar.progressProperty().bind(task.progressProperty());
        if (progressLabel != null) progressLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            // Unbind so we can set values freely again
            if (progressBar   != null) progressBar.progressProperty().unbind();
            if (progressLabel != null) progressLabel.textProperty().unbind();

            if (progressOverlay != null) progressOverlay.setVisible(false);
            setControlsDisabled(false);
            showExitConfirmation();
        });

        task.setOnFailed(e -> {
            if (progressBar   != null) progressBar.progressProperty().unbind();
            if (progressLabel != null) progressLabel.textProperty().unbind();

            if (progressOverlay != null) progressOverlay.setVisible(false);
            setControlsDisabled(false);

            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Save Failed");
            err.setHeaderText(null);
            err.setContentText("Could not save election history:\n" + task.getException().getMessage());
            err.showAndWait();
            // Still show exit confirmation so user isn't stuck
            showExitConfirmation();
        });

        new Thread(task).start();
    }

    /**
     * Writes  <electionId>.txt  with one line per constituency:
     *   constituency,winner,symbol,percent,totalVoters,totalCandidates
     * Multi-winner ties use  [Name1-Name2],[Sym1-Sym2]
     */
    private void writeConstituencyFile(Connection conn, String eid) throws Exception {
        String filePath = eid + ".txt";

        // All constituencies
        List<String> constituencies = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT constituency FROM national ORDER BY const_no");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) constituencies.add(rs.getString("constituency"));
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            for (String c : constituencies) {

                // Get constituency stats
                int totalVoters = 0, totalCand = 0, voteCasted = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT total_voters, total_candidates, vote_casted FROM national WHERE constituency=?")) {
                    ps.setString(1, c);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalVoters = rs.getInt("total_voters");
                            totalCand   = rs.getInt("total_candidates");
                            voteCasted  = rs.getInt("vote_casted");
                        }
                    }
                }

                int percent = (totalVoters > 0) ? (int) Math.round((voteCasted * 100.0) / totalVoters) : 0;

                // Find winner(s) — highest vote_earned in this constituency (>0)
                int maxVote = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT MAX(vote_earned) AS mv FROM candidate_national WHERE constituency=?")) {
                    ps.setString(1, c);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) maxVote = rs.getInt("mv");
                    }
                }

                String winnerStr, symbolStr;

                if (maxVote == 0) {
                    winnerStr = "No Winner";
                    symbolStr = "No Winner";
                } else {
                    List<String> names   = new ArrayList<>();
                    List<String> symbols = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT name, sign FROM candidate_national WHERE constituency=? AND vote_earned=?")) {
                        ps.setString(1, c);
                        ps.setInt(2, maxVote);
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

                pw.printf("%s,%s,%s,%d,%d,%d%n",
                        c, winnerStr, symbolStr, percent, totalVoters, totalCand);
            }
        }
    }

    /** Enables or disables all interactive controls. */
    private void setControlsDisabled(boolean disabled) {
        Platform.runLater(() -> {
            individualBtn .setDisable(disabled);
            backBtn       .setDisable(disabled);
            constField    .setDisable(disabled);
            symbolField   .setDisable(disabled);
            overallTable  .setDisable(disabled);
            symbolTable   .setDisable(disabled);
        });
    }

    // =========================================================
    //  ORIGINAL METHODS (unchanged except loadResultLabel caches id)
    // =========================================================

    private void loadResultLabel() {
        String sql = "SELECT id, name FROM election_info ORDER BY id_db DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                electionId   = rs.getString("id");
                electionName = rs.getString("name");
                resultLabel.setText("Result of " + electionName);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

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
                        if (voteCasted == 0) {
                            winner = "No Winner";
                            winnerSymbol = "No Winner";
                        } else if (rs2.next()) {
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

            totalVotersGlobal = sumVoters;
            totalCastedGlobal = sumCasted;

            overallTable.setItems(allOverall);
            totalVoterLabel.setText("Total Voters : " + sumVoters);
            totalCandLabel .setText("Total Vote Casted : " + sumCasted);
            updateWinnerLabel(symbolWins);
            loadOverallPie(sumVoters, sumCasted);

        } catch (Exception e) { e.printStackTrace(); }
    }

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
                    String wq =
                            "SELECT COUNT(*) AS w FROM (" +
                                    "   SELECT constituency, MAX(vote_earned) AS mv " +
                                    "   FROM candidate_national GROUP BY constituency" +
                                    ") t JOIN candidate_national c " +
                                    "ON c.vote_earned=t.mv AND c.constituency=t.constituency " +
                                    "WHERE c.sign=? AND t.mv>0";

                    try (PreparedStatement p = conn.prepareStatement(wq)) {
                        p.setString(1, symbol);
                        try (ResultSet r = p.executeQuery()) { if (r.next()) winnersCount = r.getInt("w"); }
                    }

                    int voteReceived = 0;
                    try (PreparedStatement p = conn.prepareStatement("SELECT SUM(vote_earned) AS v FROM candidate_national WHERE sign=?")) {
                        p.setString(1, symbol);
                        try (ResultSet r = p.executeQuery()) { if (r.next()) voteReceived = r.getInt("v"); }
                    }

                    double percent = (totalVotesGlobal > 0) ?
                            (voteReceived * 100.0) / totalVotesGlobal : 0;

                    allSymbol.add(new SymbolModel(symbol, candidates, winnersCount, voteReceived,
                            String.format("%.2f%%", percent)));
                }
            }

            allSymbol.sort((a, b) -> Integer.compare(b.getWinners(), a.getWinners()));
            symbolTable.setItems(allSymbol);
            loadSymbolPie(allSymbol);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateWinnerLabel(Map<String, Integer> wins) {
        boolean allZero = wins.values().stream().allMatch(v -> v == 0);
        if (wins.isEmpty() || allZero) { winnerLabel.setText("No Winners"); return; }

        int max = wins.values().stream().max(Integer::compare).orElse(0);
        List<String> topSymbols = new ArrayList<>();
        for (var e : wins.entrySet())
            if (Objects.equals(e.getValue(), max)) topSymbols.add(e.getKey());

        winnerLabel.setText(topSymbols.size() == 1
                ? "Winning Political Party : " + topSymbols.get(0)
                : "Tie Between : " + String.join(", ", topSymbols));
    }

    private void loadOverallPie(int totalVoters, int totalCasted) {
        if (totalVoters <= 0) return;
        int uncasted = totalVoters - totalCasted;
        overallChart.setData(FXCollections.observableArrayList(
                new PieChart.Data(String.format("Casted (%.2f%%)",   (totalCasted * 100.0) / totalVoters), totalCasted),
                new PieChart.Data(String.format("Uncasted (%.2f%%)", (uncasted    * 100.0) / totalVoters), uncasted)
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

        if (other > 0)
            pie.add(new PieChart.Data(String.format("Others (%.2f%%)", other * 100.0 / total), other));

        symbolChart.setData(pie);
    }

    private void applySearchFilters() {
        constField.textProperty().addListener((obs, old, nw) ->
                overallTable.setItems(allOverall.filtered(
                        m -> m.getConstName().toLowerCase().contains(nw.toLowerCase()))));

        symbolField.textProperty().addListener((obs, old, nw) ->
                symbolTable.setItems(allSymbol.filtered(
                        m -> m.getSymbol().toLowerCase().contains(nw.toLowerCase()))));
    }

    // =========================================================
    //  BUTTON HANDLERS
    // =========================================================

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

    /** Called by the back/exit button in FXML. */
    @FXML
    public void onExit(ActionEvent e) {
        handleExit();
    }

    // =========================================================
    //  MODEL CLASSES
    // =========================================================

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
        public int    getTotalVoters() { return totalVoters; }
        public int    getTotalCand()   { return totalCand; }
        public int    getVoteCasted()  { return voteCasted; }
        public String getPercent()     { return percent; }
    }

    public static class SymbolModel {
        private final String symbol, percent;
        private final int    candidates, winners, voteReceived;

        public SymbolModel(String s, int c, int w, int v, String p) {
            symbol=s; candidates=c; winners=w; voteReceived=v; percent=p;
        }
        public String getSymbol()       { return symbol; }
        public int    getCandidates()   { return candidates; }
        public int    getWinners()      { return winners; }
        public int    getVoteReceived() { return voteReceived; }
        public String getPercent()      { return percent; }
    }
}
