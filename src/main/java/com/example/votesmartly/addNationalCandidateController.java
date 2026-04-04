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
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
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

public class addNationalCandidateController implements Initializable {
    @FXML public Button fileButton;
    @FXML public ListView listView;
    @FXML public Label fileError;
    @FXML public TextField nameField;
    @FXML public DatePicker dobPicker;
    @FXML public TextField voterIdField;
    @FXML public ComboBox<String> constituencyBox;
    @FXML public Label duplicateVoter;
    @FXML public TextField candidateIdField;
    @FXML public Label duplicateCandidate;
    @FXML public TextField symbol;
    @FXML public Label symbolError;
    @FXML public Label nameError;
    @FXML public Label dobError;
    @FXML public Label constError;

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
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<File> selectedFiles;

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

    public void addCandidatesFromFile(ActionEvent e)
    {
        fileError.setText("");

        if(selectedFiles == null)
        {
            fileError.setText("No file selected");
            fileError.setVisible(true);
            return;
        }

        int notVoterFound = 0;
        int infoMismatchFound = 0;
        int duplicateFound = 0;
        int successAdded = 0;

        try {

            Connection conn = DatabaseConnection.getConnection();

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
                    String cand_id = data[3];
                    String symbol = data[4];
                    String constituency = data[5];

                    // --------------------------------
                    // 1️⃣ CHECK IF VOTER EXISTS
                    // --------------------------------

                    PreparedStatement checkVoter = conn.prepareStatement(
                            "SELECT name,birthday,constituency FROM voter_national WHERE voter_id=?");

                    checkVoter.setString(1,voter_id);

                    ResultSet rs = checkVoter.executeQuery();

                    if(!rs.next())
                    {
                        notVoterFound++;
                        checkVoter.close();
                        rs.close();
                        continue;
                    }

                    // --------------------------------
                    // 2️⃣ MATCH VOTER INFORMATION
                    // --------------------------------

                    String dbName = rs.getString("name");
                    String dbDob = rs.getString("birthday");
                    String dbConst = rs.getString("constituency");

                    if(!dbName.equalsIgnoreCase(name)
                            || !dbDob.equals(birthday)
                            || !dbConst.equalsIgnoreCase(constituency))
                    {
                        infoMismatchFound ++;
                        checkVoter.close();
                        rs.close();
                        continue;
                    }

                    // --------------------------------
                    // 3️⃣ CHECK DUPLICATE CANDIDATE ID
                    // --------------------------------

                    PreparedStatement checkCandId = conn.prepareStatement(
                            "SELECT candidate_id FROM candidate_national WHERE candidate_id=?");

                    checkCandId.setString(1,cand_id);

                    ResultSet rs2 = checkCandId.executeQuery();

                    if(rs2.next())
                    {
                        duplicateFound++;
                        checkVoter.close();
                        rs.close();
                        checkCandId.close();
                        rs2.close();
                        continue;
                    }

                    // --------------------------------
                    // 4️⃣ CHECK DUPLICATE SYMBOL IN CONSTITUENCY
                    // --------------------------------

                    PreparedStatement checkSymbol = conn.prepareStatement(
                            "SELECT sign FROM candidate_national WHERE sign=? AND constituency=?");

                    checkSymbol.setString(1, symbol);
                    checkSymbol.setString(2, constituency); // Added constituency check

                    ResultSet rs3 = checkSymbol.executeQuery();

                    if(rs3.next())
                    {
                        checkVoter.close();
                        rs.close();
                        checkCandId.close();
                        rs2.close();
                        checkSymbol.close();
                        rs3.close();
                        duplicateFound++;
                        continue;
                    }

                    // --------------------------------
                    // 5️⃣ INSERT CANDIDATE
                    // --------------------------------

                    PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO candidate_national(name,birthday,voter_id,candidate_id,sign,constituency) VALUES(?,?,?,?,?,?)");

                    insert.setString(1,name);
                    insert.setString(2,birthday);
                    insert.setString(3,voter_id);
                    insert.setString(4,cand_id);
                    insert.setString(5,symbol);
                    insert.setString(6,constituency);

                    insert.executeUpdate();
                    checkVoter.close();
                    rs.close();
                    checkCandId.close();
                    rs2.close();
                    checkSymbol.close();
                    rs3.close();
                    insert.close();
                    successAdded++;

                    PreparedStatement updateTotal = conn.prepareStatement(
                            "UPDATE national SET total_candidates = total_candidates + 1 WHERE constituency=?");

                    updateTotal.setString(1,constituency);
                    updateTotal.executeUpdate();
                    updateTotal.close();
                }

                br.close();
            }

