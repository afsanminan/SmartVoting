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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
public class showNationalVoterController implements Initializable {

    public static class Voter {

        private String name;
        private String voter_Id;
        private String bday;
        private String consti;

        public Voter(String name, String bday, String voter_Id, String consti) {
            this.name = name;
            this.voter_Id = voter_Id;
            this.bday = bday;
            this.consti = consti;
        }

        public String getName() { return name; }
        public String getVoter_Id() { return voter_Id; }
        public String getBday() { return bday; }
        public String getConsti() { return consti; }
    }
    @FXML public TableView<Voter> voter_table;
    @FXML public TableColumn<Voter, Integer> voter_no_col;
    @FXML public TableColumn<Voter, String> name_col;
    @FXML public TableColumn<Voter, String> dob_col;
    @FXML public TableColumn<Voter, String> voter_id_col;
    @FXML public TableColumn<Voter, String> const_col;
    @FXML public TextField searchField;
    @FXML public ComboBox<String> constFilter;
    @FXML public Button backBtn;
    @FXML public ProgressIndicator deleteProgress;

    private ObservableList<Voter> list = FXCollections.observableArrayList();

    public void loadData() {

        list.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst =
                     conn.prepareStatement("SELECT * FROM voter_national");
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                list.add(new Voter(
                        rs.getString("name"),
                        rs.getString("birthday"),
                        rs.getString("voter_id"),
                        rs.getString("constituency")
                ));
            }

            voter_table.setItems(list);

            ObservableList<String> constList = FXCollections.observableArrayList();

            for(Voter v : list)
            {
                if(!constList.contains(v.getConsti()))
                    constList.add(v.getConsti());
            }

            constFilter.setItems(constList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        voter_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        name_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        voter_id_col.setCellValueFactory(new PropertyValueFactory<>("voter_Id"));
        dob_col.setCellValueFactory(new PropertyValueFactory<>("bday"));
        const_col.setCellValueFactory(new PropertyValueFactory<>("consti"));

        voter_no_col.setCellFactory(col -> new TableCell<Voter, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {

                super.updateItem(item, empty);

                if (empty)
                    setText(null);
                else
                    setText(String.valueOf(getIndex() + 1));
            }
        });

        name_col.setCellFactory(tc -> {
            TableCell<Voter, String> cell = new TableCell<>() {
                private final Label label = new Label();

                {
                    label.setWrapText(true);
                    label.setStyle("-fx-text-fill: white;");
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        label.setText(item);
                        setGraphic(label);
                    }
                }
            };
            return cell;
        });

        loadData();
        FilteredList<Voter> filteredData = new FilteredList<>(list, b -> true);

        Runnable updateFilter = () -> {

            String searchText = searchField.getText();
            String selectedConst = constFilter.getValue();

            filteredData.setPredicate(voter -> {

                boolean matchesSearch = true;
                boolean matchesConst = true;
                if (searchText != null && !searchText.isEmpty()) {

                    String keyword = searchText.toLowerCase();

                    matchesSearch =
                            voter.getName().toLowerCase().contains(keyword) ||
                                    voter.getVoter_Id().toLowerCase().contains(keyword) ||
                                    voter.getBday().toLowerCase().contains(keyword) ||
                                    voter.getConsti().toLowerCase().contains(keyword);
                }
                if (selectedConst != null && !selectedConst.isEmpty()) {
                    matchesConst = voter.getConsti().equals(selectedConst);
                }

                return matchesSearch && matchesConst;
            });
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        constFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        SortedList<Voter> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(voter_table.comparatorProperty());

        voter_table.setItems(sortedData);
    }
    @FXML
    public void addVoters(ActionEvent e) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("addNationalVoter.fxml"));

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
    public void removeVoters() {

        ObservableList<Voter> selectedVoters =
                FXCollections.observableArrayList(
                        voter_table.getSelectionModel().getSelectedItems()
                );

        if (selectedVoters.isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select voter(s) to delete.");
            alert.showAndWait();

            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "These voters will be permanently deleted from voters and candidate list."
        );

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() == ButtonType.NO) {

            voter_table.getSelectionModel().clearSelection();
            return;
        }

        voter_table.setDisable(true);
        deleteProgress.setVisible(true);

        Task<Void> deleteTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                try (Connection conn = DatabaseConnection.getConnection()) {

                    PreparedStatement deleteVoter =
                            conn.prepareStatement(
                                    "DELETE FROM voter_national WHERE voter_id=?"
                            );

                    PreparedStatement deleteCandidate =
                            conn.prepareStatement(
                                    "DELETE FROM candidate_national WHERE voter_id=?"
                            );
                    PreparedStatement updateVoterCount =
                            conn.prepareStatement(
                                    "UPDATE national SET total_voters = total_voters - 1 WHERE constituency=?"
                            );

                    PreparedStatement updateCandidateCount =
                            conn.prepareStatement(
                                    "UPDATE national SET total_candidates = total_candidates - 1 WHERE constituency=?"
                            );


                    int total = selectedVoters.size();
                    int count = 0;

                    for (Voter v : selectedVoters) {

                        String voterId = v.getVoter_Id();
                        String constituency = v.getConsti();
                        deleteCandidate.setString(1, voterId);
                        int candDeleted = deleteCandidate.executeUpdate();

                        if(candDeleted > 0)
                        {
                            updateCandidateCount.setString(1, constituency);
                            updateCandidateCount.executeUpdate();
                        }

                        deleteVoter.setString(1, voterId);
                        deleteVoter.executeUpdate();

                        updateVoterCount.setString(1, constituency);
                        updateVoterCount.executeUpdate();

                        count++;
                        updateProgress(count, total);
                    }
                }

                return null;
            }
        };

        deleteProgress.progressProperty().bind(deleteTask.progressProperty());

        deleteTask.setOnSucceeded(e -> {

            list.removeAll(selectedVoters);

            deleteProgress.progressProperty().unbind();
            deleteProgress.setVisible(false);

            voter_table.setDisable(false);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Success");
            success.setHeaderText(null);
            success.setContentText("Selected voters deleted successfully.");
            success.showAndWait();
        });

        deleteTask.setOnFailed(e -> {

            deleteProgress.progressProperty().unbind();
            deleteProgress.setVisible(false);

            voter_table.setDisable(false);

            deleteTask.getException().printStackTrace();
        });

        new Thread(deleteTask).start();
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