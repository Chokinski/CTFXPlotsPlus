module com.jat {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    opens com.jat to javafx.fxml;
    exports com.jat;
}
