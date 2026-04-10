
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
import java.time.Period;
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


public class addNationalVoterController implements Initializable {
    @FXML public Button fileButton;
    @FXML public ListView listView;
    @FXML public Label fileError;
    @FXML private TextField nameField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField voterIdField;
    @FXML private ComboBox<String> constituencyBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label duplicateLabel;
    @FXML public Label nameError;
    @FXML public Label dobError;
    @FXML public Label constError;
    List<File> selectedFiles;

    private static final String CONSTITUENCY_PROMPT = "Select constituency";
    private static final String DATE_PROMPT         = "Date of Birth";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //consd load order
        ObservableList<String> constituencies = FXCollections.observableArrayList();
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT constituency FROM national ORDER BY constituency");
            while (rs.next()) constituencies.add(rs.getString("constituency"));
            constituencyBox.setItems(constituencies);
            st.close(); rs.close(); conn.close();
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
                picker.hide();
                picker.show();
            } catch (NumberFormatException ignored) {

                LocalDate cur = picker.getValue();
                yearField.setText(String.valueOf(cur != null ? cur.getYear() : LocalDate.now().getYear()));
            }
        };

        //scroll
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

        //manual yr
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

    public void addVotersFromFile(ActionEvent e) {
        fileError.setText("");
        if (selectedFiles == null) {
            fileError.setText("No file selected");
            return;
        }

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            int duplicateFound = 0;
            int invalidConstituencyFound = 0;
            int successAdded = 0;

            @Override
            protected Void call() throws Exception {

                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement insertPs   = conn.prepareStatement("INSERT INTO voter_national(name,birthday,voter_id,constituency) VALUES(?,?,?,?)");
                PreparedStatement checkDup   = conn.prepareStatement("SELECT voter_id FROM voter_national WHERE voter_id=?");
                PreparedStatement checkConst = conn.prepareStatement("SELECT constituency FROM national WHERE constituency=?");
                PreparedStatement updateTot  = conn.prepareStatement("UPDATE national SET total_voters = total_voters + 1 WHERE constituency=?");

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
                        String[] d = line.split(",");
                        String name = d[0], birthday = d[1], voter_id = d[2], constituency = d[3];

                        checkConst.setString(1, constituency);
                        ResultSet rsConst = checkConst.executeQuery();
                        if (!rsConst.next()) {
                            invalidConstituencyFound++;
                            rsConst.close();
                            processed++;
                            updateProgress(processed, totalLines);
                            continue;
                        }
                        rsConst.close();

                        checkDup.setString(1, voter_id);
                        ResultSet rsDup = checkDup.executeQuery();
                        if (rsDup.next()) {
                            duplicateFound++;
                            rsDup.close();
                            processed++;
                            updateProgress(processed, totalLines);
                            continue;
                        }
                        rsDup.close();

                        insertPs.setString(1, name);
                        insertPs.setString(2, birthday);
                        insertPs.setString(3, voter_id);
                        insertPs.setString(4, constituency);
                        insertPs.executeUpdate();

                        updateTot.setString(1, constituency);
                        updateTot.executeUpdate();

                        successAdded++;
                        processed++;

                        updateProgress(processed, totalLines);
                    }
                    br.close();
                }

                insertPs.close();
                checkDup.close();
                checkConst.close();
                updateTot.close();
                conn.close();

                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);

                if (invalidConstituencyFound > 0) {
                    Alert a = new Alert(AlertType.WARNING);
                    a.setTitle("Invalid Constituency");
                    a.setContentText(invalidConstituencyFound + " voters had invalid constituencies and were ignored.");
                    a.showAndWait();
                }

                if (duplicateFound > 0) {
                    Alert a = new Alert(AlertType.WARNING);
                    a.setTitle("Duplicate Voter ID");
                    a.setContentText(duplicateFound + " duplicate voter IDs were ignored.");
                    a.showAndWait();
                }

                if (successAdded > 0) {
                    Alert a = new Alert(AlertType.INFORMATION);
                    a.setTitle("Success");
                    a.setContentText(successAdded + " voters added successfully.");
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



    public void addVoter(ActionEvent b) {
        nameError.setVisible(false); dobError.setVisible(false);
        constError.setVisible(false); duplicateLabel.setVisible(false);

        String name = nameField.getText().trim();
        String voterId = voterIdField.getText().trim();
        String constituency = constituencyBox.getValue();
        boolean hasError = false;

        if (name.isEmpty()) {
            nameError.setText("Name cannot be empty!"); nameError.setVisible(true); hasError = true;
        }
        if (dobPicker.getValue() == null) {
            dobError.setText("Date of birth must be selected!"); dobError.setVisible(true); hasError = true;
        } else if (Period.between(dobPicker.getValue(), LocalDate.now()).getYears() < 18) {
            dobError.setText("Voter must be at least 18 years old!"); dobError.setVisible(true); hasError = true;
        }
        if (voterId.isEmpty()) {
            duplicateLabel.setText("Voter ID cannot be empty!"); duplicateLabel.setVisible(true); hasError = true;
        }
        if (constituency == null) {
            constError.setText("A constituency must be selected!"); constError.setVisible(true); hasError = true;
        }
        if (hasError) return;

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement checkPs = conn.prepareStatement("SELECT voter_id FROM voter_national WHERE voter_id = ?");
            checkPs.setString(1, voterId);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                duplicateLabel.setText("A voter with this ID already exists!"); duplicateLabel.setVisible(true);
                rs.close(); checkPs.close(); conn.close(); return;
            }
            rs.close(); checkPs.close();

            PreparedStatement ps = conn.prepareStatement("INSERT INTO voter_national(name,birthday,voter_id,constituency) VALUES(?,?,?,?)");
            ps.setString(1, name); ps.setString(2, dobPicker.getValue().toString());
            ps.setString(3, voterId); ps.setString(4, constituency);
            ps.executeUpdate();

            PreparedStatement updatePs = conn.prepareStatement("UPDATE national SET total_voters = total_voters + 1 WHERE constituency = ?");
            updatePs.setString(1, constituency); updatePs.executeUpdate();
            ps.close(); updatePs.close(); conn.close();

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Success"); a.setHeaderText(null); a.setContentText("Voter added successfully!");
            a.showAndWait();
            clearAllFields();
        } catch (Exception ex) { ex.printStackTrace(); }
    }


    public void onBack(ActionEvent x) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));
            ((Stage) ((Node) x.getSource()).getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }


    public void clearAllFields() {
        nameField.clear(); voterIdField.clear();
        dobPicker.setValue(null);
        javafx.application.Platform.runLater(() -> {
            dobPicker.getEditor().clear();
            dobPicker.getEditor().setPromptText(DATE_PROMPT);
        });
        constituencyBox.setValue(null);
        applyConstituencyPromptFix();
        listView.getItems().clear();
        selectedFiles = null;
        nameError.setVisible(false); dobError.setVisible(false);
        constError.setVisible(false); fileError.setVisible(false);
    }
}
