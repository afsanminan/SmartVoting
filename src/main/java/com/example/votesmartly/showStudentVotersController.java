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

public class showStudentVotersController implements Initializable {


    public static class Student {

        private String name;
        private String stdId;
        private String dept;

        public Student(String name, String stdId, String dept) {
            this.name = name;
            this.stdId = stdId;
            this.dept = dept;
        }

        public String getName() { return name; }
        public String getStdId() { return stdId; }
        public String getDept() { return dept; }
    }


    @FXML public TableView<Student> student_table;
    @FXML public TableColumn<Student,Integer> stdNo_col;
    @FXML public TableColumn<Student,String> name_col;
    @FXML public TableColumn<Student,String> stdId_col;
    @FXML public TableColumn<Student,String> dept_col;

    @FXML public TextField searchField;
    @FXML public ComboBox<String> deptFilter;

    @FXML public Button backBtn;
    @FXML public ProgressIndicator deleteProgress;

    private ObservableList<Student> list = FXCollections.observableArrayList();

    public void loadData() {

        list.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst =
                     conn.prepareStatement("SELECT * FROM voter_std");
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {

                list.add(new Student(
                        rs.getString("name"),
                        rs.getString("std_id"),
                        rs.getString("dept")
                ));
            }
            rs.close();
            pst.close();
            conn.close();
            student_table.setItems(list);

            ObservableList<String> deptList = FXCollections.observableArrayList();

            for(Student s : list)
            {
                if(!deptList.contains(s.getDept()))
                    deptList.add(s.getDept());
            }

            deptFilter.setItems(deptList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        student_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        name_col.setCellValueFactory(new PropertyValueFactory<>("name"));
        stdId_col.setCellValueFactory(new PropertyValueFactory<>("stdId"));
        dept_col.setCellValueFactory(new PropertyValueFactory<>("dept"));
        stdNo_col.setCellFactory(col -> new TableCell<Student,Integer>() {
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
        dept_col.setCellFactory(tc -> wrapCell(dept_col));

        loadData();
        FilteredList<Student> filteredData = new FilteredList<>(list,b->true);

        Runnable updateFilter = () -> {

            String searchText = searchField.getText();
            String dept = deptFilter.getValue();

            filteredData.setPredicate(student -> {

                boolean matchesSearch = true;
                boolean matchesDept = true;

                if(searchText!=null && !searchText.isEmpty())
                {
                    String keyword = searchText.toLowerCase();

                    matchesSearch =
                            student.getName().toLowerCase().contains(keyword) ||
                                    student.getStdId().toLowerCase().contains(keyword) ||
                                    student.getDept().toLowerCase().contains(keyword);
                }

                if(dept!=null && !dept.isEmpty())
                    matchesDept = student.getDept().equals(dept);

                return matchesSearch && matchesDept;
            });
        };

        searchField.textProperty().addListener((obs,oldVal,newVal)->updateFilter.run());
        deptFilter.valueProperty().addListener((obs,oldVal,newVal)->updateFilter.run());

        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(student_table.comparatorProperty());

        student_table.setItems(sortedData);
    }
    private TableCell<Student, String> wrapCell(TableColumn<Student, String> column) {
        TableCell<Student, String> cell = new TableCell<>();
        Text text = new Text();
        text.setFill(javafx.scene.paint.Color.WHITE);

        cell.setGraphic(text);
        text.wrappingWidthProperty().bind(column.widthProperty());
        text.textProperty().bind(cell.itemProperty());
        student_table.setFixedCellSize(-1);

        return cell;
    }
    @FXML
    public void deleteStudent() {

        ObservableList<Student> selected =
                FXCollections.observableArrayList(
                        student_table.getSelectionModel().getSelectedItems());

        if(selected.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setContentText("Select student(s) to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setContentText("Selected students will be removed permanently from voter and candidate database.");

        confirm.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();

        if(result.isEmpty() || result.get()==ButtonType.NO)
            return;

        student_table.setDisable(true);
        deleteProgress.setVisible(true);

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {

                try
                {
                    Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement deleteCandidate =
                            conn.prepareStatement(
                                    "DELETE FROM candidate_std WHERE std_id=?");

                    PreparedStatement deleteVoter =
                            conn.prepareStatement(
                                    "DELETE FROM voter_std WHERE std_id=?");

                    int total = selected.size();
                    int count=0;

                    for(Student s: selected)
                    {
                        deleteCandidate.setString(1,s.getStdId());
                        deleteCandidate.executeUpdate();

                        deleteVoter.setString(1,s.getStdId());
                        deleteVoter.executeUpdate();
                        // Update total_candidates in student table for the post if student was a candidate
                        PreparedStatement updatePost =
                                conn.prepareStatement(
                                        "UPDATE student SET total_candidates = total_candidates - 1 WHERE  posts = (SELECT post_for_vote FROM candidate_std WHERE std_id = ?)");

                        updatePost.setString(1, s.getStdId());
                        updatePost.executeUpdate();
                        updatePost.close();
                        count++;
                        updateProgress(count,total);
                    }
                    deleteCandidate.close();
                    deleteVoter.close();
                    conn.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        deleteProgress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e->{

            list.removeAll(selected);

            deleteProgress.setVisible(false);
            student_table.setDisable(false);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setContentText("Students deleted successfully.");
            success.showAndWait();
        });

        new Thread(task).start();
    }

    @FXML
    public void addStudent(ActionEvent e) {

        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("addStdVoter.fxml"));

            Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch(Exception ex) {
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

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}