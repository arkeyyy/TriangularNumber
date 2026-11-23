module org.example.triangular_number {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.triangular_number to javafx.fxml;
    exports org.example.triangular_number;
}