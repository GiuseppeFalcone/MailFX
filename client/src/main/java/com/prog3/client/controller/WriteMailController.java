package com.prog3.client.controller;

import com.prog3.client.library.AlertNotification;
import com.prog3.client.library.Check;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WriteMailController {
    @FXML
    private VBox recipientsBox;
    @FXML
    private TextField firstRecipient;
    @FXML
    private Button addRecipient;
    @FXML
    private TextField subjectField; // Campo per l'oggetto
    @FXML
    private TextArea bodyField; // Area di testo per il corpo della mail

    private String userMail;

    private List<String> recipients;

    int insertIndex = 2;
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8082;

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    @FXML
    public void initialize() {
        recipients = new ArrayList<>();
        initFields("", "", "");
    }

    public void initFields(String recipient, String subject, String body) {
        if (recipient.contains("|")) {
            initMultipleRecipients(recipient); // Inizializza più destinatari
        } else {
            firstRecipient.setText(recipient); // Imposta il destinatario iniziale
            if (!recipient.isEmpty() && Check.isValidEmail(recipient)) {
                recipients.add(recipient); // Aggiungi il destinatario iniziale alla lista
            }
            addRecipientTxtFieldListener(firstRecipient); // Collega il listener
        }
        subjectField.setText(subject); // Imposta l'oggetto
        bodyField.setText(body);       // Imposta il corpo
    }



    private void initMultipleRecipients(String recipients) {
        String[] recipientsParts = recipients.split("\\|");
        firstRecipient.setText(recipientsParts[0]);
        this.recipients.add(recipientsParts[0]);
        for (int indx = 1; indx < recipientsParts.length; indx++) {
            addHBoxWithRecipient(recipientsParts[indx]);
            this.recipients.add(recipientsParts[indx]);
        }
    }

    private void addHBoxWithRecipient(String recipientEmail) {
        // Creazione di un nuovo HBox per il nuovo destinatario
        HBox recipientBox = new HBox();
        recipientBox.setPadding(new Insets(0, 0, 0, 54));

        TextField recipientField = new TextField();
        recipientField.setPrefWidth(512.0);
        addRecipientTxtFieldListener(recipientField);
        recipientField.setText(recipientEmail);

        Button removeButton = new Button("-");
        addRemoveButtonListener(removeButton, recipientField, recipientBox);

        recipientBox.getChildren().addAll(recipientField, removeButton);
        recipientsBox.getChildren().add(insertIndex++, recipientBox);

    }


    @FXML
    public void addHBoxWithTextField() {
        checkFirstField();
        // Creazione di un nuovo HBox per il nuovo destinatario
        HBox newRecipientBox = new HBox();
        newRecipientBox.setPadding(new Insets(0, 0, 0, 54));

        TextField newRecipientField = new TextField();
        addRecipientTxtFieldListener(newRecipientField);
        newRecipientField.setPromptText("example@mail.com");
        newRecipientField.setPrefWidth(512.0);

        // Bottone per rimuovere il destinatario
        Button removeButton = new Button("-");
        addRemoveButtonListener(removeButton, newRecipientField, newRecipientBox);


        // Aggiunge un listener per validare e aggiungere il destinatario
        newRecipientBox.getChildren().addAll(newRecipientField, removeButton);
        recipientsBox.getChildren().add(insertIndex++, newRecipientBox);
    }

    @FXML
    private void handleSendEmail() {
        String subject = subjectField.getText();
        String body = bodyField.getText().replace("\n", "\\n").replace("|", "\\|");

        // Controlla che i campi non siano vuoti
        if (recipients.isEmpty()) {
            AlertNotification.emptyField("Inserisci almeno un destinatario");
            return;
        }
        if (subject.isEmpty()) {
            subject = " ";
        }
        if (body.isEmpty()) {
            body = " ";
        }
        // Costruisci la stringa da inviare al server
        String recipientsString = String.join("#", recipients); // Destinatari separati da #
        String emailString = String.format(
                "SEND_EMAIL|%s|%s|%s|%s|%s",
                userMail,
                recipientsString,
                subject.trim(),
                body.trim(),
                LocalDateTime.now()
        );

        // Invia la stringa al server
        new Thread(() -> {
            Socket sendEmailSocket = null;
            boolean connected = false;

            while (!connected) {
                try {
                    sendEmailSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    connected = true;
                } catch (Exception e) {
                    Platform.runLater(() -> AlertNotification.errorConnection("Errore di connesione al server, " +
                            "Riprovo fra 20 secondi"));
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            try { // Cambia indirizzo IP e porta se necessario
                PrintWriter out = new PrintWriter(sendEmailSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(sendEmailSocket.getInputStream()));

                // Invia la richiesta al server
                out.println(emailString);

                // Leggi la risposta del server
                String response = in.readLine();
                String[] responseParts = response.split("\\|");
                String status = responseParts[0];
                String message = responseParts[1];

                try {
                    sendEmailSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Platform.runLater(() -> {
                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        AlertNotification.showResponsetoSendMessage("Successo", "Email inviata con successo.");
                        closeStage(); // Pulisci i campi dopo l'invio
                    } else {
                        AlertNotification.showResponsetoSendMessage("Errore", message);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertNotification.errorConnection("Errore di comunicazione con il server." + e.getMessage()));
            }
        }).start();
    }

    private void closeStage() {
        Stage currentStage = (Stage) recipientsBox.getScene().getWindow();
        currentStage.close();
    }

    private void checkFirstField() {
        String firstEmail = firstRecipient.getText();

        if (firstEmail.isEmpty() || !Check.isValidEmail(firstEmail)) {
            AlertNotification.showAlertWrongEmail("Il primo destinatario non è valido!");
            return;
        }

        if (!recipients.contains(firstEmail)) {
            recipients.add(firstEmail);
            firstRecipient.setEditable(false); // Blocca modifiche al campo
        }
    }

    private void addRemoveButtonListener(Button removeButton, TextField textField, HBox recipientBox) {
        Platform.runLater(() ->
                removeButton.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) { // Quando il bottone ottiene il focus
                        recipientsBox.getChildren().remove(recipientBox);
                        recipients.remove(textField.getText());
                        insertIndex--;
                        addRecipient.setDisable(false);
                        if (insertIndex == 2) {
                            firstRecipient.setEditable(true);
                        }
                    }
                }));
    }

    private void addRecipientTxtFieldListener(TextField recipientField) {
        // Listener per monitorare modifiche al testo
        recipientField.focusedProperty().addListener((observable, oldFocus, newFocus) -> {
            if (!newFocus) { // L'utente ha perso il focus dal campo
                String email = recipientField.getText();

                if (email.isEmpty()) {
                    recipientField.setStyle(""); // Campo vuoto, nessun errore
                    addRecipient.setDisable(false);
                    return;
                }

                if (!Check.isValidEmail(email)) { // Email non valida
                    recipientField.setStyle("-fx-border-color: red;");
                    AlertNotification.showAlertWrongEmail("Il destinatario inserito non è valido!");
                    addRecipient.setDisable(true);
                } else { // Email valida
                    recipientField.setStyle("-fx-border-color: green;");
                    addRecipient.setDisable(false);

                    // Aggiungi l'email alla lista solo se non esiste già
                    if (!recipients.contains(email)) {
                        recipients.add(email);
                    }
                }
            }
        });
    }


}