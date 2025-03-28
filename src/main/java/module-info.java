module com.example.associations_universitaires_javafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.associations_universitaires_javafx to javafx.fxml;
    exports com.example.associations_universitaires_javafx;
}