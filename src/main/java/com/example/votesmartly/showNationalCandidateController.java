package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import javafx.scene.control.Control;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class showNationalCandidateController implements Initializable {
    public static class Candidate {
        private String name;
        private String voter_Id;
        private String cand_Id;
        private String symbol;
        private String consti;

        public Candidate(String name, String voter_Id, String cand_Id, String symbol, String consti) {
            this.name = name;
            this.voter_Id = voter_Id;
            this.consti = consti;
            this.cand_Id = cand_Id;
            this.symbol= symbol;
        }
        public String getName() { return name; }
        public String getVoter_Id() { return voter_Id; }
        public String getCand_Id() { return cand_Id; }
        public String getConsti() { return consti; }
        public String getSymbol() {return symbol;}
    }
    @FXML public TableView<Candidate> cand_table;
    @FXML public TableColumn<Candidate, Integer> cand_no_col;
    @FXML public TableColumn<Candidate, String> name_col;
    @FXML public TableColumn<Candidate, String> voterId_col;
    @FXML public TableColumn<Candidate, String> candId_col;
    @FXML public TableColumn<Candidate, String> const_col;
    @FXML public TableColumn<Candidate,String> symbol_col;
    @FXML public TextField searchField;
    @FXML public ComboBox<String> constFilter;
    @FXML public Button backBtn;
    @FXML public ProgressIndicator deleteProgress;
    private ObservableList<Candidate> list = FXCollections.observableArrayList();
    public void loadData() {

        list.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst =
                     conn.prepareStatement("SELECT * FROM candidate_national");
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                list.add(new Candidate(
                        rs.getString("name"),
                        rs.getString("voter_id"),
                        rs.getString("candidate_id"),
                        rs.getString("sign"),
                        rs.getString("constituency")
                ));
            }
            rs.close();
            pst.close();
            conn.close();

            // Set table data
            cand_table.setItems(list);

            // Populate constituency filter
            ObservableList<String> constList = FXCollections.observableArrayList();

            for (Candidate c : list) {

                if (!constList.contains(c.getConsti())) {
                    constList.add(c.getConsti());
                }
            }

            constFilter.setItems(constList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ----------------------------
    // Initialize
    // ----------------------------
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Multiple row selection
        cand_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Column mapping
        name_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        voterId_col.setCellValueFactory(new PropertyValueFactory<>("voter_Id"));
        candId_col.setCellValueFactory(new PropertyValueFactory<>("cand_Id"));
        symbol_col.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        const_col.setCellValueFactory(new PropertyValueFactory<>("consti"));

        // Auto row numbering
        cand_no_col.setCellFactory(col -> new TableCell<Candidate, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty)
                    setText(null);
                else
                    setText(String.valueOf(getIndex() + 1));
            }
        });

        // Wrap long text
        name_col.setCellFactory(tc -> {
            TableCell<Candidate,String> cell = new TableCell<>();
            Text text = new Text();
            text.setFill(javafx.scene.paint.Color.WHITE);

            cell.setGraphic(text);
            text.wrappingWidthProperty().bind(name_col.widthProperty());
            text.textProperty().bind(cell.itemProperty());

            cand_table.setFixedCellSize(-1);

            return cell;
        });

        voterId_col.setCellFactory(tc -> {
            TableCell<Candidate,String> cell = new TableCell<>();
            Text text = new Text();
            text.setFill(javafx.scene.paint.Color.WHITE);

            cell.setGraphic(text);
            text.wrappingWidthProperty().bind(voterId_col.widthProperty());
            text.textProperty().bind(cell.itemProperty());

            return cell;
        });

        candId_col.setCellFactory(tc -> {
            TableCell<Candidate,String> cell = new TableCell<>();
            Text text = new Text();
            text.setFill(javafx.scene.paint.Color.WHITE);

            cell.setGraphic(text);
            text.wrappingWidthProperty().bind(candId_col.widthProperty());
            text.textProperty().bind(cell.itemProperty());

            return cell;
        });

        symbol_col.setCellFactory(tc -> {
            TableCell<Candidate,String> cell = new TableCell<>();
            Text text = new Text();
            text.setFill(javafx.scene.paint.Color.WHITE);

            cell.setGraphic(text);
            text.wrappingWidthProperty().bind(symbol_col.widthProperty());
            text.textProperty().bind(cell.itemProperty());

            return cell;
        });

        // Load data from DB
        loadData();

        // Filtered list
        FilteredList<Candidate> filteredData = new FilteredList<>(list, b -> true);

        // Search + Filter together
        Runnable updateFilter = () -> {

            String searchText = searchField.getText();
            String selectedConst = constFilter.getValue();

            filteredData.setPredicate(candidate -> {

                boolean matchesSearch = true;
                boolean matchesConst = true;

                // Search logic
                if (searchText != null && !searchText.isEmpty()) {

                    String keyword = searchText.toLowerCase();

                    matchesSearch =
                            candidate.getName().toLowerCase().contains(keyword) ||
                                    candidate.getVoter_Id().toLowerCase().contains(keyword) ||
                                    candidate.getCand_Id().toLowerCase().contains(keyword) ||
                                    candidate.getSymbol().toLowerCase().contains(keyword) ||
                                    candidate.getConsti().toLowerCase().contains(keyword);
                }

                // Constituency filter
                if (selectedConst != null && !selectedConst.isEmpty()) {
                    matchesConst = candidate.getConsti().equals(selectedConst);
                }

                return matchesSearch && matchesConst;
            });
        };

        // Search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        // Filter listener
        constFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        // Sorted list
        SortedList<Candidate> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(cand_table.comparatorProperty());

        cand_table.setItems(sortedData);
    }
    // ----------------------------
    // Delete Candidates
    // ----------------------------
    @FXML
    public void deleteCandidates() {

        ObservableList<Candidate> selected =
                FXCollections.observableArrayList(
                        cand_table.getSelectionModel().getSelectedItems());

        if (selected.isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select candidate(s) to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("These candidates will be permanently deleted.");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() == ButtonType.NO) {

            cand_table.getSelectionModel().clearSelection();
            return;
        }

        cand_table.setDisable(true);
        deleteProgress.setVisible(true);

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {

                try  {
                    Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement delete =
                            conn.prepareStatement(
                                    "DELETE FROM candidate_national WHERE candidate_id=?");

                    int total = selected.size();
                    int count = 0;

                    for (Candidate c : selected) {

                        delete.setString(1, c.getCand_Id());
                        delete.executeUpdate();
                        // Update total_candidates in national table
                        PreparedStatement updateNational =
                                conn.prepareStatement(
                                        "UPDATE national SET total_candidates = total_candidates - 1 WHERE constituency = ?");
                        updateNational.setString(1, c.getConsti());
                        updateNational.executeUpdate();
                        updateNational.close();
                        count++;
                        updateProgress(count, total);
                    }
                    delete.close();

                    conn.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        deleteProgress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {

            list.removeAll(selected);

            deleteProgress.progressProperty().unbind();
            deleteProgress.setVisible(false);

            cand_table.setDisable(false);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Success");
            success.setHeaderText(null);
            success.setContentText("Selected candidates deleted successfully.");
            success.showAndWait();
        });

        task.setOnFailed(e -> {

            deleteProgress.progressProperty().unbind();
            deleteProgress.setVisible(false);

            cand_table.setDisable(false);

            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }
    @FXML
    public void addCandidate(ActionEvent e) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("addNationalCandidate.fxml"));

            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) e.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    public void onBack() {

        try {
            Parent root =
                    FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));

            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
