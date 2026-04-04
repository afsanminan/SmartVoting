package com.example.votesmartly;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.PropertyValueFactory;
// SQL Imports
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.concurrent.Task;
import java.util.List;
import java.util.ArrayList;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
// JavaFX UI Controls
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.PropertyValueFactory;
import java.lang.Thread; // এটি সাধারণত অটোমেটিক থাকে, তবে সমস্যা করলে যোগ করতে পারেন

// JavaFX Collections
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
public class stdInfoController {

    private final String[] ALL_POSTS = {
            "Vice President(VP)","General Secretary(GS)",
            "Additional General Secretary(AGS)","Sports Secretary",
            "Literature and Cultural Secretary",
            "Science and Technology Secretary",
            "Common Room, Reading Room & Cafeteria Secretary",
            "International Affairs Secretary",
            "Research & Publication Secretary",
            "Student Transport Secretary",
            "Social Welfare Secretary",
            "Career Development Secretary",
            "Health & Environment Secretary"
    };
    public static class post_class{
        private String post_name;
        private int cand_in_post;
        public post_class(String name,int candi)
        {
            this.post_name=name;
            this.cand_in_post=candi;
        }

        public String getPost_name() {
            return post_name;
        }

        public int getCand_in_post() {
            return cand_in_post;
        }
    }
    public static class dept_class{
        private String dept_name;
        private int voter_in_dept;
        public dept_class(String name,int vot)
        {
            this.dept_name=name;
            this.voter_in_dept=vot;
        }

        public String getDeptName() {
            return dept_name;
        }

        public int getVoterDept() {
            return voter_in_dept;
        }
    }
    @FXML public TableView<post_class> post_table;
    @FXML public TableColumn<post_class,String>  post_name_col;
    @FXML public TableColumn<post_class,Integer> cand_in_post_col;
    @FXML public TableColumn<post_class,Integer> post_no;
    @FXML public TableView<dept_class> dept_table;
    @FXML public TableColumn<dept_class,String> dept_name_col;
    @FXML public TableColumn<dept_class,Integer> voter_in_dept_col;
    @FXML public TableColumn<dept_class,Integer> dept_no;
    @FXML public Label totalVoterLabel;
    @FXML public  Label totalCandLabel;
    @FXML public Button addPostBtn;
    @FXML public  Button deletePostBtn;
    @FXML public Button backBtn;

