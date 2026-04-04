package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;
public class addStdVoterController implements Initializable{
    @FXML
    public Button fileButton;
    @FXML public ListView listView;
    @FXML public Label fileError;
    @FXML public Label nameError;
    @FXML public Label stdIdError;
    @FXML public  Label deptError;
    @FXML public ComboBox<String> deptBox;
    @FXML public TextField nameField;
    @FXML public TextField stdId;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        ObservableList<String> departments = FXCollections.observableArrayList();

        String sql = "SELECT dept FROM dept_for_std";

        try (Connection conn = DatabaseConnection.getConnection()  ;
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery())
        {

            while(rs.next())
            {
                departments.add(rs.getString("dept"));
            }

            deptBox.setItems(departments);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    List<File> selectedFiles;
    public  void chooseFile()
    {
        FileChooser fc=new FileChooser();
        selectedFiles=fc.showOpenMultipleDialog(null);
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("text Files","*.txt"));
        if(selectedFiles!=null){
            for(File f : selectedFiles)
            {
                listView.getItems().add(f.getAbsolutePath());
            }
        }
        else {
            System.out.println("File is not Valid");
        }
    }
    public void addVotersFromFile(ActionEvent e)
    {
        fileError.setVisible(false);

        if(selectedFiles == null){
            fileError.setText("Invalid Files!");
            fileError.setVisible(true);
            return;
        }

        int duplicateFound = 0;
        int invalidDeptFound = 0;

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sqlInsert = "INSERT OR IGNORE INTO voter_std(name, std_id, dept) VALUES(?,?,?)";
            PreparedStatement psInsert = conn.prepareStatement(sqlInsert);

            String sqlDeptCheck = "SELECT dept FROM dept_for_std WHERE dept = ?";
            PreparedStatement psDeptCheck = conn.prepareStatement(sqlDeptCheck);
            int added=0;
            for(File file : selectedFiles){
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while((line = br.readLine()) != null){
                    String[] data = line.split(",");

                    if (data.length < 3) continue;  // skip malformed lines

                    String name = data[0].trim();
                    String stdId = data[1].trim();
                    String dept = data[2].trim();

                    // -----------------------------
                    // DEPARTMENT VALIDATION
                    // -----------------------------
                    psDeptCheck.setString(1, dept);
                    ResultSet rs = psDeptCheck.executeQuery();

                    if(!rs.next()) {
                        invalidDeptFound++;
                        rs.close();
                        // dept not found → skip
                        continue;
                    }
                    rs.close();
                    // -----------------------------
                    // INSERT INTO DATABASE
                    // -----------------------------
                    psInsert.setString(1, name);
                    psInsert.setString(2, stdId);
                    psInsert.setString(3, dept);
                    int rows = psInsert.executeUpdate();
                    if(rows == 0){
                        duplicateFound++;
                    }
                    else {
                            added++;

                            // 🔥 STUDENT TABLE UPDATE (only when actually inserted)
                            PreparedStatement updateStudent =
                                    conn.prepareStatement("UPDATE student SET total_voters = total_voters + 1");

                            updateStudent.executeUpdate();
                            updateStudent.close();
                    }
                }

                br.close();
            }

            // Duplicate warning
            if(duplicateFound>0){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Duplicate Student ID");
                alert.setHeaderText(null);
                alert.setContentText(duplicateFound +" student IDs already existed and were ignored.");
                alert.showAndWait();
            }

            // Invalid department warning
            if(invalidDeptFound > 0){
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Invalid Department Found");
                warn.setHeaderText(null);
                warn.setContentText(invalidDeptFound +
                        " students were ignored because their department does not exist.");
                warn.showAndWait();
            }

            // Success message
            if(added>0)
            {
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Success");
                done.setHeaderText(null);
                done.setContentText("Student voters added successfully!");
                done.showAndWait();
            }
            nameError.setVisible(false);
            stdIdError.setVisible(false);
            deptError.setVisible(false);
            nameField.clear();
            stdId.clear();
            deptBox.setValue(null);
            listView.getItems().clear();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    public void addVoter(ActionEvent ex)
    {
        // Hide all previous errors
        nameError.setVisible(false);
        stdIdError.setVisible(false);
        deptError.setVisible(false);

        // Read values
        String name = nameField.getText().trim();
        String Id = stdId.getText().trim();
        String dept = deptBox.getValue();

        boolean hasError = false;

        // -------------------------
        // NAME VALIDATION
        // -------------------------
        if(name.isEmpty()){
            nameError.setText("Name cannot be empty!");
            nameError.setVisible(true);
            hasError = true;
        }

        // -------------------------
        // STUDENT ID VALIDATION
        // -------------------------
        if(Id.isEmpty()){
            stdIdError.setText("Student ID cannot be empty!");
            stdIdError.setVisible(true);
            hasError = true;
        }

        // -------------------------
        // DEPARTMENT VALIDATION
        // -------------------------
        if(dept == null){
            deptError.setText("Department must be selected!");
            deptError.setVisible(true);
            hasError = true;
        }

        if(hasError) return;

        // -------------------------
        // DATABASE VALIDATION
        // -------------------------
        try {
            Connection conn = DatabaseConnection.getConnection();

            // Check duplicate student ID
            PreparedStatement check = conn.prepareStatement(
                    "SELECT std_id FROM voter_std WHERE std_id = ?"
            );
            check.setString(1, Id);
            ResultSet rs = check.executeQuery();

            if(rs.next()){
                stdIdError.setText("A student with this ID already exists!");
                stdIdError.setVisible(true);
                rs.close();
                conn.close();
                return;
            }

            // -------------------------
            // INSERT INTO DATABASE
            // -------------------------
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO voter_std(name, std_id, dept) VALUES(?,?,?)"
            );

            ps.setString(1, name);
            ps.setString(2, Id);
            ps.setString(3, dept);

            ps.executeUpdate();
            PreparedStatement updateStudent =
                    conn.prepareStatement("UPDATE student SET total_voters = total_voters + 1");

            updateStudent.executeUpdate();
            updateStudent.close();
            ps.close();
            rs.close();
            conn.close();

            // Success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Student voter added successfully!");
            alert.showAndWait();

            // Clear form
            nameField.clear();
            stdId.clear();
            deptBox.setValue(null);
            listView.getItems().clear();
        }
        catch(Exception ex2){
            ex2.printStackTrace();
        }
    }
    public void onBack(ActionEvent x)
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("addOrRemove.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) x.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
