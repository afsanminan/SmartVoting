package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.ResourceBundle;

public class showStudentCandidatesController implements Initializable {
    public static class StudentCand {

        private String name;
        private String stdId;
        private String candId;
        private String post;
        private String symbol;
        private String dept;

        public StudentCand(String name, String stdId, String candId, String post, String symbol, String dept) {
            this.name = name;
            this.stdId = stdId;
            this.candId = candId;
            this.post = post;
            this.symbol = symbol;
            this.dept = dept;
        }

        public String getName() { return name; }
        public String getStdId() { return stdId; }
        public String getCandId() { return candId; }
        public String getPost() { return post; }
        public String getSymbol() { return symbol; }
        public String getDept() { return dept; }
    }

    @FXML public TableView<StudentCand> studentCand_table;

    @FXML public TableColumn<StudentCand,Integer> candNo_col;
    @FXML public TableColumn<StudentCand,String> name_col;
    @FXML public TableColumn<StudentCand,String> stdId_col;
    @FXML public TableColumn<StudentCand,String> candId_col;
    @FXML public TableColumn<StudentCand,String> post_col;
    @FXML public TableColumn<StudentCand,String> symbol_col;
    @FXML public TableColumn<StudentCand,String> dept_col;

    @FXML public TextField searchField;
    @FXML public ComboBox<String> deptFilter;

    @FXML public Button backBtn;
    @FXML public ProgressIndicator deleteProgress;

    private ObservableList<StudentCand> list = FXCollections.observableArrayList();

    public void loadData() {

        list.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst =
                     conn.prepareStatement("SELECT * FROM candidate_std");
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                list.add(new StudentCand(
                        rs.getString("name"),
                        rs.getString("std_id"),
                        rs.getString("candidate_id"),
                        rs.getString("post_for_vote"),
                        rs.getString("sign"),
                        rs.getString("dept")
                ));
            }

            studentCand_table.setItems(list);
            ObservableList<String> deptList = FXCollections.observableArrayList();

            for(StudentCand c : list)
            {
                if(!deptList.contains(c.getDept()))
                    deptList.add(c.getDept());
            }

            deptFilter.setItems(deptList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        studentCand_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        name_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        stdId_col.setCellValueFactory(new PropertyValueFactory<>("stdId"));
        candId_col.setCellValueFactory(new PropertyValueFactory<>("candId"));
        post_col.setCellValueFactory(new PropertyValueFactory<>("post"));
        symbol_col.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        dept_col.setCellValueFactory(new PropertyValueFactory<>("dept"));

        candNo_col.setCellFactory(col -> new TableCell<StudentCand,Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {

                super.updateItem(item, empty);

                if(empty)
                    setText(null);
                else
                    setText(String.valueOf(getIndex()+1));
            }
        });

        name_col.setCellFactory(tc -> wrapCell(name_col));
        stdId_col.setCellFactory(tc -> wrapCell(stdId_col));
        candId_col.setCellFactory(tc -> wrapCell(candId_col));
        post_col.setCellFactory(tc -> wrapCell(post_col));
        symbol_col.setCellFactory(tc -> wrapCell(symbol_col));
        dept_col.setCellFactory(tc -> wrapCell(dept_col));
        loadData();

        FilteredList<StudentCand> filteredData = new FilteredList<>(list,b->true);

        Runnable updateFilter = () -> {

            String searchText = searchField.getText();
            String dept = deptFilter.getValue();

            filteredData.setPredicate(cand -> {

                boolean matchesSearch = true;
                boolean matchesDept = true;

                if(searchText!=null && !searchText.isEmpty())
                {
                    String keyword = searchText.toLowerCase();

                    matchesSearch =
                            cand.getName().toLowerCase().contains(keyword) ||
                                    cand.getStdId().toLowerCase().contains(keyword) ||
                                    cand.getCandId().toLowerCase().contains(keyword) ||
                                    cand.getPost().toLowerCase().contains(keyword) ||
                                    cand.getSymbol().toLowerCase().contains(keyword);
                }

                if(dept!=null && !dept.isEmpty())
                    matchesDept = cand.getDept().equals(dept);

                return matchesSearch && matchesDept;
            });
        };

        searchField.textProperty().addListener((obs,oldVal,newVal)->updateFilter.run());
        deptFilter.valueProperty().addListener((obs,oldVal,newVal)->updateFilter.run());

        SortedList<StudentCand> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentCand_table.comparatorProperty());

        studentCand_table.setItems(sortedData);
    }

    private TableCell<StudentCand, String> wrapCell(TableColumn<StudentCand, String> column) {
        TableCell<StudentCand, String> cell = new TableCell<>();
        Text text = new Text();

        text.setFill(javafx.scene.paint.Color.WHITE);

        cell.setGraphic(text);

        text.wrappingWidthProperty().bind(column.widthProperty());

        text.textProperty().bind(cell.itemProperty());

        studentCand_table.setFixedCellSize(-1);

        return cell;
    }
    @FXML
    public void deleteCandidate() {

        ObservableList<StudentCand> selected =
                FXCollections.observableArrayList(
                        studentCand_table.getSelectionModel().getSelectedItems());

        if(selected.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Select candidate(s) to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Selected candidates will be permanently deleted from candidate database.");
        confirm.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();

        if(result.isEmpty() || result.get()==ButtonType.NO)
            return;

        studentCand_table.setDisable(true);
        deleteProgress.setVisible(true);

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {

                try(Connection conn = DatabaseConnection.getConnection())
                {

                    PreparedStatement delete =
                            conn.prepareStatement(
                                    "DELETE FROM candidate_stdWHERE candidate_id=?");

                    int total = selected.size();

                    int count=0;
                    for(StudentCand c : selected)
                    {
                        delete.setString(1,c.getCandId());
                        delete.executeUpdate();
                        // Update total_candidates in student table for this post
                        PreparedStatement updatePost =
                                conn.prepareStatement(
                                        "UPDATE student SET total_candidates = total_candidates - 1 WHERE posts = ?");
                        updatePost.setString(1, c.getPost());
                        updatePost.executeUpdate();
                        updatePost.close();
                        count++;
                        updateProgress(count,total);
                    }
                }

                return null;
            }
        };

        deleteProgress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e->{

            list.removeAll(selected);

            deleteProgress.setVisible(false);
            studentCand_table.setDisable(false);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setContentText("Candidates deleted successfully.");
            success.showAndWait();
        });

        new Thread(task).start();
    }

    @FXML
    public void addCandidate(ActionEvent e) {

        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("addStdCandidate.fxml"));

            Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onBack() {

        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));

            Stage stage = (Stage)backBtn.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}