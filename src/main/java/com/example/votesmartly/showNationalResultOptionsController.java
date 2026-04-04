package com.example.votesmartly;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class showNationalResultOptionsController {

    @FXML public Button showResult;

    public void onShow() {
        try {
            String electionType = "";

            // -------------------- 1. Get last election type --------------------
            try (java.sql.Connection conn = DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                         "SELECT name FROM election_info ORDER BY id_db DESC LIMIT 1");
                 java.sql.ResultSet rs = ps.executeQuery())
            {
                if(rs.next()) {
                    electionType = rs.getString("name");
                }
            }

            // -------------------- 2. Decide which FXML --------------------
            String fxmlFile;

            if(electionType.equalsIgnoreCase("National Election")) {
                fxmlFile = "nationalResultOverall.fxml";
            } else {
                fxmlFile = "stdOverallResult.fxml";
            }

            // -------------------- 3. Load Scene --------------------
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = (Stage) showResult.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}