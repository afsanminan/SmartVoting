
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

import javafx.scene.control.Alert.AlertType;

import java.time.LocalDate;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;


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
    @FXML private ProgressBar progressBar;

    List<File> selectedFiles;

    private static final String CONSTITUENCY_PROMPT = "Select constituency";
    private static final String DATE_PROMPT         = "Date of Birth";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //const loading in order
        ObservableList<String> constituencies = FXCollections.observableArrayList();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT constituency FROM national ORDER BY constituency");
            while (rs.next()) constituencies.add(rs.getString("constituency"));
            constituencyBox.setItems(constituencies);
            rs.close(); st.close(); conn.close();
        } catch (Exception e) { e.printStackTrace(); }
        applyConstituencyPromptFix();
        applyDatePickerFixes();
    }

    public void applyConstituencyPromptFix() {
        constituencyBox.setPromptText(CONSTITUENCY_PROMPT);
        constituencyBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(constituencyBox.getPromptText());
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
    }

    public void applyDatePickerFixes() {
        dobPicker.setPromptText(DATE_PROMPT);
        dobPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                javafx.application.Platform.runLater(() -> {
                    dobPicker.getEditor().clear();
                    dobPicker.getEditor().setPromptText(DATE_PROMPT);
                });
            }
        });

        dobPicker.setOnShown(event -> injectYearScrollField(dobPicker));
        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(date.isAfter(LocalDate.now()));
            }
        });
    }

    private void injectYearScrollField(DatePicker picker) {
        javafx.scene.control.skin.DatePickerSkin skin =
                (javafx.scene.control.skin.DatePickerSkin) picker.getSkin();
        javafx.scene.layout.Region content =
                (javafx.scene.layout.Region) skin.getPopupContent();
        if (content.lookup("#yearScrollBar") != null) return;

        int initialYear = (picker.getValue() != null)
                ? picker.getValue().getYear()
                : LocalDate.now().getYear();

        TextField yearField = new TextField(String.valueOf(initialYear));
        yearField.setPrefWidth(72);
        yearField.setAlignment(Pos.CENTER);
        yearField.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-color: -fx-background;" +
                        "-fx-border-color: -fx-box-border;" +
                        "-fx-border-radius: 3;" +
                        "-fx-background-radius: 3;"
        );

        Runnable applyYear = () -> {
            try {
                int y = Integer.parseInt(yearField.getText().trim());
                int maxYear = LocalDate.now().getYear();
                if (y < 1900) y = 1900;
                if (y > maxYear) y = maxYear;
                int finalY = y;
                yearField.setText(String.valueOf(finalY));
                LocalDate base = picker.getValue() != null ? picker.getValue() : LocalDate.now();
                picker.setValue(base.withYear(finalY));
                // Force the calendar to redraw at the new year
                picker.hide();
                picker.show();
            } catch (NumberFormatException ignored) {
                LocalDate cur = picker.getValue();
                yearField.setText(String.valueOf(cur != null ? cur.getYear() : LocalDate.now().getYear()));
            }
        };

        yearField.setOnScroll(ev -> {
            try {
                int y = Integer.parseInt(yearField.getText().trim());
                y += (ev.getDeltaY() > 0) ? 1 : -1;
                int maxYear = LocalDate.now().getYear();
                if (y < 1900) y = 1900;
                if (y > maxYear) y = maxYear;
                yearField.setText(String.valueOf(y));
                applyYear.run();
            } catch (NumberFormatException ignored) {}
        });

        yearField.setOnAction(ev -> applyYear.run());

        Label hint = new Label("scroll ↑↓ or type + Enter");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-mid-text-color;");

        HBox bar = new HBox(8, new Label("Year:"), yearField, hint);
        bar.setId("yearScrollBar");
        bar.setPadding(new Insets(7, 10, 5, 10));
        bar.setAlignment(Pos.CENTER_LEFT);

        if (content instanceof VBox vb) {
            vb.getChildren().add(0, bar);
        } else {
            VBox wrapper = new VBox(bar, content);
            skin.getPopupContent().getScene().setRoot(wrapper);
        }
    }


    public void chooseFile(ActionEvent e) {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter txtFilter =
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(txtFilter);
        fc.setSelectedExtensionFilter(txtFilter);
        selectedFiles = fc.showOpenMultipleDialog(null);
        if (selectedFiles != null) {
            for (File f : selectedFiles) listView.getItems().add(f.getAbsolutePath());
        }
    }


    public void addCandidatesFromFile(ActionEvent e) {
        fileError.setText("");
        if (selectedFiles == null) {
            fileError.setText("No file selected");
            fileError.setVisible(true);
            return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {

            int notVoterFound = 0;
            int infoMismatchFound = 0;
            int duplicateFound = 0;
            int successAdded = 0;

            @Override
            protected Void call() throws Exception {

                Connection conn = DatabaseConnection.getConnection();
                int totalLines = 0;
                for (File file : selectedFiles) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    while (br.readLine() != null) totalLines++;
                    br.close();
                }

                int processed = 0;

                for (File file : selectedFiles) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while ((line = br.readLine()) != null) {

                        String[] data = line.split(",");
                        String name = data[0], birthday = data[1], voter_id = data[2];
                        String cand_id = data[3], sym = data[4], constituency = data[5];

                        PreparedStatement checkVoter = conn.prepareStatement(
                                "SELECT name,birthday,constituency FROM voter_national WHERE voter_id=?");
                        checkVoter.setString(1, voter_id);
                        ResultSet rs = checkVoter.executeQuery();

                        if (!rs.next()) {
                            notVoterFound++;
                            rs.close(); checkVoter.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        String dbName = rs.getString("name"),
                                dbDob = rs.getString("birthday"),
                                dbConst = rs.getString("constituency");

                        if (!dbName.equalsIgnoreCase(name)
                                || !dbDob.equals(birthday)
                                || !dbConst.equalsIgnoreCase(constituency)) {
                            infoMismatchFound++;
                            rs.close(); checkVoter.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        PreparedStatement checkCandId = conn.prepareStatement(
                                "SELECT candidate_id FROM candidate_national WHERE candidate_id=?");
                        checkCandId.setString(1, cand_id);
                        ResultSet rs2 = checkCandId.executeQuery();

                        if (rs2.next()) {
                            duplicateFound++;
                            rs.close(); checkVoter.close();
                            rs2.close(); checkCandId.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        PreparedStatement checkSym = conn.prepareStatement(
                                "SELECT sign FROM candidate_national WHERE sign=? AND constituency=?");
                        checkSym.setString(1, sym);
                        checkSym.setString(2, constituency);
                        ResultSet rs3 = checkSym.executeQuery();

                        if (rs3.next()) {
                            duplicateFound++;
                            rs.close(); checkVoter.close();
                            rs2.close(); checkCandId.close();
                            rs3.close(); checkSym.close();
                            processed++; updateProgress(processed, totalLines);
                            continue;
                        }

                        PreparedStatement insert = conn.prepareStatement(
                                "INSERT INTO candidate_national(name,birthday,voter_id,candidate_id,sign,constituency) VALUES(?,?,?,?,?,?)");
                        insert.setString(1, name);
                        insert.setString(2, birthday);
                        insert.setString(3, voter_id);
                        insert.setString(4, cand_id);
                        insert.setString(5, sym);
                        insert.setString(6, constituency);
                        insert.executeUpdate();

                        PreparedStatement updateTotal = conn.prepareStatement(
                                "UPDATE national SET total_candidates = total_candidates + 1 WHERE constituency=?");
                        updateTotal.setString(1, constituency);
                        updateTotal.executeUpdate();

                        rs.close(); checkVoter.close();
                        rs2.close(); checkCandId.close();
                        rs3.close(); checkSym.close();
                        insert.close(); updateTotal.close();

                        successAdded++;
                        processed++;
                        updateProgress(processed, totalLines);
                    }

                    br.close();
                }

                conn.close();
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);

                if (notVoterFound > 0) {
                    Alert a = new Alert(AlertType.WARNING);
                    a.setTitle("Invalid Voter");
                    a.setContentText(notVoterFound + " IDs in the file are not voters.\nThey were ignored.");
                    a.showAndWait();
                }

                if (infoMismatchFound > 0) {
                    Alert a = new Alert(AlertType.WARNING);
                    a.setTitle("Information Mismatch");
                    a.setContentText(infoMismatchFound + " candidate information does not match voter database.\nThey were ignored.");
                    a.showAndWait();
                }

                if (duplicateFound > 0) {
                    Alert a = new Alert(AlertType.WARNING);
                    a.setTitle("Duplicate Found");
                    a.setContentText(duplicateFound + " duplicate candidate IDs or symbols found.\nThey were ignored.");
                    a.showAndWait();
                }

                if (successAdded > 0) {
                    Alert a = new Alert(AlertType.INFORMATION);
                    a.setTitle("Success");
                    a.setContentText(successAdded + " valid candidates from file added successfully.");
                    a.showAndWait();
                    clearAllFields();
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
    // ========================== ADD CANDIDATE (MANUAL) ==========================
    public void addCandidate() {
        nameError.setVisible(false); dobError.setVisible(false); constError.setVisible(false);
        duplicateVoter.setVisible(false); duplicateCandidate.setVisible(false); symbolError.setVisible(false);

        String name = nameField.getText().trim(), voterId = voterIdField.getText().trim();
        String candidateId = candidateIdField.getText().trim(), symbolStr = symbol.getText().trim();
        String constituency = constituencyBox.getValue();
        LocalDate dob = dobPicker.getValue();
        boolean hasError = false;

        if (name.isEmpty())        { nameError.setText("Name cannot be empty!");            nameError.setVisible(true);        hasError = true; }
        if (dob == null)           { dobError.setText("Date of Birth must be selected!");   dobError.setVisible(true);         hasError = true; }
        if (voterId.isEmpty())     { duplicateVoter.setText("Voter ID cannot be empty!");   duplicateVoter.setVisible(true);   hasError = true; }
        if (candidateId.isEmpty()) { duplicateCandidate.setText("Candidate ID cannot be empty!"); duplicateCandidate.setVisible(true); hasError = true; }
        if (symbolStr.isEmpty())   { symbolError.setText("Symbol cannot be empty!");        symbolError.setVisible(true);      hasError = true; }
        if (constituency == null)  { constError.setText("Constituency must be selected!");  constError.setVisible(true);       hasError = true; }
        if (hasError) return;

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement checkVoter = conn.prepareStatement(
                    "SELECT name,birthday,constituency FROM voter_national WHERE voter_id=?");
            checkVoter.setString(1, voterId);
            ResultSet rs = checkVoter.executeQuery();
            if (!rs.next()) {
                duplicateVoter.setText("This voter ID does not exist!"); duplicateVoter.setVisible(true);
                rs.close(); checkVoter.close(); conn.close(); return;
            }

            String dbName = rs.getString("name"), dbDob = rs.getString("birthday"), dbConst = rs.getString("constituency");
            boolean mismatch = false;
            if (!dbName.equalsIgnoreCase(name))          { nameError.setText("Name does not match voter record!");          nameError.setVisible(true);  mismatch = true; }
            if (!dbDob.equals(dob.toString()))            { dobError.setText("DOB does not match voter record!");            dobError.setVisible(true);   mismatch = true; }
            if (!dbConst.equalsIgnoreCase(constituency))  { constError.setText("Constituency does not match voter record!"); constError.setVisible(true); mismatch = true; }
            if (mismatch) { rs.close(); checkVoter.close(); conn.close(); return; }

            PreparedStatement checkCand = conn.prepareStatement("SELECT voter_id FROM candidate_national WHERE voter_id=?");
            checkCand.setString(1, voterId);
            ResultSet rs2 = checkCand.executeQuery();
            if (rs2.next()) {
                duplicateVoter.setText("This voter is already a candidate!"); duplicateVoter.setVisible(true);
                rs.close(); checkVoter.close(); rs2.close(); checkCand.close(); conn.close(); return;
            }

            PreparedStatement checkCandId = conn.prepareStatement("SELECT candidate_id FROM candidate_national WHERE candidate_id=?");
            checkCandId.setString(1, candidateId);
            ResultSet rs3 = checkCandId.executeQuery();
            if (rs3.next()) {
                duplicateCandidate.setText("Candidate ID already exists!"); duplicateCandidate.setVisible(true);
                rs.close(); checkVoter.close(); rs2.close(); checkCand.close(); rs3.close(); checkCandId.close(); conn.close(); return;
            }

            PreparedStatement checkSym = conn.prepareStatement("SELECT sign FROM candidate_national WHERE sign=? AND constituency=?");
            checkSym.setString(1, symbolStr); checkSym.setString(2, constituency);
            ResultSet rs4 = checkSym.executeQuery();
            if (rs4.next()) {
                symbolError.setText("This symbol is already used in this constituency!"); symbolError.setVisible(true);
                rs.close(); checkVoter.close(); rs2.close(); checkCand.close(); rs3.close(); checkCandId.close(); rs4.close(); checkSym.close(); conn.close(); return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO candidate_national(name,birthday,voter_id,candidate_id,sign,constituency) VALUES(?,?,?,?,?,?)");
            ps.setString(1, name); ps.setString(2, dob.toString()); ps.setString(3, voterId);
            ps.setString(4, candidateId); ps.setString(5, symbolStr); ps.setString(6, constituency);
            ps.executeUpdate();

            PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE national SET total_candidates = total_candidates + 1 WHERE constituency = ?");
            updatePs.setString(1, constituency); updatePs.executeUpdate();

            rs.close(); checkVoter.close(); rs2.close(); checkCand.close(); rs3.close();
            checkCandId.close(); rs4.close(); checkSym.close(); ps.close(); updatePs.close(); conn.close();

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Success"); a.setHeaderText(null); a.setContentText("Candidate added successfully!");
            a.showAndWait();
            clearAllFields();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========================== BACK ==========================
    public void onBack(ActionEvent x) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));
            ((Stage) ((Node) x.getSource()).getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ========================== CLEAR ALL FIELDS ==========================
    public void clearAllFields() {
        nameField.clear(); voterIdField.clear(); candidateIdField.clear(); symbol.clear();
        dobPicker.setValue(null);
        javafx.application.Platform.runLater(() -> {
            dobPicker.getEditor().clear();
            dobPicker.getEditor().setPromptText(DATE_PROMPT);
        });
        constituencyBox.setValue(null);
        applyConstituencyPromptFix();
        listView.getItems().clear();
        selectedFiles = null;
        nameError.setVisible(false); dobError.setVisible(false); constError.setVisible(false);
        symbolError.setVisible(false); duplicateVoter.setVisible(false);
        duplicateCandidate.setVisible(false); fileError.setVisible(false);
    }
}
