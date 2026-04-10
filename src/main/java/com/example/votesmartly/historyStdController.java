package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class historyStdController {

    @FXML public TableView<HistoryStdModel> stdTable;
    @FXML public TableColumn<HistoryStdModel, String> post_no_col;
    @FXML public TableColumn<HistoryStdModel, String> post_name_col;
    @FXML public TableColumn<HistoryStdModel, String> winner_name_col;
    @FXML public TableColumn<HistoryStdModel, String> winner_symbol_col;
    @FXML public TableColumn<HistoryStdModel, String> total_voter_col;
    @FXML public TableColumn<HistoryStdModel, String> total_cand_col;

    @FXML public Button backBtn;
    @FXML public Button exitBtn;
    @FXML public Label histLabel;
    @FXML
    public void initialize() {

        post_no_col      .setCellValueFactory(new PropertyValueFactory<>("postNo"));
        post_name_col    .setCellValueFactory(new PropertyValueFactory<>("postName"));
        winner_name_col  .setCellValueFactory(new PropertyValueFactory<>("winnerName"));
        winner_symbol_col.setCellValueFactory(new PropertyValueFactory<>("winnerSymbol"));
        total_voter_col  .setCellValueFactory(new PropertyValueFactory<>("totalVoter"));
        total_cand_col   .setCellValueFactory(new PropertyValueFactory<>("totalCand"));


        List<TableColumn<HistoryStdModel, String>> allCols = List.of(
                post_no_col, post_name_col, winner_name_col,
                winner_symbol_col,
                total_voter_col, total_cand_col
        );

        for (TableColumn<HistoryStdModel, String> col : allCols) {
            col.setCellFactory(c -> new TableCell<HistoryStdModel, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: white;");
                    }
                }
            });
        }
    }
    public void loadElection(String electionId) {

        String projectRoot = System.getProperty("user.dir");
        String path = projectRoot + java.io.File.separator + electionId + ".txt";

        System.out.println("Looking for std history file at: " + path);

        ObservableList<HistoryStdModel> list = FXCollections.observableArrayList();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String line;
            int rowNo = 1;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",");

                if (p.length < 6) {
                    System.out.println("Skipping invalid line: " + line);
                    continue;
                }

                String postName     = p[0].trim();
                String winnerName   = p[1].trim();
                String winnerSymbol = p[2].trim();
                String percentage   = p[3].trim();
                String totalVoter   = p[4].trim();
                String totalCand    = p[5].trim();

                winnerName   = winnerName  .replace("[", "").replace("]", "");
                winnerSymbol = winnerSymbol.replace("[", "").replace("]", "");

                HistoryStdModel row = new HistoryStdModel(
                        String.valueOf(rowNo++),
                        postName, winnerName, winnerSymbol,
                         totalVoter, totalCand
                );

                System.out.println("Row: " + row.getPostNo()
                        + " | " + row.getPostName()
                        + " | " + row.getWinnerName()
                        + " | " + row.getWinnerSymbol()
                        + " | " + row.getTotalVoter()
                        + " | " + row.getTotalCand());

                list.add(row);
            }

        } catch (Exception e) {
            System.out.println("File not found or error reading: " + path);
            e.printStackTrace();
        }

        System.out.println("Total rows loaded: " + list.size()); // debug

        histLabel.setText("Election ID: " + electionId);
        stdTable.setItems(list);
    }

    public static class HistoryStdModel {

        private final String postNo;
        private final String postName;
        private final String winnerName;
        private final String winnerSymbol;
        private final String totalVoter;
        private final String totalCand;

        public HistoryStdModel(String postNo, String postName, String winnerName,
                               String winnerSymbol,
                               String totalVoter, String totalCand) {
            this.postNo       = postNo;
            this.postName     = postName;
            this.winnerName   = winnerName;
            this.winnerSymbol = winnerSymbol;
            this.totalVoter   = totalVoter;
            this.totalCand    = totalCand;
        }

        public String getPostNo()       { return postNo; }
        public String getPostName()     { return postName; }
        public String getWinnerName()   { return winnerName; }
        public String getWinnerSymbol() { return winnerSymbol; }
        public String getTotalVoter()   { return totalVoter; }
        public String getTotalCand()    { return totalCand; }
    }

    @FXML
    public void onBack(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("history.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onExit(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to exit?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            System.exit(0);
        }
    }
}