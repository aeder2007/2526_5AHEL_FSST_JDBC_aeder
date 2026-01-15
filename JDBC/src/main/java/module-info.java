module com.example.jdbc {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.jdbc to javafx.fxml;
    exports com.example.jdbc;
}