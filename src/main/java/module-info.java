module com.example.votesmartly {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;

    opens com.example.votesmartly to javafx.fxml;
    exports com.example.votesmartly;
}
