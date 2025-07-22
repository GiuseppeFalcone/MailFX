package com.prog3.client.library;

import javafx.scene.control.Alert;

public class AlertNotification {
    public static void showAlertWrongEmail(String header) {
        String content = """
                La mail può contenere:
                \tcaratteri ammessi: a-z / A-Z
                \tsimboli ammessi: . _ 0-9
                \tdominio: @mail.com""";
        String title = "Mail Non Valida";
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("La seguente mail è sbagliata: "+header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    public static void showResponsetoSendMessage(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
    public static void errorConnection(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        String title = "Errore di Connessione";
        String header = "Impossibile connettersi al server";
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    public static void emptyField(String header) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        String title = "Errore";
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
