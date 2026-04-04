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

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.time.Period;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ResourceBundle;


public class addNationalVoterController  implements Initializable{
    @FXML public Button fileButton;
    @FXML public ListView listView;
    @FXML public Label fileError;
    @FXML private TextField nameField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField voterIdField;
    @FXML private ComboBox<String> constituencyBox;
    @FXML private Label duplicateLabel;
    @FXML public Label nameError;
    @FXML public Label dobError;
    @FXML public Label constError;
    List<File> selectedFiles;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> constituencies = FXCollections.observableArrayList();
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT constituency FROM national";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next())
            {
                constituencies.add(rs.getString("constituency"));
            }
            constituencyBox.setItems(constituencies);
            st.close();
            rs.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void chooseFile(ActionEvent e)
    {
        FileChooser fc = new FileChooser();

        FileChooser.ExtensionFilter txtFilter =
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");

        fc.getExtensionFilters().clear();      // default filter remove
        fc.getExtensionFilters().add(txtFilter);
        fc.setSelectedExtensionFilter(txtFilter);
        selectedFiles=fc.showOpenMultipleDialog(null);

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
        fileError.setText("");

        if(selectedFiles == null)
        {
            fileError.setText("No file selected");
            return;
        }
        int duplicateFound = 0;
        int invalidConstituencyFound = 0;
        int successAdded = 0;

        try
        {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO voter_national(name,birthday,voter_id,constituency) VALUES(?,?,?,?)";
            PreparedStatement insertPs = conn.prepareStatement(sql);

            PreparedStatement checkDuplicate =
                    conn.prepareStatement("SELECT voter_id FROM voter_national WHERE voter_id=?");

            PreparedStatement checkConst =
                    conn.prepareStatement("SELECT constituency FROM national WHERE constituency=?");
            PreparedStatement updateTotal =
                    conn.prepareStatement("UPDATE national SET total_voters = total_voters + 1 WHERE constituency=?");

            for(File file : selectedFiles)
            {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while((line = br.readLine()) != null)
                {
                    String[] data = line.split(",");

                    String name = data[0];
                    String birthday = data[1];
                    String voter_id = data[2];
                    String constituency = data[3];

                    // --------------------------
                    // CHECK CONSTITUENCY VALID
                    // --------------------------

                    checkConst.setString(1,constituency);
                    ResultSet rsConst = checkConst.executeQuery();

                    if(!rsConst.next())
                    {
                        invalidConstituencyFound++;
                        continue;
                    }

                    // --------------------------
                    // CHECK DUPLICATE VOTER ID
                    // --------------------------

                    checkDuplicate.setString(1,voter_id);
                    ResultSet rs = checkDuplicate.executeQuery();

                    if(rs.next())
                    {
                        duplicateFound++;
                        continue;
                    }

                    // --------------------------
                    // INSERT VOTER
                    // --------------------------

                    insertPs.setString(1,name);
                    insertPs.setString(2,birthday);
                    insertPs.setString(3,voter_id);
                    insertPs.setString(4,constituency);

                    insertPs.executeUpdate();
                    updateTotal.setString(1, constituency);
                    updateTotal.executeUpdate();
                    successAdded++;
                }

                br.close();
            }

            insertPs.close();
            checkDuplicate.close();
            checkConst.close();
            updateTotal.close();
            conn.close();

            // --------------------------
            // FINAL ALERTS
            // --------------------------

            if(invalidConstituencyFound>0)
            {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Invalid Constituency");
                alert.setContentText(invalidConstituencyFound+" voters belong to constituencies that are not in the election.\nThey were ignored.");
                alert.showAndWait();
            }

            if(duplicateFound>0)
            {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Duplicate Voter ID");
                alert.setContentText(duplicateFound+" duplicate voter IDs were found and ignored.");
                alert.showAndWait();
            }

            if(successAdded>0)
            {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setContentText(successAdded+ " valid voters from file added successfully.");
                alert.showAndWait();

                clearAllFields();
            }

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
    public void addVoter(ActionEvent b)
    {
        // Hide all error labels initially
        nameError.setVisible(false);
        dobError.setVisible(false);
        constError.setVisible(false);
        duplicateLabel.setVisible(false);

        String name = nameField.getText().trim();
        String voterId = voterIdField.getText().trim();
        String constituency = constituencyBox.getValue();
        boolean hasError = false;
        if(name.isEmpty())
        {
            nameError.setText("Name cannot be empty!");
            nameError.setVisible(true);
            hasError = true;
        }

        // -------------------------
        // DATE OF BIRTH VALIDATION
        // -------------------------
        if(dobPicker.getValue() == null)
        {
            dobError.setText("Date of birth must be selected!");
            dobError.setVisible(true);
            hasError = true;
        }
        else
        {
            // check age >= 18
            LocalDate dob = dobPicker.getValue();
            LocalDate today = LocalDate.now();
            int age = Period.between(dob, today).getYears();

            if(age < 18)
            {
                dobError.setText("Voter must be at least 18 years old!");
                dobError.setVisible(true);
                hasError = true;
            }
        }

        // -------------------------
        // VOTER ID VALIDATION
        // -------------------------
        if(voterId.isEmpty())
        {
            duplicateLabel.setText("Voter ID cannot be empty!");
            duplicateLabel.setVisible(true);
            hasError = true;
        }

        // -------------------------
        // CONSTITUENCY VALIDATION
        // -------------------------
        if(constituency == null)
        {
            constError.setText("A constituency must be selected!");
            constError.setVisible(true);
            hasError = true;
        }

        // If any validation failed → stop
        if(hasError) return;

        String birthday = dobPicker.getValue().toString();

        try
        {
            Connection conn = DatabaseConnection.getConnection();

            // -------------------------
            // Duplicate voter ID check
            // -------------------------
            String checkSql = "SELECT voter_id FROM voter_national WHERE voter_id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, voterId);
            ResultSet rs = checkPs.executeQuery();
            int ind=0;
            if(rs.next())
            {
                duplicateLabel.setText("A voter with this ID already exists!");
                duplicateLabel.setVisible(true);
                ind=1;
            }

            // -------------------------
            // Insert voter into DB
            // -------------------------
            String sql = "INSERT INTO voter_national(name,birthday,voter_id,constituency) VALUES(?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
           if(ind==0)
           {

               ps.setString(1,name);
               ps.setString(2,birthday);
               ps.setString(3,voterId);
               ps.setString(4,constituency);
               ps.executeUpdate();
               String updateSql = "UPDATE national SET total_voters = total_voters + 1 WHERE constituency = ?";
               PreparedStatement updatePs = conn.prepareStatement(updateSql);
               updatePs.setString(1, constituency);
               updatePs.executeUpdate();
               updatePs.close();
           }
            checkPs.close();
            ps.close();
            rs.close();
            conn.close();
            if(ind==0)
            {
                // Optional success dialog
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Voter added successfully!");
                alert.showAndWait();
            }

            // Clear fields
            if(ind==0)
            {
                clearAllFields();
            }

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
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
    public void clearAllFields()
    {
        nameField.clear();
        voterIdField.clear();
        dobPicker.setValue(null);
        constituencyBox.setValue(null);
        listView.getItems().clear();
        selectedFiles = null;
        nameError.setVisible(false);
        dobError.setVisible(false);
        constError.setVisible(false);
        fileError.setVisible(false);
    }
}
