module com.example.associations_universitaires_javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires java.desktop;
    requires jdk.sctp;

    opens com.example.associations_universitaires_javafx to javafx.fxml;
    exports com.example.associations_universitaires_javafx;
}