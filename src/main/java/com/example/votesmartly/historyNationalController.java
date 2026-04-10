package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;

public class historyNationalController {

    @FXML public TableView<NationalRow> nationalTable;
    @FXML public TableColumn<NationalRow, Integer> const_no_col;
    @FXML public TableColumn<NationalRow, String> const_name_col;
    @FXML public TableColumn<NationalRow, String> winner_name_col;
    @FXML public TableColumn<NationalRow, String> winner_symbol_col;
    @FXML public TableColumn<NationalRow, Integer> percentage_col;
    @FXML public TableColumn<NationalRow, Integer> total_voter_col;
    @FXML public TableColumn<NationalRow, Integer> total_cand_col;

    @FXML public Button backBtn;
    @FXML public Button exitBtn;

    @FXML public Label histLabel;

    ObservableList<NationalRow> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {


        const_no_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("constNo"));
        const_name_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("constituency"));
        winner_name_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("winnerName"));
        winner_symbol_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("winnerSymbol"));
        percentage_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("percentage"));
        total_voter_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalVoters"));
        total_cand_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalCandidates"));


        winner_name_col.setCellFactory(col -> new TableCell<NationalRow, String>() {

        });

        const_no_col.setCellFactory(col -> new TableCell<NationalRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-text-fill: white;");
            }
        });

        const_name_col.setCellFactory(col -> new TableCell<NationalRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: white;");
            }
        });

        percentage_col.setCellFactory(col -> new TableCell<NationalRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-text-fill: white;");
            }
        });

        total_voter_col.setCellFactory(col -> new TableCell<NationalRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-text-fill: white;");
            }
        });

        total_cand_col.setCellFactory(col -> new TableCell<NationalRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle("-fx-text-fill: white;");
            }
        });
        winner_name_col.setCellFactory(col -> new TableCell<NationalRow, String>() {
            private final Text text = new Text();

            {
                text.setWrappingWidth(180); // adjust width
                text.setFill(javafx.scene.paint.Color.WHITE); // white text
                setGraphic(text);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    text.setText("");
                } else {
                    text.setText(item);
                }
            }
        });

        winner_symbol_col.setCellFactory(col -> new TableCell<NationalRow, String>() {
            private final Text text = new Text();

            {
                text.setWrappingWidth(140); // adjust width
                text.setFill(javafx.scene.paint.Color.WHITE); // white text
                setGraphic(text);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    text.setText("");
                } else {
                    text.setText(item);
                }
            }
        });

        const_no_col.setStyle("-fx-text-fill: white;");
        const_name_col.setStyle("-fx-text-fill: white;");
        total_voter_col.setStyle("-fx-text-fill: white;");
        total_cand_col.setStyle("-fx-text-fill: white;");
        percentage_col.setStyle("-fx-text-fill: white;");
    }

    public void loadElection(String electionId) {
        histLabel.setText("Election ID: " + electionId);
        String filename = electionId + ".txt";

        // Get absolute path relative to project root
        String projectRoot = System.getProperty("user.dir");
        File file = new File(projectRoot + File.separator + filename);

        System.out.println("Looking for file at: " + file.getAbsolutePath()); // debug line

        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        loadFileData(file);
    }

    private void loadFileData(File file) {

        list.clear();
        int counter = 1;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");

                if (parts.length != 6) {
                    System.out.println("Invalid line format: " + line);
                    continue;
                }
                String constituency = parts[0].trim();
                String winnerName = parts[1].trim();
                String winnerSymbol = parts[2].trim();
                int percentage = Integer.parseInt(parts[3].trim());
                int totalVoters = Integer.parseInt(parts[4].trim());
                int totalCandidates = Integer.parseInt(parts[5].trim());
                winnerName = winnerName.replace("[", "").replace("]", "");
                winnerSymbol = winnerSymbol.replace("[", "").replace("]", "");

                list.add(new NationalRow(
                        counter++,
                        constituency,
                        winnerName,
                        winnerSymbol,
                        percentage,
                        totalVoters,
                        totalCandidates
                ));
            }

            nationalTable.setItems(list);

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    @FXML
    public void exitApp() {
        System.exit(0);
    }

    public static class NationalRow {

        private int constNo;
        private String constituency;
        private String winnerName;
        private String winnerSymbol;
        private int percentage;
        private int totalVoters;
        private int totalCandidates;

        public NationalRow(int constNo, String constituency, String winnerName,
                           String winnerSymbol, int percentage, int totalVoters, int totalCandidates) {

            this.constNo = constNo;
            this.constituency = constituency;
            this.winnerName = winnerName;
            this.winnerSymbol = winnerSymbol;
            this.percentage = percentage;
            this.totalVoters = totalVoters;
            this.totalCandidates = totalCandidates;
        }

        public int getConstNo() { return constNo; }
        public String getConstituency() { return constituency; }
        public String getWinnerName() { return winnerName; }
        public String getWinnerSymbol() { return winnerSymbol; }
        public int getPercentage() { return percentage; }
        public int getTotalVoters() { return totalVoters; }
        public int getTotalCandidates() { return totalCandidates; }
    }
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
}