package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.io.IOException;

public class nationalIndividualResultController implements Initializable {


    @FXML public Label resultLabel;
    @FXML public Label totalVoterLabel;
    @FXML public Label totalCandLabel;
    @FXML public Label winnerLabel;
    @FXML public Label winnerSymbolLabel;
    @FXML public Label voteCastedLabel;
    @FXML public Label percentLabel;
    @FXML public Button backBtn;

    @FXML public TableView<CandidateModel> resultTable;
    @FXML public TableColumn<CandidateModel, Integer> candNoCol;
    @FXML public TableColumn<CandidateModel, String> candNameCol;
    @FXML public TableColumn<CandidateModel, String> symbolCol;
    @FXML public TableColumn<CandidateModel, Integer> voteRcvdCol;
    @FXML public TableColumn<CandidateModel, String> candIdCol;

    @FXML public PieChart resultChart;

    public static class CandidateModel {
        private int candNo;
        private String name;
        private String symbol;
        private String candidateId;
        private int voteEarned;

        public CandidateModel(int candNo, String name, String symbol, String candidateId, int voteEarned) {
            this.candNo = candNo;
            this.name = name;
            this.symbol = symbol;
            this.candidateId = candidateId;
            this.voteEarned = voteEarned;
        }

        public int getCandNo() { return candNo; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
        public String getCandidateId() { return candidateId; }
        public int getVoteEarned() { return voteEarned; }
    }

    private String constituency;

    public void setConstituency(String c) {
        this.constituency = c;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        candNoCol.setCellValueFactory(new PropertyValueFactory<>("candNo"));
        candNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        voteRcvdCol.setCellValueFactory(new PropertyValueFactory<>("voteEarned"));
        candIdCol.setCellValueFactory(new PropertyValueFactory<>("candidateId"));
    }

    public void loadData() {

        if (constituency == null) return;

        resultLabel.setText("Result of " + constituency);

        ObservableList<CandidateModel> list = FXCollections.observableArrayList();

        int totalVoters = 0;
        int totalCandidates = 0;
        int voteCasted = 0;

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                    "SELECT name, sign, candidate_id, vote_earned " +
                            "FROM candidate_national WHERE constituency = ? " +
                            "ORDER BY vote_earned DESC"
            );
            ps.setString(1, constituency);
            ResultSet rs = ps.executeQuery();

            int row = 1;
            while (rs.next()) {
                list.add(new CandidateModel(
                        row++,
                        rs.getString("name"),
                        rs.getString("sign"),
                        rs.getString("candidate_id"),
                        rs.getInt("vote_earned")
                ));
            }

            PreparedStatement ps2 = con.prepareStatement(
                    "SELECT total_voters, total_candidates, vote_casted " +
                            "FROM national WHERE constituency = ?"
            );
            ps2.setString(1, constituency);
            ResultSet rs2 = ps2.executeQuery();

            if (rs2.next()) {
                totalVoters = rs2.getInt("total_voters");
                totalCandidates = rs2.getInt("total_candidates");
                voteCasted = rs2.getInt("vote_casted");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        totalVoterLabel.setText("Total Voters : " + totalVoters);
        totalCandLabel.setText("Total Candidates : " + totalCandidates);
        voteCastedLabel.setText("Vote Casted : " + voteCasted);

        if (totalVoters > 0)
            percentLabel.setText(
                    "Vote Casting Percentage : " + String.format("%.2f%%", (voteCasted * 100.0) / totalVoters)
            );

        resultTable.setItems(list);

        if (!list.isEmpty()) {
            CandidateModel w = list.get(0);
            winnerLabel.setText("Winner : " + w.getName());
            winnerSymbolLabel.setText("Winning Symbol : " + w.getSymbol());
        }

        loadPieChart(list, voteCasted);
    }

    private void loadPieChart(ObservableList<CandidateModel> list, int voteCasted) {

        if (voteCasted <= 0) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        int othersVotes = 0;

        for (int i = 0; i < list.size(); i++) {
            CandidateModel c = list.get(i);

            if (i < 5) {
                double per = (c.getVoteEarned() * 100.0) / voteCasted;
                pieData.add(new PieChart.Data(
                        c.getSymbol() + " (" + String.format("%.2f%%", per) + ")",
                        per
                ));
            } else {
                othersVotes += c.getVoteEarned();
            }
        }

        if (othersVotes > 0) {
            double per = (othersVotes * 100.0) / voteCasted;
            pieData.add(new PieChart.Data(
                    "Others (" + String.format("%.2f%%", per) + ")",
                    per
            ));
        }

        resultChart.setData(pieData);
    }
    public void onBack(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("nationalResultOverall.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}