            conn.close();

            // --------------------------------
            // FINAL ALERTS
            // --------------------------------

            if(notVoterFound>0)
            {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Invalid Voter");
                alert.setContentText(notVoterFound+" IDs in the file are not voters.\nThey were ignored.");
                alert.showAndWait();
            }

            if(infoMismatchFound>0)
            {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Information Mismatch");
                alert.setContentText(infoMismatchFound+" candidate information does not match voter database.\nThey were ignored.");
                alert.showAndWait();
            }

            if(duplicateFound>0)
            {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Duplicate Found");
                alert.setContentText(duplicateFound+" duplicate candidate IDs or symbols found.\nThey were ignored.");
                alert.showAndWait();
            }

            if(successAdded>0)
            {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setContentText(successAdded+" valid candidates from file added successfully.");
                alert.showAndWait();
                nameField.clear();
                voterIdField.clear();
                candidateIdField.clear();
                symbol.clear();
                dobPicker.setValue(null);
                constituencyBox.setValue(null);
                listView.getItems().clear();
                nameError.setVisible(false);
                dobError.setVisible(false);
                constError.setVisible(false);
                symbolError.setVisible(false);
                duplicateVoter.setVisible(false);
                duplicateCandidate.setVisible(false);
                fileError.setVisible(false);
            }

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void addCandidate() {

        nameError.setVisible(false);
        dobError.setVisible(false);
        constError.setVisible(false);
        duplicateVoter.setVisible(false);
        duplicateCandidate.setVisible(false);
        symbolError.setVisible(false);

        String name = nameField.getText().trim();
        String voterId = voterIdField.getText().trim();
        String candidateId = candidateIdField.getText().trim();
        String symbolStr = symbol.getText().trim();
        String constituency = constituencyBox.getValue();
        LocalDate dob = dobPicker.getValue();

        boolean hasError = false;

        // -------- Empty validation --------

        if(name.isEmpty()){
            nameError.setText("Name cannot be empty!");
            nameError.setVisible(true);
            hasError = true;
        }

        if(dob == null){
            dobError.setText("Date of Birth must be selected!");
            dobError.setVisible(true);
            hasError = true;
        }

        if(voterId.isEmpty()){
            duplicateVoter.setText("Voter ID cannot be empty!");
            duplicateVoter.setVisible(true);
            hasError = true;
        }

        if(candidateId.isEmpty()){
            duplicateCandidate.setText("Candidate ID cannot be empty!");
            duplicateCandidate.setVisible(true);
            hasError = true;
        }

        if(symbolStr.isEmpty()){
            symbolError.setText("Symbol cannot be empty!");
            symbolError.setVisible(true);
            hasError = true;
        }

        if(constituency == null){
            constError.setText("Constituency must be selected!");
            constError.setVisible(true);
            hasError = true;
        }

        if(hasError) return;

        try{

            Connection conn = DatabaseConnection.getConnection();

            // ------------------------------
            // CHECK IF VOTER EXISTS
            // ------------------------------

            PreparedStatement checkVoter = conn.prepareStatement(
                    "SELECT name,birthday,constituency FROM voter_national WHERE voter_id=?");

            checkVoter.setString(1,voterId);

            ResultSet rs = checkVoter.executeQuery();

            if(!rs.next()){
                duplicateVoter.setText("This voter ID does not exist!");
                duplicateVoter.setVisible(true);
                return;
            }

            // ------------------------------
            // MATCH NAME DOB CONSTITUENCY
            // ------------------------------

            String dbName = rs.getString("name");
            String dbDob = rs.getString("birthday");
            String dbConst = rs.getString("constituency");

            boolean mismatch = false;

            if (!dbName.equalsIgnoreCase(name)) {
                nameError.setText("Name does not match voter record!");
                nameError.setVisible(true);
                mismatch = true;
            }

            if (!dbDob.equals(dob.toString())) {
                dobError.setText("DOB does not match voter record!");
                dobError.setVisible(true);
                mismatch = true;
            }

            if (!dbConst.equalsIgnoreCase(constituency)) {
                constError.setText("Constituency does not match voter record!");
                constError.setVisible(true);
                mismatch = true;
            }

            if (mismatch) {
                rs.close(); checkVoter.close(); conn.close();
                return;
            }

            // ------------------------------
            // CHECK ALREADY CANDIDATE
            // ------------------------------

            PreparedStatement checkCandidate = conn.prepareStatement(
                    "SELECT voter_id FROM candidate_national WHERE voter_id=?");

            checkCandidate.setString(1,voterId);

            ResultSet rs2 = checkCandidate.executeQuery();

            if(rs2.next()){
                duplicateVoter.setText("This voter is already a candidate!");
                duplicateVoter.setVisible(true);
                checkVoter.close();
                checkCandidate.close();
                rs2.close();
                conn.close();
                return;
            }

            // ------------------------------
            // CHECK CANDIDATE ID UNIQUE
            // ------------------------------

            PreparedStatement checkCandId = conn.prepareStatement(
                    "SELECT candidate_id FROM candidate_national WHERE candidate_id=?");

            checkCandId.setString(1,candidateId);

            ResultSet rs3 = checkCandId.executeQuery();

            if(rs3.next()){
                duplicateCandidate.setText("Candidate ID already exists!");
                duplicateCandidate.setVisible(true);
                checkVoter.close();
                checkCandidate.close();
                rs2.close();
                checkCandId.close();
                rs3.close();
                conn.close();
                return;
            }

            // ------------------------------
            // CHECK SYMBOL UNIQUE IN CONSTITUENCY
            // ------------------------------

            PreparedStatement checkSymbol = conn.prepareStatement(
                    "SELECT sign FROM candidate_national WHERE sign=? AND constituency=?");

            checkSymbol.setString(1, symbolStr);
            checkSymbol.setString(2, constituency); // Added constituency check

            ResultSet rs4 = checkSymbol.executeQuery();

            if(rs4.next()){
                symbolError.setText("This symbol is already used in this constituency!");
                symbolError.setVisible(true);
                checkVoter.close();
                checkCandidate.close();
                rs2.close();
                checkCandId.close();
                rs3.close();
                checkSymbol.close();
                rs4.close();
                conn.close();
                return;
            }

            // ------------------------------
            // INSERT CANDIDATE
            // ------------------------------

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO candidate_national(name,birthday,voter_id,candidate_id,sign,constituency) VALUES(?,?,?,?,?,?)");

            ps.setString(1,name);
            ps.setString(2,dob.toString());
            ps.setString(3,voterId);
            ps.setString(4,candidateId);
            ps.setString(5,symbolStr);
            ps.setString(6,constituency);

            ps.executeUpdate();
            ps.close();
            checkVoter.close();
            checkCandidate.close();
            rs2.close();
            checkCandId.close();
            rs3.close();
            checkSymbol.close();
            rs4.close();

            String updateSql = "UPDATE national SET total_candidates = total_candidates + 1 WHERE constituency = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setString(1, constituency);
            updatePs.executeUpdate();
            updatePs.close();
            conn.close();

            // ------------------------------
            // SUCCESS ALERT
            // ------------------------------

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Candidate added successfully!");
            alert.showAndWait();

            nameField.clear();
            voterIdField.clear();
            candidateIdField.clear();
            symbol.clear();
            dobPicker.setValue(null);
            constituencyBox.setValue(null);
            listView.getItems().clear();
            nameError.setVisible(false);
            dobError.setVisible(false);
            constError.setVisible(false);
            symbolError.setVisible(false);
            duplicateVoter.setVisible(false);
            duplicateCandidate.setVisible(false);
            fileError.setVisible(false);

        }
        catch(Exception e){
            e.printStackTrace();
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