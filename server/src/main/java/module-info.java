module com.prog3.server {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.prog3.server to javafx.fxml;
    exports com.prog3.server;
    exports com.prog3.server.controller;
    opens com.prog3.server.controller to javafx.fxml;
}