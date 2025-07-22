package com.prog3.client.controller;

import com.prog3.client.library.AlertNotification;
import com.prog3.client.library.Check;
import com.prog3.client.model.ClientStorage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class LoginController {
    @FXML
    private TextField userMailTxtField;
    @FXML
    private Label reinserisciMail;
    @FXML
    private Button loginBtn;
    @FXML
    private TextArea emailRegex;

    private String userGivenEmail;
    public ClientStorage clientStorage;

    private Socket loginSocket;
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8082;


    @FXML
    public void initialize() {
        // Inizializza il modello del client
        clientStorage = new ClientStorage();

        userMailTxtField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Check.isValidEmail(newValue.trim())) {
                userGivenEmail = newValue.trim();
                userMailTxtField.setStyle("-fx-border-color: green;");
                reinserisciMail.setVisible(false);
                emailRegex.setStyle("-fx-border-color: green;");
                loginBtn.setDisable(false);
            } else {
                loginBtn.setDisable(true);
                userMailTxtField.setStyle("-fx-border-color: red;");
                emailRegex.setStyle("-fx-border-color: red;");
                reinserisciMail.setVisible(true);
            }
        });

        emailRegex.setText("""
                
                La mail può contenere:
                \tcaratteri ammessi: a-z / A-Z
                \tsimboli ammessi: . _ 0-9
                \tdominio: @mail.com""");

        safeClosing();
    }

    @FXML
    private void handleLogInButtonClick() {
        loginBtn.setDisable(true);
        // Avvia un thread per gestire la comunicazione con il server
        new Thread(() -> {
            try {
                loginSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(loginSocket.getInputStream()));
                PrintWriter out = new PrintWriter(loginSocket.getOutputStream(), true);

                sendLoginRequestToServer(out);

                String response = in.readLine();
                handleLoginResponse(response);

                loginSocket.close();
            } catch (IOException e) {
                Platform.runLater(()->AlertNotification.errorConnection("Errore nella connessione al server"));
                loginBtn.setDisable(false);
            }
        }).start();
    }


    private void sendLoginRequestToServer(PrintWriter out) {
        // Create login request
        String loginRequest = String.format("LOGIN|%s", userGivenEmail);
        out.println(loginRequest);
    }

    private void handleLoginResponse(String responseString) {
        try {
            String[] responseParts = responseString.split("\\|");
            String status = responseParts[0];
            String message = responseParts[1];

            if ("SUCCESS".equalsIgnoreCase(status)) {
                clientStorage.setUserEmail(userGivenEmail);
                Platform.runLater(this::switchToInbox);
            } else if ("ERROR".equalsIgnoreCase(status)) {
                Platform.runLater(() -> AlertNotification.showAlertWrongEmail("Errore: " + message));
            } else {
                Platform.runLater(() -> AlertNotification.emptyField("Risposta inattesa dal server: " + responseString));
            }
        } catch (Exception e) {
            Platform.runLater(() -> AlertNotification.errorConnection("Errore nella comunicazione al server"));
        } finally {
            Platform.runLater(() -> loginBtn.setDisable(false)); // Reabilita il pulsante
        }
    }

    /**
     * Cambia la finestra corrente e carica la schermata Inbox.
     */
    private void switchToInbox() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/inbox-view.fxml"));
            Parent root = loader.load();

            InboxController inboxController = loader.getController();
            inboxController.setClientStorage(clientStorage);

            Stage currentStage = (Stage) loginBtn.getScene().getWindow();
            currentStage.setTitle("Mail Client - Inbox");
            currentStage.setScene(new Scene(root,575,475));
        } catch (IOException e) {
            Platform.runLater(() -> AlertNotification.errorConnection("Impossibile aprire finestra inbox"));
        }
    }

    private void safeClosing() {
        Platform.runLater(() -> {
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (loginSocket != null && !loginSocket.isClosed()) {
                    try {
                        loginSocket.close();
                    } catch (IOException e) {
                        Platform.runLater(() -> AlertNotification.errorConnection("Errore nella chiusura del socket"));
                    }
                }
            });
        });
    }

}