    @FXML
    public void initialize() {

        post_name_col.setCellValueFactory(new PropertyValueFactory<>("post_name"));
        cand_in_post_col.setCellValueFactory(new PropertyValueFactory<>("cand_in_post"));

        dept_name_col.setCellValueFactory(new PropertyValueFactory<>("deptName"));
        voter_in_dept_col.setCellValueFactory(new PropertyValueFactory<>("voterDept"));

        // multiple selection enable
        post_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        post_no.setCellFactory(col -> new TableCell<>(){
            @Override
            protected void updateItem(Integer item, boolean empty)
            {
                super.updateItem(item, empty);

                if(empty)
                    setText(null);
                else
                    setText(String.valueOf(getIndex()+1));
            }
        });

        loadPosts();
        loadDepartments();
    }
    private void loadPosts()
    {
        ObservableList<post_class> list = FXCollections.observableArrayList();

        String sql = "SELECT posts, total_candidates FROM student";

        try(Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery())
        {

            while(rs.next())
            {
                String post = rs.getString("posts");
                int cand = rs.getInt("total_candidates");

                list.add(new post_class(post,cand));
            }

            post_table.setItems(list);

        }catch(Exception e){
            e.printStackTrace();
        }

        updateTotalCandidates();
    }
    private void updateTotalCandidates()
    {
        String sql = "SELECT COUNT(*) FROM candidate_std";

        try(Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery())
        {

            if(rs.next())
            {
                totalCandLabel.setText(String.valueOf(rs.getInt(1)));
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void loadDepartments()
    {
        ObservableList<dept_class> list = FXCollections.observableArrayList();

        String sql = """
        SELECT d.dept,
        (SELECT COUNT(*) FROM voter_std v WHERE v.dept=d.dept) AS total
        FROM dept_for_std d
        """;

        try(Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery())
        {
            int totalVoter = 0;

            while(rs.next())
            {
                String dept = rs.getString("dept");
                int vot = rs.getInt("total");

                totalVoter += vot;

                list.add(new dept_class(dept,vot));
            }

            dept_table.setItems(list);

            totalVoterLabel.setText(String.valueOf(totalVoter));

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @FXML
    private void deletePost()
    {
        ObservableList<post_class> selected =
                post_table.getSelectionModel().getSelectedItems();

        if(selected.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("No post selected");
            alert.show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete Confirmation");
        confirm.setContentText(
                "Selected posts and their candidates will be permanently deleted.\nContinue?"
        );

        if(confirm.showAndWait().get() == ButtonType.OK)
        {
            deletePostsTask(selected);
        }
    }
    private void deletePostsTask(ObservableList<post_class> selected)
    {
        Task<Void> task = new Task<>()
        {
            @Override
            protected Void call() throws Exception
            {

                try(Connection con = DatabaseConnection.getConnection())
                {

                    String delCand = "DELETE FROM candidate_std WHERE post_for_vote=?";
                    String delPost = "DELETE FROM student WHERE posts=?";

                    PreparedStatement ps1 = con.prepareStatement(delCand);
                    PreparedStatement ps2 = con.prepareStatement(delPost);

                    int total = selected.size();
                    int i=0;

                    for(post_class p : selected)
                    {

                        ps1.setString(1,p.getPost_name());
                        ps1.executeUpdate();

                        ps2.setString(1,p.getPost_name());
                        ps2.executeUpdate();

                        i++;
                        updateProgress(i,total);
                    }

                }

                return null;
            }
        };

        task.setOnSucceeded(e->{

            loadPosts();
            updateTotalCandidates();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setContentText("Deletion successful");
            ok.show();

        });

        new Thread(task).start();
    }
    @FXML
    private void addPostBtnClicked()
    {
        showAddPostPopup();
    }
    private void showAddPostPopup()
    {
        List<String> availablePosts = new ArrayList<>();

        try(Connection con = DatabaseConnection.getConnection())
        {
            String sql = "SELECT posts FROM student";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            List<String> existing = new ArrayList<>();

            while(rs.next())
                existing.add(rs.getString("posts"));

            for(String p : ALL_POSTS)
            {
                if(!existing.contains(p))
                    availablePosts.add(p);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        Stage stage = new Stage();
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label label = new Label("Select the posts you want to add");

        // যদি সব post already added থাকে
        if(availablePosts.isEmpty())
        {
            Label msg = new Label("All the posts are already added.");

            Button ok = new Button("OK");
            ok.setOnAction(e -> stage.close());

            root.getChildren().addAll(msg, ok);

            stage.setScene(new Scene(root,300,120));
            stage.show();

            return;
        }

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(availablePosts);

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Button add = new Button("Add");
        Button cancel = new Button("Cancel");

        HBox buttons = new HBox(10,add,cancel);

        root.getChildren().addAll(label,listView,buttons);

        stage.setScene(new Scene(root,350,350));
        stage.show();
        cancel.setOnAction(e -> stage.close());
        add.setOnAction(e -> {

            ObservableList<String> selected =
                    listView.getSelectionModel().getSelectedItems();

            if(selected.isEmpty())
                return;

            try(Connection con = DatabaseConnection.getConnection())
            {
                String sql = "INSERT INTO student(posts,total_candidates) VALUES(?,0)";
                PreparedStatement ps = con.prepareStatement(sql);

                for(String post : selected)
                {
                    ps.setString(1,post);
                    ps.executeUpdate();
                }

            }catch(Exception ex){
                ex.printStackTrace();
            }

            stage.close();

            loadPosts(); // table refresh

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Posts added successfully");
            alert.show();

        });
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
