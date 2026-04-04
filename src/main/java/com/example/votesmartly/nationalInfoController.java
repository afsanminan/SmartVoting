package com.example.votesmartly;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class nationalInfoController {

    // ---------- Row Model ----------
    public static class const_row {
        private String const_name;
        private int const_voter;
        private int const_cand;

        public const_row(String name, int voter, int cand) {
            this.const_name = name;
            this.const_voter = voter;
            this.const_cand = cand;
        }

        public String getConst_name() { return const_name; }
        public int getConst_voter() { return const_voter; }
        public int getConst_cand() { return const_cand; }
    }

    // ---------- FXML ----------
    @FXML public TableView<const_row> const_table;
    @FXML public TableColumn<const_row, String> name_col;
    @FXML public TableColumn<const_row, Integer> voter_col;
    @FXML public TableColumn<const_row, Integer> cand_col;
    @FXML public TableColumn<const_row, Integer> const_no;

    @FXML public Label total_voter;
    @FXML public Label total_cand;

    @FXML public TextField const_field;
    @FXML public Button addConst;
    @FXML public Button deleteConst;
    @FXML public ProgressIndicator deleteProgress;

    String[] allConstituencies = {
            "Panchagarh-1","Panchagarh-2",
            "Thakurgaon-1","Thakurgaon-2","Thakurgaon-3",
            "Dinajpur-1","Dinajpur-2","Dinajpur-3","Dinajpur-4","Dinajpur-5","Dinajpur-6",
            "Nilphamari-1","Nilphamari-2","Nilphamari-3","Nilphamari-4",
            "Lalmonirhat-1","Lalmonirhat-2","Lalmonirhat-3",
            "Rangpur-1","Rangpur-2","Rangpur-3","Rangpur-4","Rangpur-5","Rangpur-6",
            "Kurigram-1","Kurigram-2","Kurigram-3","Kurigram-4",
            "Gaibandha-1","Gaibandha-2","Gaibandha-3","Gaibandha-4","Gaibandha-5",

            // Rajshahi Division
            "Joypurhat-1","Joypurhat-2",
            "Bogura-1","Bogura-2","Bogura-3","Bogura-4","Bogura-5","Bogura-6","Bogura-7",
            "Chapainawabganj-1","Chapainawabganj-2","Chapainawabganj-3",
            "Naogaon-1","Naogaon-2","Naogaon-3","Naogaon-4","Naogaon-5","Naogaon-6",
            "Rajshahi-1","Rajshahi-2","Rajshahi-3","Rajshahi-4","Rajshahi-5","Rajshahi-6",
            "Natore-1","Natore-2","Natore-3","Natore-4",
            "Sirajganj-1","Sirajganj-2","Sirajganj-3","Sirajganj-4","Sirajganj-5","Sirajganj-6",
            "Pabna-1","Pabna-2","Pabna-3","Pabna-4","Pabna-5",

            // Khulna Division
            "Meherpur-1","Meherpur-2",
            "Kushtia-1","Kushtia-2","Kushtia-3","Kushtia-4",
            "Chuadanga-1","Chuadanga-2",
            "Jhenaidah-1","Jhenaidah-2","Jhenaidah-3","Jhenaidah-4",
            "Jashore-1","Jashore-2","Jashore-3","Jashore-4","Jashore-5","Jashore-6",
            "Magura-1","Magura-2",
            "Narail-1","Narail-2",
            "Bagerhat-1","Bagerhat-2","Bagerhat-3","Bagerhat-4",
            "Khulna-1","Khulna-2","Khulna-3","Khulna-4","Khulna-5","Khulna-6",
            "Satkhira-1","Satkhira-2","Satkhira-3","Satkhira-4",

            // Barishal Division
            "Barguna-1","Barguna-2",
            "Patuakhali-1","Patuakhali-2","Patuakhali-3","Patuakhali-4",
            "Bhola-1","Bhola-2","Bhola-3","Bhola-4",
            "Barishal-1","Barishal-2","Barishal-3","Barishal-4","Barishal-5","Barishal-6",
            "Jhalokathi-1","Jhalokathi-2",
            "Pirojpur-1","Pirojpur-2","Pirojpur-3",

            // Mymensingh Division
            "Jamalpur-1","Jamalpur-2","Jamalpur-3","Jamalpur-4","Jamalpur-5",
            "Sherpur-1","Sherpur-2","Sherpur-3",
            "Mymensingh-1","Mymensingh-2","Mymensingh-3","Mymensingh-4","Mymensingh-5","Mymensingh-6","Mymensingh-7","Mymensingh-8","Mymensingh-9","Mymensingh-10","Mymensingh-11",
            "Netrokona-1","Netrokona-2","Netrokona-3","Netrokona-4","Netrokona-5",

            // Dhaka Division
            "Kishoreganj-1","Kishoreganj-2","Kishoreganj-3","Kishoreganj-4","Kishoreganj-5","Kishoreganj-6",
            "Tangail-1","Tangail-2","Tangail-3","Tangail-4","Tangail-5","Tangail-6","Tangail-7","Tangail-8",
            "Manikganj-1","Manikganj-2","Manikganj-3",
            "Munshiganj-1","Munshiganj-2","Munshiganj-3",
            "Dhaka-1","Dhaka-2","Dhaka-3","Dhaka-4","Dhaka-5","Dhaka-6","Dhaka-7","Dhaka-8","Dhaka-9","Dhaka-10","Dhaka-11","Dhaka-12","Dhaka-13","Dhaka-14","Dhaka-15","Dhaka-16","Dhaka-17","Dhaka-18","Dhaka-19","Dhaka-20",
            "Gazipur-1","Gazipur-2","Gazipur-3","Gazipur-4","Gazipur-5",
            "Narsingdi-1","Narsingdi-2","Narsingdi-3","Narsingdi-4","Narsingdi-5",
            "Narayanganj-1","Narayanganj-2","Narayanganj-3","Narayanganj-4","Narayanganj-5",
            "Rajbari-1","Rajbari-2",
            "Faridpur-1","Faridpur-2","Faridpur-3","Faridpur-4",
            "Gopalganj-1","Gopalganj-2","Gopalganj-3",
            "Madaripur-1","Madaripur-2","Madaripur-3",
            "Shariatpur-1","Shariatpur-2","Shariatpur-3",

            // Sylhet Division
            "Sunamganj-1","Sunamganj-2","Sunamganj-3","Sunamganj-4","Sunamganj-5",
            "Sylhet-1","Sylhet-2","Sylhet-3","Sylhet-4","Sylhet-5","Sylhet-6",
            "Moulvibazar-1","Moulvibazar-2","Moulvibazar-3","Moulvibazar-4",
            "Habiganj-1","Habiganj-2","Habiganj-3","Habiganj-4",

            // Chattogram Division
            "Brahmanbaria-1","Brahmanbaria-2","Brahmanbaria-3","Brahmanbaria-4","Brahmanbaria-5","Brahmanbaria-6",
            "Cumilla-1","Cumilla-2","Cumilla-3","Cumilla-4","Cumilla-5","Cumilla-6","Cumilla-7","Cumilla-8","Cumilla-9","Cumilla-10","Cumilla-11",
            "Chandpur-1","Chandpur-2","Chandpur-3","Chandpur-4","Chandpur-5",
            "Feni-1","Feni-2","Feni-3",
            "Noakhali-1","Noakhali-2","Noakhali-3","Noakhali-4","Noakhali-5","Noakhali-6",
            "Lakshmipur-1","Lakshmipur-2","Lakshmipur-3","Lakshmipur-4",
            "Chattogram-1","Chattogram-2","Chattogram-3","Chattogram-4","Chattogram-5","Chattogram-6","Chattogram-7","Chattogram-8","Chattogram-9","Chattogram-10","Chattogram-11","Chattogram-12","Chattogram-13","Chattogram-14","Chattogram-15","Chattogram-16",
            "Cox's Bazar-1","Cox's Bazar-2","Cox's Bazar-3","Cox's Bazar-4",
            "Khagrachhari-1","Rangamati-1","Bandarban-1"
    };

    ObservableList<const_row> tableData = FXCollections.observableArrayList();


    // ---------- Initialize ----------
    @FXML
    public void initialize() {
        name_col.setCellValueFactory(new PropertyValueFactory<>("const_name"));
        voter_col.setCellValueFactory(new PropertyValueFactory<>("const_voter"));
        cand_col.setCellValueFactory(new PropertyValueFactory<>("const_cand"));

        // Row numbering করা
        const_no.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setText(null);
                else setText(String.valueOf(getIndex() + 1));
            }
        });

        loadTable();
        setupSearch();
    }

    // ---------- Load Constituency Table ----------
    public void loadTable() {
        tableData.clear();

        try (Connection con = DatabaseConnection.getConnection()) {
            String q = "SELECT constituency, total_voters, total_candidates FROM national";
            PreparedStatement ps = con.prepareStatement(q);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tableData.add(new const_row(
                        rs.getString("constituency"),
                        rs.getInt("total_voters"),
                        rs.getInt("total_candidates")
                ));
            }

            const_table.setItems(tableData);
            updateLabels();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Update Total Labels ----------
    public void updateLabels() {
        int totalV = tableData.stream().mapToInt(const_row::getConst_voter).sum();
        int totalC = tableData.stream().mapToInt(const_row::getConst_cand).sum();

        total_voter.setText("Total Voters: " + totalV);
        total_cand.setText("Total Candidates: " + totalC);
    }

    // ---------- Search Field ----------
    private void setupSearch() {
        const_field.textProperty().addListener((obs, oldVal, newVal) -> {
            ObservableList<const_row> filtered = FXCollections.observableArrayList();

            for (const_row r : tableData) {
                if (r.getConst_name().toLowerCase().contains(newVal.toLowerCase())) {
                    filtered.add(r);
                }
            }
            const_table.setItems(filtered);
        });
    }

    // ---------- Delete Button ----------
    @FXML
    public void deleteSelected() {
        ObservableList<const_row> selected = const_table.getSelectionModel().getSelectedItems();

        if (selected.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please select at least one constituency!");
            a.show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete selected constituencies?\n" +
                        "All voters and candidates under these constituencies will be deleted permanently!",
                ButtonType.YES, ButtonType.CANCEL);

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            const_table.getSelectionModel().clearSelection();
            return;
        }

        // ---------- Start Deletion with ProgressBar ----------
        deleteProgress.setVisible(true);
        deleteConst.setDisable(true);
        addConst.setDisable(true);

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                try (Connection con = DatabaseConnection.getConnection()) {

                    int total = selected.size();
                    int count = 0;

                    for (const_row row : selected) {

                        String cname = row.getConst_name();

                        // national table delete
                        PreparedStatement p1 =
                                con.prepareStatement("DELETE FROM national WHERE constituency = ?");
                        p1.setString(1, cname);
                        p1.executeUpdate();

                        // voter_national delete
                        PreparedStatement p2 =
                                con.prepareStatement("DELETE FROM voter_national WHERE constituency = ?");
                        p2.setString(1, cname);
                        p2.executeUpdate();

                        // candidate_national delete
                        PreparedStatement p3 =
                                con.prepareStatement("DELETE FROM candidate_national WHERE constituency = ?");
                        p3.setString(1, cname);
                        p3.executeUpdate();

                        count++;
                        updateProgress(count, total);
                        Thread.sleep(300); // শুধু demo effect, চাইলে বাদ দিতে পারো
                    }
                }

                return null;
            }
        };

        deleteTask.setOnSucceeded(ev -> {
            deleteProgress.setVisible(false);
            deleteConst.setDisable(false);
            addConst.setDisable(false);

            loadTable();

            Alert done = new Alert(Alert.AlertType.INFORMATION,
                    "Selected constituencies deleted successfully!");
            done.show();
        });

        new Thread(deleteTask).start();
    }
    @FXML
    public void openAddPopup() {

        // Popup window
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Add Constituencies");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label msg = new Label("Select constituencies you want to add:");

        ListView<String> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Filter — only those not in national DB
        ObservableList<String> options = FXCollections.observableArrayList();

        try (Connection con = DatabaseConnection.getConnection()) {
            for (String c : allConstituencies) {

                PreparedStatement ps =
                        con.prepareStatement("SELECT * FROM national WHERE constituency = ?");
                ps.setString(1, c);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {   // not found in DB
                    options.add(c);
                }
                System.out.println("Tore chudi");
            }
        } catch (Exception e) { e.printStackTrace(); }

        listView.setItems(options);

        Button addBtn = new Button("Add");

        // If all are already added
        if (options.isEmpty()) {
            msg.setText("All constituencies are already selected!");
            addBtn.setDisable(true);
        }

        // Add button action
        addBtn.setOnAction(ev -> {
            ObservableList<String> selected = listView.getSelectionModel().getSelectedItems();

            if (selected.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please select at least one constituency!");
                a.show();
                return;
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                PreparedStatement ps =
                        con.prepareStatement("INSERT INTO national(constituency,total_voters,total_candidates) VALUES(?,?,?)");

                for (String c : selected) {
                    ps.setString(1, c);
                    ps.setInt(2, 0);
                    ps.setInt(3, 0);
                    ps.executeUpdate();
                }

                loadTable(); // main table refresh

            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.close();
        });

        root.getChildren().addAll(msg, listView, addBtn);

        Scene scene = new Scene(root, 400, 500);
        popup.setScene(scene);
        popup.showAndWait();
    }
    public void showVoters(ActionEvent d)
    {
        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("showNationalVoters.fxml"));

            Stage stage = (Stage)((Node)d.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            System.out.println("In show voters change");

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public void showCandidates(ActionEvent f)
    {
        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("showNationalCandidate.fxml"));

            Stage stage = (Stage)((Node)f.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public  void onBack(ActionEvent g)
    {
        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));

            Stage stage = (Stage)((Node)g.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}