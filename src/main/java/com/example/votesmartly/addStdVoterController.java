
package com.example.votesmartly;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class addStdVoterController implements Initializable {

    @FXML public Button fileButton;
    @FXML public ListView listView;
    @FXML public Label fileError;
    @FXML public Label nameError;
    @FXML public Label stdIdError;
    @FXML public Label deptError;
    @FXML public ComboBox<String> deptBox;
    @FXML public TextField nameField;
    @FXML public TextField stdId;
    @FXML private ProgressBar progressBar;
    List<File> selectedFiles;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        ObservableList<String> departments = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT dept FROM dept_for_std");
             ResultSet rs = pst.executeQuery())
        {
            while(rs.next()) {
                departments.add(rs.getString("dept"));
            }
            deptBox.setItems(departments);
        }
        catch (Exception e) {
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
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        selectedFiles = fc.showOpenMultipleDialog(null);

        if(selectedFiles != null) {
            for(File f : selectedFiles) {
                listView.getItems().add(f.getAbsolutePath());
            }
        } else {
            System.out.println("File is not Valid");
        }
    }


    public void addVotersFromFile(ActionEvent e)
    {
        fileError.setVisible(false);

        if(selectedFiles == null) {
            fileError.setText("Invalid Files!");
            fileError.setVisible(true);
            return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {

            int duplicateFound   = 0;
            int invalidDeptFound = 0;
            int added = 0;

            @Override
            protected Void call() throws Exception {

                Connection conn = DatabaseConnection.getConnection();

                PreparedStatement psInsert = conn.prepareStatement(
                        "INSERT OR IGNORE INTO voter_std(name, std_id, dept) VALUES(?,?,?)"
                );
                PreparedStatement psDeptCheck = conn.prepareStatement(
                        "SELECT dept FROM dept_for_std WHERE dept = ?"
                );

                int totalLines = 0;
                for(File file : selectedFiles) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    while(br.readLine() != null) totalLines++;
                    br.close();
                }

                int processed = 0;

                for(File file : selectedFiles) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while((line = br.readLine()) != null) {

                        String[] data = line.split(",");
                        if(data.length < 3) {
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        String name = data[0].trim();
                        String sId  = data[1].trim();
                        String dept = data[2].trim();

                        psDeptCheck.setString(1, dept);
                        ResultSet rs = psDeptCheck.executeQuery();

                        if(!rs.next()) {
                            invalidDeptFound++;
                            rs.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }
                        rs.close();

                        psInsert.setString(1, name);
                        psInsert.setString(2, sId);
                        psInsert.setString(3, dept);

                        int rows = psInsert.executeUpdate();

                        if(rows == 0) {
                            duplicateFound++;
                        } else {
                            added++;
                            PreparedStatement updateStudent = conn.prepareStatement(
                                    "UPDATE student SET total_voters = total_voters + 1"
                            );
                            updateStudent.executeUpdate();
                            updateStudent.close();
                        }

                        processed++;
                        updateProgress(processed, totalLines);
                    }

                    br.close();
                }

                psInsert.close();
                psDeptCheck.close();
                conn.close();

                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);

                if(duplicateFound > 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Duplicate Student ID");
                    alert.setHeaderText(null);
                    alert.setContentText(duplicateFound + " student IDs already existed and were ignored.");
                    alert.showAndWait();
                }

                if(invalidDeptFound > 0) {
                    Alert warn = new Alert(Alert.AlertType.WARNING);
                    warn.setTitle("Invalid Department Found");
                    warn.setHeaderText(null);
                    warn.setContentText(invalidDeptFound + " students were ignored because their department does not exist.");
                    warn.showAndWait();
                }

                if(added > 0) {
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
                resetComboBox(deptBox);
                listView.getItems().clear();
                selectedFiles = null;
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


    public void addVoter(ActionEvent ex)
    {
        nameError.setVisible(false);
        stdIdError.setVisible(false);
        deptError.setVisible(false);

        String name = nameField.getText().trim();
        String Id   = stdId.getText().trim();
        String dept = deptBox.getValue();

        boolean hasError = false;

        if(name.isEmpty()) {
            nameError.setText("Name cannot be empty!");
            nameError.setVisible(true);
            hasError = true;
        }

        if(Id.isEmpty()) {
            stdIdError.setText("Student ID cannot be empty!");
            stdIdError.setVisible(true);
            hasError = true;
        }

        if(dept == null) {
            deptError.setText("Department must be selected!");
            deptError.setVisible(true);
            hasError = true;
        }

        if(hasError) return;

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement check = conn.prepareStatement(
                    "SELECT std_id FROM voter_std WHERE std_id = ?"
            );
            check.setString(1, Id);
            ResultSet rs = check.executeQuery();

            if(rs.next()) {
                stdIdError.setText("A student with this ID already exists!");
                stdIdError.setVisible(true);
                rs.close();
                conn.close();
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO voter_std(name, std_id, dept) VALUES(?,?,?)"
            );
            ps.setString(1, name);
            ps.setString(2, Id);
            ps.setString(3, dept);
            ps.executeUpdate();

            PreparedStatement updateStudent = conn.prepareStatement(
                    "UPDATE student SET total_voters = total_voters + 1"
            );
            updateStudent.executeUpdate();

            updateStudent.close();
            ps.close();
            rs.close();
            conn.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Student voter added successfully!");
            alert.showAndWait();

            // ✅ Clear form and restore prompt text
            nameField.clear();
            stdId.clear();
            resetComboBox(deptBox); // ✅ prompt text restored
            listView.getItems().clear();

        } catch(Exception ex2) {
            ex2.printStackTrace();
        }
    }

    public void onBack(ActionEvent x)
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("addOrRemove.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) x.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

