
package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

public class addStdCandidateController implements Initializable {

    @FXML public Button fileButton;
    @FXML public ListView listView;
    @FXML public TextField nameField;
    @FXML public TextField stdIdField;
    @FXML public ComboBox<String> deptBox;
    @FXML public ComboBox<String> postBox;
    @FXML public TextField candIdField;
    @FXML public TextField symbolField;
    @FXML public Label nameError;
    @FXML public Label stdIdError;
    @FXML public Label deptError;
    @FXML public Label fileError;
    @FXML public Label candIdError;
    @FXML public Label symbolError;
    @FXML public Label postError;
    @FXML private ProgressBar progressBar;

    List<File> selectedFiles;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        ObservableList<String> departments = FXCollections.observableArrayList();

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT dept FROM dept_for_std");
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                departments.add(rs.getString("dept"));
            }

            deptBox.setItems(departments);
            rs.close();
            ps.close();
            conn.close();

        } catch(Exception e){
            e.printStackTrace();
        }

        ObservableList<String> posts = FXCollections.observableArrayList();

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT posts FROM student");
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                posts.add(rs.getString("posts"));
            }

            postBox.setItems(posts);
            rs.close();
            ps.close();
            conn.close();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void resetComboBox(ComboBox<String> box) {
        box.getSelectionModel().clearSelection();
        box.setValue(null);
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(box.getPromptText());
                    setStyle("-fx-text-fill: derive(-fx-control-inner-background, -30%);");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }

    public void chooseFile()
    {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files","*.txt"));
        selectedFiles = fc.showOpenMultipleDialog(null);

        if(selectedFiles != null){
            for(File f : selectedFiles){
                listView.getItems().add(f.getAbsolutePath());
            }
        }
    }

    public void addCandidate()
    {
        nameError.setVisible(false);
        stdIdError.setVisible(false);
        deptError.setVisible(false);
        postError.setVisible(false);
        candIdError.setVisible(false);
        symbolError.setVisible(false);

        String name   = nameField.getText().trim();
        String stdId  = stdIdField.getText().trim();
        String dept   = deptBox.getValue();
        String post   = postBox.getValue();
        String candId = candIdField.getText().trim();
        String symbol = symbolField.getText().trim();

        boolean hasError = false;

        if(name.isEmpty())  { nameError.setText("Name cannot be empty!");         nameError.setVisible(true);   hasError = true; }
        if(stdId.isEmpty()) { stdIdError.setText("Student ID cannot be empty!");  stdIdError.setVisible(true);  hasError = true; }
        if(dept == null)    { deptError.setText("Select a department!");           deptError.setVisible(true);   hasError = true; }
        if(post == null)    { postError.setText("Select post!");                   postError.setVisible(true);   hasError = true; }
        if(candId.isEmpty()){ candIdError.setText("Candidate ID required!");      candIdError.setVisible(true); hasError = true; }
        if(symbol.isEmpty()){ symbolError.setText("Symbol required!");             symbolError.setVisible(true); hasError = true; }

        if(hasError) return;

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkStudent = conn.prepareStatement(
                    "SELECT name, dept FROM voter_std WHERE std_id=?"
            );
            checkStudent.setString(1, stdId);
            ResultSet rs = checkStudent.executeQuery();

            if(!rs.next()){
                stdIdError.setText("This student ID does not exist in student database!");
                stdIdError.setVisible(true);
                rs.close(); checkStudent.close(); conn.close();
                return;
            }

            String realName = rs.getString("name");
            String realDept = rs.getString("dept");

            if(!realName.equalsIgnoreCase(name)){
                nameError.setText("Name does not match student database!");
                nameError.setVisible(true);
                rs.close(); checkStudent.close(); conn.close();
                return;
            }

            if(!realDept.equalsIgnoreCase(dept)){
                deptError.setText("Department does not match student database!");
                deptError.setVisible(true);
                rs.close(); checkStudent.close(); conn.close();
                return;
            }

            PreparedStatement checkCID = conn.prepareStatement(
                    "SELECT candidate_id FROM candidate_std WHERE candidate_id=?"
            );
            checkCID.setString(1, candId);
            if(checkCID.executeQuery().next()){
                candIdError.setText("Candidate ID already exists!");
                candIdError.setVisible(true);
                rs.close(); checkStudent.close(); checkCID.close(); conn.close();
                return;
            }

            PreparedStatement checkSYM = conn.prepareStatement(
                    "SELECT sign FROM candidate_std WHERE sign=? AND post_for_vote=?"
            );
            checkSYM.setString(1, symbol);
            checkSYM.setString(2, post);
            if(checkSYM.executeQuery().next()){
                symbolError.setText("Symbol already used for this post!");
                symbolError.setVisible(true);
                rs.close(); checkStudent.close(); checkCID.close(); checkSYM.close(); conn.close();
                return;
            }

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO candidate_std(name,std_id,candidate_id,post_for_vote,sign,dept) VALUES(?,?,?,?,?,?)"
            );
            insert.setString(1, name);
            insert.setString(2, stdId);
            insert.setString(3, candId);
            insert.setString(4, post);
            insert.setString(5, symbol);
            insert.setString(6, dept);
            insert.executeUpdate();

            PreparedStatement updatePost = conn.prepareStatement(
                    "UPDATE student SET total_candidates = total_candidates + 1 WHERE posts=?"
            );
            updatePost.setString(1, post);
            updatePost.executeUpdate();

            rs.close(); checkStudent.close(); checkCID.close();
            checkSYM.close(); insert.close(); updatePost.close(); conn.close();

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Candidate added successfully!");
            ok.showAndWait();

            nameField.clear();
            stdIdField.clear();
            candIdField.clear();
            symbolField.clear();
            resetComboBox(deptBox);
            resetComboBox(postBox);
            listView.getItems().clear();
            fileError.setVisible(false);

        } catch(Exception ex) { ex.printStackTrace(); }
    }

    public void addCandidatesFromFile()
    {
        fileError.setVisible(false);

        if(listView.getItems().isEmpty()){
            fileError.setText("Invalid Files!");
            fileError.setVisible(true);
            return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {

            int invalidStudent = 0;
            int mismatchInfo   = 0;
            int duplicateCID   = 0;
            int duplicateSYM   = 0;
            int invalidPost    = 0;
            int success        = 0;

            @Override
            protected Void call() throws Exception {

                Connection conn = DatabaseConnection.getConnection();

                PreparedStatement findStudent = conn.prepareStatement(
                        "SELECT name, dept FROM voter_std WHERE std_id=?"
                );
                PreparedStatement checkCID = conn.prepareStatement(
                        "SELECT candidate_id FROM candidate_std WHERE candidate_id=?"
                );
                PreparedStatement checkSYM = conn.prepareStatement(
                        "SELECT sign FROM candidate_std WHERE sign=? AND post_for_vote=?"
                );
                PreparedStatement checkPost = conn.prepareStatement(
                        "SELECT DISTINCT posts FROM student WHERE posts=?"
                );
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO candidate_std(name, std_id, candidate_id, post_for_vote, sign, dept) VALUES(?,?,?,?,?,?)"
                );
                PreparedStatement updatePost = conn.prepareStatement(
                        "UPDATE student SET total_candidates = total_candidates + 1 WHERE posts=?"
                );

                int totalLines = 0;
                for(Object obj : listView.getItems()) {
                    BufferedReader br = new BufferedReader(new FileReader(obj.toString()));
                    while(br.readLine() != null) totalLines++;
                    br.close();
                }

                int processed = 0;

                for(Object obj : listView.getItems())
                {
                    BufferedReader br = new BufferedReader(new FileReader(obj.toString()));
                    String line;

                    while((line = br.readLine()) != null)
                    {
                        String[] d = line.split(",");

                        if(d.length < 6){
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        String name   = d[0].trim();
                        String stdId  = d[1].trim();
                        String candId = d[2].trim();
                        String post   = d[3].trim();
                        String symbol = d[4].trim();
                        String dept   = d[5].trim();

                        findStudent.setString(1, stdId);
                        ResultSet rs = findStudent.executeQuery();

                        if(!rs.next()){
                            invalidStudent++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        if(!rs.getString("name").equalsIgnoreCase(name) ||
                                !rs.getString("dept").equalsIgnoreCase(dept)){
                            mismatchInfo++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        checkCID.setString(1, candId);
                        if(checkCID.executeQuery().next()){
                            duplicateCID++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        checkSYM.setString(1, symbol);
                        checkSYM.setString(2, post);
                        if(checkSYM.executeQuery().next()){
                            duplicateSYM++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        checkPost.setString(1, post);
                        if(!checkPost.executeQuery().next()){
                            invalidPost++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        rs.close();

                        insert.setString(1, name);
                        insert.setString(2, stdId);
                        insert.setString(3, candId);
                        insert.setString(4, post);
                        insert.setString(5, symbol);
                        insert.setString(6, dept);
                        insert.executeUpdate();

                        updatePost.setString(1, post);
                        updatePost.executeUpdate();

                        success++;
                        processed++;
                        updateProgress(processed, totalLines);
                    }

                    br.close();
                }

                checkCID.close(); checkSYM.close(); checkPost.close();
                findStudent.close(); insert.close(); updatePost.close(); conn.close();

                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);

                if(invalidStudent > 0)
                    new Alert(Alert.AlertType.WARNING, invalidStudent + " IDs ignored (Not students)").showAndWait();

                if(mismatchInfo > 0)
                    new Alert(Alert.AlertType.WARNING, mismatchInfo + " students ignored (Name/Dept mismatch)").showAndWait();

                if(duplicateCID > 0)
                    new Alert(Alert.AlertType.WARNING, duplicateCID + " candidate IDs ignored (Duplicate)").showAndWait();

                if(duplicateSYM > 0)
                    new Alert(Alert.AlertType.WARNING, duplicateSYM + " symbols ignored (Duplicate for same post)").showAndWait();

                if(invalidPost > 0)
                    new Alert(Alert.AlertType.WARNING, invalidPost + " posts ignored (Invalid post)").showAndWait();

                if(success > 0) {
                    new Alert(Alert.AlertType.INFORMATION, success + " Candidates added successfully!").showAndWait();
                    listView.getItems().clear();
                }
            }

            @Override
            protected void failed() {
                progressBar.setVisible(false);
                getException().printStackTrace();
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setVisible(true);

        new Thread(task).start();
    }

    public void onBack(ActionEvent x)
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("addOrRemove.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) x.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) { e.printStackTrace(); }
    }
}
