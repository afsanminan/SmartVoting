package com.example.votesmartly;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.cell.CheckBoxListCell;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.PasswordField;

import javax.xml.transform.stream.StreamSource;
import java.sql.Connection;
import java.sql.PreparedStatement;


public class NewElectionController implements Initializable {
    @FXML
    public ComboBox<String> electionTypeComboBox;
    @FXML
            public ListView<String> constituencies;
    @FXML public ComboBox<String> universities;
    @FXML public Button allSelect;
    @FXML public Button deselectAll;
    @FXML public AnchorPane forNational;
    @FXML public AnchorPane forStudent;
    @FXML public ListView<String> posts;
    @FXML public Button allPostSelected;
    @FXML public  Button allPostDeselect;
    @FXML public Label postError;
    @FXML public Label electionError;
    @FXML public Label passError;
    @FXML public Label confirmPassError;
    @FXML public Label idError;
    @FXML public  Label uniError;
    @FXML public Label idErrorNational;
    @FXML public Label passErrorNational;
    @FXML public Label confirmPassErrorNational;
    @FXML public Label constituencyError;
    @FXML public TextField electionId;

    @FXML public TextField electionIdNational;

    @FXML public PasswordField password;
    @FXML public PasswordField confirmPass;
    @FXML public PasswordField passwordNational;
    @FXML public PasswordField confirmPassNational;
    ObservableList<String> types= FXCollections.observableArrayList("National Election","Student Election");
    ObservableList<String> uni=FXCollections.observableArrayList("Bangladesh University of Engineering and Technology",
            "Khulna University of Engineering and Technology","Rajshahi University of Engineering and Technology",
            "Chattogram University of Engineering and Technology");
    ObservableList<String> post=FXCollections.observableArrayList("Vice President(VP)","General Secretary(GS)",
            "Additional General Secretary(AGS)","Sports Secretary","Literature and Cultural Secretary",
            "Science and Technology Secretary", "Common Room, Reading Room & Cafeteria Secretary","International Affairs Secretary",
            "Research & Publication Secretary","Student Transport Secretary","Social Welfare Secretary",
            "Career Development Secretary","Health & Environment Secretary");
    ObservableList<String> consts = FXCollections.observableArrayList(
            // Dhaka
            // Rangpur Division (৩৩টি আসন)
            "Panchagarh-1","Panchagarh-2",
            "Thakurgaon-1","Thakurgaon-2","Thakurgaon-3",
            "Dinajpur-1","Dinajpur-2","Dinajpur-3","Dinajpur-4","Dinajpur-5","Dinajpur-6",
            "Nilphamari-1","Nilphamari-2","Nilphamari-3","Nilphamari-4",
            "Lalmonirhat-1","Lalmonirhat-2","Lalmonirhat-3",
            "Rangpur-1","Rangpur-2","Rangpur-3","Rangpur-4","Rangpur-5","Rangpur-6",
            "Kurigram-1","Kurigram-2","Kurigram-3","Kurigram-4",
            "Gaibandha-1","Gaibandha-2","Gaibandha-3","Gaibandha-4","Gaibandha-5",

// Rajshahi Division (৩৯টি আসন)
            "Joypurhat-1","Joypurhat-2",
            "Bogura-1","Bogura-2","Bogura-3","Bogura-4","Bogura-5","Bogura-6","Bogura-7",
            "Chapainawabganj-1","Chapainawabganj-2","Chapainawabganj-3",
            "Naogaon-1","Naogaon-2","Naogaon-3","Naogaon-4","Naogaon-5","Naogaon-6",
            "Rajshahi-1","Rajshahi-2","Rajshahi-3","Rajshahi-4","Rajshahi-5","Rajshahi-6",
            "Natore-1","Natore-2","Natore-3","Natore-4",
            "Sirajganj-1","Sirajganj-2","Sirajganj-3","Sirajganj-4","Sirajganj-5","Sirajganj-6",
            "Pabna-1","Pabna-2","Pabna-3","Pabna-4","Pabna-5",

// Khulna Division (৩৬টি আসন)
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

// Barishal Division (২১টি আসন)
            "Barguna-1","Barguna-2",
            "Patuakhali-1","Patuakhali-2","Patuakhali-3","Patuakhali-4",
            "Bhola-1","Bhola-2","Bhola-3","Bhola-4",
            "Barishal-1","Barishal-2","Barishal-3","Barishal-4","Barishal-5","Barishal-6",
            "Jhalokathi-1","Jhalokathi-2",
            "Pirojpur-1","Pirojpur-2","Pirojpur-3",

// Mymensingh Division (২৪টি আসন)
            "Jamalpur-1","Jamalpur-2","Jamalpur-3","Jamalpur-4","Jamalpur-5",
            "Sherpur-1","Sherpur-2","Sherpur-3",
            "Mymensingh-1","Mymensingh-2","Mymensingh-3","Mymensingh-4","Mymensingh-5","Mymensingh-6","Mymensingh-7","Mymensingh-8","Mymensingh-9","Mymensingh-10","Mymensingh-11",
            "Netrokona-1","Netrokona-2","Netrokona-3","Netrokona-4","Netrokona-5",

// Dhaka Division (৭০টি আসন)
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

// Sylhet Division (১৯টি আসন)
            "Sunamganj-1","Sunamganj-2","Sunamganj-3","Sunamganj-4","Sunamganj-5",
            "Sylhet-1","Sylhet-2","Sylhet-3","Sylhet-4","Sylhet-5","Sylhet-6",
            "Moulvibazar-1","Moulvibazar-2","Moulvibazar-3","Moulvibazar-4",
            "Habiganj-1","Habiganj-2","Habiganj-3","Habiganj-4",

// Chattogram Division (৫৮টি আসন)
            "Brahmanbaria-1","Brahmanbaria-2","Brahmanbaria-3","Brahmanbaria-4","Brahmanbaria-5","Brahmanbaria-6",
            "Cumilla-1","Cumilla-2","Cumilla-3","Cumilla-4","Cumilla-5","Cumilla-6","Cumilla-7","Cumilla-8","Cumilla-9","Cumilla-10","Cumilla-11",
            "Chandpur-1","Chandpur-2","Chandpur-3","Chandpur-4","Chandpur-5",
            "Feni-1","Feni-2","Feni-3",
            "Noakhali-1","Noakhali-2","Noakhali-3","Noakhali-4","Noakhali-5","Noakhali-6",
            "Lakshmipur-1","Lakshmipur-2","Lakshmipur-3","Lakshmipur-4",
            "Chattogram-1","Chattogram-2","Chattogram-3","Chattogram-4","Chattogram-5","Chattogram-6","Chattogram-7","Chattogram-8","Chattogram-9","Chattogram-10","Chattogram-11","Chattogram-12","Chattogram-13","Chattogram-14","Chattogram-15","Chattogram-16",
            "Cox's Bazar-1","Cox's Bazar-2","Cox's Bazar-3","Cox's Bazar-4",
            "Khagrachhari-1",
            "Rangamati-1",
            "Bandarban-1");
    Map<String, BooleanProperty> constMap = new HashMap<>();
    Map<String, BooleanProperty> postMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        electionTypeComboBox.setItems(types);
        constituencies.setItems(consts);
        universities.setItems(uni);
        posts.setItems(post);
        for(String item : consts){
            constMap.put(item,new SimpleBooleanProperty(false));
        }
        constituencies.setCellFactory(CheckBoxListCell.forListView(item -> constMap.get(item)));
        for(String item : post){
            postMap.put(item, new SimpleBooleanProperty(false));
        }
        posts.setCellFactory(CheckBoxListCell.forListView(item -> postMap.get(item)));
    }

    public String type;
    public void typeSelected(ActionEvent e)
    {
         type=electionTypeComboBox.getValue();
        if (type != null) {
            try (FileWriter writer = new FileWriter("electiontype.txt", false)) {
                writer.write(type);
                writer.flush();
                System.out.println("Election type saved: " + type);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if("National Election".equals(type))
        {
            forNational.setVisible(true);
            forStudent.setVisible(false);
        }
        if("Student Election".equals(type))
        {
            forStudent.setVisible(true);
            forNational.setVisible(false);
        }

    }

    public void onSelect(ActionEvent e)
    {
        for(BooleanProperty property : constMap.values())
        {
            property.set(true);
        }
    }
    public void postSelect(ActionEvent e)
    {
        for(BooleanProperty property : postMap.values())
        {
            property.set(true);
        }
    }
    public void onDeselect(ActionEvent e)
    {
        for(BooleanProperty property :constMap.values())
        {
            property.set(false);
        }
    }
    public void postDeselect(ActionEvent e)
    {
        for(BooleanProperty property :postMap.values())
        {
            property.set(false);
        }
    }
    public void getSelectedConst()
    {
        for(String item :constMap.keySet())
        {
            if(constMap.get(item).get())
            {
                System.out.println(item);
            }
        }
    }
    public void getSelectedPost()
    {
        for(String item :postMap.keySet())
        {
            if(postMap.get(item).get())
            {
                System.out.println(item);
            }
        }
    }
    public int countSelectedConst() {
        int count = 0;
        for (BooleanProperty property : constMap.values()) {
            if (property.get()) {
                count++;
            }
        }
        return count;
    }
    public int countSelectedPost() {
        int count = 0;
        for (BooleanProperty property : postMap.values()) {
            if (property.get()) {
                count++;
            }
        }
        return count;
    }
    public void onContinue(ActionEvent e)
    {
        String electId=electionId.getText();
        String pass=password.getText();
        String confirm=confirmPass.getText();
        String univer=universities.getValue();
        idError.setText("");
        passError.setText("");
        confirmPassError.setText("");
        uniError.setText("");
        postError.setText("");
        idError.setVisible(true);
        passError.setVisible(true);
        confirmPassError.setVisible(true);
        uniError.setVisible(true);
        postError.setVisible(true);
        boolean hasError = false;
        int postNo=countSelectedPost();
        if(univer==null)
        {
            uniError.setText("University Name is not selected");
            hasError=true;
        }
        if (electId == null || electId.trim().isEmpty()) {
            idError.setText("Election id cannot be empty");
            hasError = true;
        }
        if (pass == null || pass.trim().isEmpty()) {
            passError.setText("Password cannot be empty");
            hasError = true;
        }
        if (confirm == null || confirm.trim().isEmpty()) {
            confirmPassError.setText("Please confirm your password");
            hasError = true;
        }
        if (!hasError && !pass.equals(confirm)) {
            confirmPassError.setText("Passwords do not match!");
            hasError = true;
        }
        if(!hasError && postNo==0)
        {
            postError.setText("Minimum one post has to be selected to continue");
            hasError=true;
        }
        if(!hasError)
        {
            try{
                Connection conn=DatabaseConnection.getConnection();
                String deleteQuery = "DELETE FROM student";
                PreparedStatement deletePst = conn.prepareStatement(deleteQuery);
                deletePst.executeUpdate();
                deletePst.close();
                String query="INSERT INTO election_info (name,id,pass) VALUES(?,?,?)";
                PreparedStatement pst= conn.prepareStatement(query);
                pst.setString(1,type);
                pst.setString(2,electId);
                pst.setString(3,pass);
                try{
                    pst.executeUpdate();
                }
                catch (SQLException a)
                {
                    System.out.println("Id repatation");
                    idError.setText("An election history with this Id exists");
                    return;
                }
                System.out.println("Election data inserted successfully");
                pst.close();
                query="INSERT INTO student(university,posts)VALUES (?,?)";
                PreparedStatement pst1=conn.prepareStatement(query);
                for(String post : postMap.keySet())
                {
                    if(postMap.get(post).get())
                    {
                        pst1.setString(1,univer);
                        pst1.setString(2, post);
                        pst1.executeUpdate();
                    }
                }
                System.out.println("posts added perfectly");
                pst1.close();
                conn.close();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                return;
            }
            try {
                Parent root = FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));
                Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    public void onContinueNational(ActionEvent e)
    {
        boolean hasError = false;

        String id=electionIdNational.getText();
        String pass=passwordNational.getText();
        String confirm=confirmPassNational.getText();
        idErrorNational.setText("");
        passErrorNational.setText("");
        confirmPassErrorNational.setText("");
        constituencyError.setText("");
        constituencyError.setVisible(true);
        idErrorNational.setVisible(true);
        passErrorNational.setVisible(true);
        confirmPassErrorNational.setVisible(true);
        int constNo=countSelectedConst();
        if (id == null || id.trim().isEmpty()) {
            idErrorNational.setText("Election Id cannot be empty");
            hasError = true;
        }
        if (pass == null || pass.trim().isEmpty()) {
            passErrorNational.setText("Password cannot be empty");
            hasError = true;
        }
        if (confirm == null || confirm.trim().isEmpty()) {
            confirmPassErrorNational.setText("Please confirm your password");
            hasError = true;
        }
        if (!hasError && !pass.equals(confirm)) {
            confirmPassErrorNational.setText("Passwords do not match!");
            hasError = true;
        }
        if(!hasError && constNo==0)
        {
            constituencyError.setText("Minimum one constituency has to be selected to continue");
            hasError=true;
        }
        if(!hasError)
        {
            try {
                Connection conn = DatabaseConnection.getConnection();
                String deleteQuery = "DELETE FROM national";
                PreparedStatement deletePst = conn.prepareStatement(deleteQuery);
                deletePst.executeUpdate();
                deletePst.close();
                String query = "INSERT INTO election_info (name, id, pass) VALUES (?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(query);
                pst.setString(1, type);
                pst.setString(2, id);
                pst.setString(3, pass);
                try{
                    pst.executeUpdate();
                }
                catch (SQLException a)
                {
                    System.out.println("Id repatation");
                    idError.setText("An election history with this Id exists");
                    return;
                }
                System.out.println("National election saved");
                pst.close();
                query="INSERT INTO national(constituency) VALUES (?)";
                PreparedStatement pst1=conn.prepareStatement(query);
                for(String constituency : constMap.keySet())
                {
                    if(constMap.get(constituency).get())
                    {
                        pst1.setString(1, constituency);
                        pst1.executeUpdate();
                    }
                }
                System.out.println("Constituencies added perfectly");
                pst1.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            try {
                Parent root = FXMLLoader.load(getClass().getResource("addOrRemove.fxml"));
                Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
