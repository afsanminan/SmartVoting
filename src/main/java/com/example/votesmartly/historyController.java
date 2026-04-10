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
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class historyController {

    @FXML public TableView<HistoryRow> historyTable;
    @FXML public TableColumn<HistoryRow, Integer> elect_no_col;
    @FXML public TableColumn<HistoryRow, String> elect_type_col;
    @FXML public TableColumn<HistoryRow, String> elect_id_col;
    @FXML public TableColumn<HistoryRow, String> elect_date_col;

    @FXML public Button backButton;
    @FXML public Button exitButton;
    @FXML public Button viewButton;
    @FXML public Button deleteButton;

    ObservableList<HistoryRow> list = FXCollections.observableArrayList();
    @FXML
    public void initialize() {

        elect_no_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("electionNo"));
        elect_type_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("electionType"));
        elect_id_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("electionId"));
        elect_date_col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("doe"));

        historyTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        loadHistory();
    }

    private void loadHistory() {

        list.clear();
        String sql = "SELECT election_type, doe, percentage, id FROM history";
        int counter = 1;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                list.add(new HistoryRow(
                        counter++,
                        rs.getString("election_type"),
                        rs.getString("id"),
                        rs.getString("doe"),
                        rs.getInt("percentage")
                ));
            }

            historyTable.setItems(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void viewSelected() throws IOException {
        HistoryRow selected = historyTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a row first.");
            return;
        }

        String type = selected.getElectionType();
        String electionId = selected.getElectionId();

        if (type.equalsIgnoreCase("Student Election")) {
            loadSceneWithParam("historyStd.fxml", electionId);
        }
        else if (type.equalsIgnoreCase("National Election")) {
            loadSceneWithParam("historyNational.fxml", electionId);
        }
        else {
            showAlert("Unknown election type: " + type);
        }
    }

    private void loadSceneWithParam(String fxmlName, String electionId) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
        Parent root = loader.load();

        if (fxmlName.equals("historyStd.fxml")) {
            historyStdController c = loader.getController();
            c.loadElection(electionId);
        } else {
            historyNationalController c = loader.getController();
            c.loadElection(electionId);
        }

        Stage stage = (Stage) historyTable.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void deleteSelected() {
        ObservableList<HistoryRow> rows = historyTable.getSelectionModel().getSelectedItems();

        if (rows.isEmpty()) {
            showAlert("Please select row(s) to delete.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            for (HistoryRow r : rows) {
                String sql = "DELETE FROM history WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, r.getElectionId());
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        loadHistory();
    }

    @FXML
    public void goBack(ActionEvent e) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("loginDone.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onExit(ActionEvent e)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to exit?");

        if(alert.showAndWait().get() == ButtonType.OK)
        {
            System.exit(0);
        }
    }


    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }


    public class HistoryRow {
        private int electionNo;
        private String electionType;
        private String electionId;
        private String doe;
        private int percentage;

        public HistoryRow(int electionNo, String electionType, String electionId, String doe, int percentage) {
            this.electionNo = electionNo;
            this.electionType = electionType;
            this.electionId = electionId;
            this.doe = doe;
            this.percentage = percentage;
        }

        public int getElectionNo() { return electionNo; }
        public String getElectionType() { return electionType; }
        public String getElectionId() { return electionId; }
        public String getDoe() { return doe; }
        public int getPercentage() { return percentage; }

        public void setElectionNo(int no) { this.electionNo = no; }
    }
}