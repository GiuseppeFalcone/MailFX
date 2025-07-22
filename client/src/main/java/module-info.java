module com.prog3.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.prog3.client to javafx.fxml;
    exports com.prog3.client;
    exports com.prog3.client.controller;
    opens com.prog3.client.controller to javafx.fxml;
}