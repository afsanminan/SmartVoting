package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class addOrRemoveController {

    @FXML public Button addVoterButton;
    @FXML public Button addCandButton;

    // -----------------------------
    // Add Voter Button
    // -----------------------------
    public void addVoter(ActionEvent e)
    {
        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if(rs.next())
            {
                String electionType = rs.getString("name");

                Stage stage = (Stage) addVoterButton.getScene().getWindow();
                Parent root;

                if(electionType.equalsIgnoreCase("National Election"))
                {
                    root = FXMLLoader.load(getClass().getResource("addNationalVoter.fxml"));
                }
                else
                {
                    root = FXMLLoader.load(getClass().getResource("addStdVoter.fxml"));
                }

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            st.close();
            rs.close();
            conn.close();

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // -----------------------------
    // Add Candidate Button
    // -----------------------------
    public void addCandidate(ActionEvent ex)
    {
        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if(rs.next())
            {
                String electionType = rs.getString("name");

                Stage stage = (Stage) addCandButton.getScene().getWindow();
                Parent root;

                if(electionType.equalsIgnoreCase("National Election"))
                {
                    root = FXMLLoader.load(getClass().getResource("addNationalCandidate.fxml"));
                }
                else
                {
                    root = FXMLLoader.load(getClass().getResource("addStdCandidate.fxml"));
                }

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            st.close();
            rs.close();
            conn.close();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    // -----------------------------
// Show Candidate
// -----------------------------
    public void showCandidate()
    {
        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if(rs.next())
            {
                String electionType = rs.getString("name");

                Stage stage = (Stage) addCandButton.getScene().getWindow();
                Parent root;

                if(electionType.equalsIgnoreCase("National Election"))
                {
                    root = FXMLLoader.load(getClass().getResource("showNationalCandidate.fxml"));
                }
                else
                {
                    root = FXMLLoader.load(getClass().getResource("showStudentCandidates.fxml"));
                }

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            st.close();
            rs.close();
            conn.close();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    // -----------------------------
// Show Voter
// -----------------------------
    public void showVoter()
    {
        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if(rs.next())
            {
                String electionType = rs.getString("name");

                Stage stage = (Stage) addVoterButton.getScene().getWindow();
                Parent root;

                if(electionType.equalsIgnoreCase("National Election"))
                {
                    root = FXMLLoader.load(getClass().getResource("showNationalVoters.fxml"));
                }
                else
                {
                    root = FXMLLoader.load(getClass().getResource("showStudentVoters.fxml"));
                }

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            st.close();
            rs.close();
            conn.close();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void showElectionInfo()
    {
        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if(rs.next())
            {
                String electionType = rs.getString("name");

                Stage stage = (Stage) addVoterButton.getScene().getWindow();
                Parent root;

                if(electionType.equalsIgnoreCase("National Election"))
                {
                    root = FXMLLoader.load(getClass().getResource("nationalInfo.fxml"));
                }
                else
                {
                    root = FXMLLoader.load(getClass().getResource("stdInfo.fxml"));
                }

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            st.close();
            rs.close();
            conn.close();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void onStart(ActionEvent e)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText("After starting the election, no modification of any data can be done! You can not add or remove any voter or candidate.You will not be able to see them until the election ends! Are you sure you want to start election?");

        // Wait for user response
        if(alert.showAndWait().get() == ButtonType.OK)
        {
            try {
                Connection conn = DatabaseConnection.getConnection();

                String sql = "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1";

                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql);

                if(rs.next())
                {
                    String electionType = rs.getString("name");

                    Stage stage = (Stage) addVoterButton.getScene().getWindow();
                    Parent root;

                    if(electionType.equalsIgnoreCase("National Election"))
                    {
                        root = FXMLLoader.load(getClass().getResource("nationalElectionRunning.fxml"));
                    }
                    else
                    {
                        root = FXMLLoader.load(getClass().getResource("studentElectionRunning.fxml"));
                    }

                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.show();
                }

                st.close();
                rs.close();
                conn.close();

            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        // যদি Cancel দেয় → কিছুই হবে না (alert auto close)
    }
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
        // Cancel দিলে কিছুই হবে না
    }
}