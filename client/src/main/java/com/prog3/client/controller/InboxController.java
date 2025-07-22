package com.prog3.client.controller;

import com.prog3.client.library.AlertNotification;
import com.prog3.client.model.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class InboxController {
    @FXML
    private ListView<Email> inboxListView;
    @FXML
    private MenuButton menuButton;
    @FXML
    private VBox detailEmail;
    @FXML
    private Label statoConnessione;
    @FXML
    private TextField subjectField;
    @FXML
    private TextField senderField;
    @FXML
    private TextField recipientsField;
    @FXML
    private TextArea bodyArea;

    private ClientStorage clientStorage;
    private final ObservableList<Socket> openSockets = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private final ScheduledExecutorService emailChecker = new ScheduledThreadPoolExecutor(1);
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8082;

    /**
     * Imposta il modello locale del client.
     *
     * @param clientStorage Il modello locale.
     */
    public void setClientStorage(ClientStorage clientStorage) {
        this.clientStorage = clientStorage;
    }

    /**
     * Inizializza il controller e configura l'esecutore per i thread.
     */
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            menuButton.setText(clientStorage.getUserEmail());
            detailEmail.setVisible(false);
            updateConnectionStatus();
            requestInboxToServer();
            inboxListView.setItems(clientStorage.getInbox());
            setInboxListView();
        });

        safeClosing();

    }

    public void setInboxListView() {
        // Configura il CellFactory per personalizzare l'aspetto degli elementi
        inboxListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Email email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Usa un VBox per contenere i diversi stili di testo
                    Label senderLabel = new Label(email.getSender());
                    senderLabel.setStyle("-fx-font-weight: bold;");

                    Label subjectLabel = new Label(email.getSubject());
                    subjectLabel.setStyle("-fx-font-weight: normal;");

                    Label bodyLabel = new Label(email.getBody().split("\n")[0]);
                    bodyLabel.setStyle("-fx-text-fill: grey;");

                    VBox emailBox = new VBox(senderLabel, subjectLabel, bodyLabel);
                    setGraphic(emailBox);
                }
            }
        });
        inboxListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showEmailDetails(newValue);
            } else {
                detailEmail.setVisible(false); // Nasconde la VBox se nessuna email è selezionata
            }
        });
    }

    private void showEmailDetails(Email email) {
        senderField.setText(email.getSender());
        recipientsField.setText(email.getRecipientsAsString());
        subjectField.setText(email.getSubject());
        bodyArea.setText(email.getBody());
        detailEmail.setVisible(true); // Mostra la VBox
    }

    private void requestInboxToServer() {
        new Thread(() -> {
            try {
                Socket requestInboxSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                openSockets.add(requestInboxSocket);

                BufferedReader in = new BufferedReader(new InputStreamReader(requestInboxSocket.getInputStream()));
                PrintWriter out = new PrintWriter(requestInboxSocket.getOutputStream(), true);

                String request = String.join("|", "GET_ALL_MESSAGES", clientStorage.getUserEmail());
                out.println(request);

                String responseString = in.readLine();
                handleGetAllMessagesResponse(responseString);

                try {
                    openSockets.remove(requestInboxSocket);
                    requestInboxSocket.close();
                } catch (IOException e) {
                    Platform.runLater(() -> AlertNotification.errorConnection("Errore nella richiesta della Inbox al Server"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> AlertNotification.errorConnection("Errore nella richiesta della Inbox al Server"));
            } finally {
                getNewEmails();
            }
        }).start();
    }

    private void handleGetAllMessagesResponse(String responseString) {
        String[] responseParts = responseString.split("\\|", 2);
        String status = responseParts[0];
        if ("SUCCESS".equalsIgnoreCase(status)) {
            if (responseParts[1].equalsIgnoreCase("No messages found"))
                return;
            // Validate and extract mailbox
            String[] emails = responseParts[1].split("±");
            for (String email : emails) {
                Email newEmail = reconstructSingleEmail(email);
                Platform.runLater(() -> {
                    clientStorage.addEmail(newEmail);
                    FXCollections.sort(clientStorage.getInbox(), (e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));// Ordina per data decrescente
                });
            }
        } else {
            Platform.runLater(() -> AlertNotification.errorConnection("Errore nella risposta dal server: " + responseParts[1]));
        }
    }

    private void getNewEmails() {
        Runnable ckeckNewEmailTask = () -> {
            try {
                requestNewEmails();
            } catch (Exception e) {
                Platform.runLater(() -> AlertNotification.errorConnection("Errore nel richiedere Nuove emails: " + e.getMessage()));
            }
        };
        emailChecker.scheduleAtFixedRate(ckeckNewEmailTask, 5, 5, TimeUnit.SECONDS);

    }

    private void requestNewEmails() {
        try {
            Socket newEmailsSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            openSockets.add(newEmailsSocket);

            BufferedReader in = new BufferedReader(new InputStreamReader(newEmailsSocket.getInputStream()));
            PrintWriter out = new PrintWriter(newEmailsSocket.getOutputStream(), true);

            String mostRecentEmailDate = clientStorage.mostRecentEmailDate();
            String request;
            String responseString;

            if (clientStorage.getInbox().isEmpty()) {
                request = String.join("|", "GET_ALL_MESSAGES", clientStorage.getUserEmail());
                out.println(request);
            } else {
                request = String.join("|", "GET_NEW_MESSAGES", clientStorage.getUserEmail(), mostRecentEmailDate);
                out.println(request);
            }

            responseString = in.readLine();
            handleGetAllMessagesResponse(responseString);

            if (responseString.contains("SUCCESS") && !responseString.contains("No messages found")) {
                Platform.runLater(() -> AlertNotification.showResponsetoSendMessage("Avviso", "Arrivate nuove mail"));
            }

            openSockets.remove(newEmailsSocket);
            newEmailsSocket.close();
        } catch (IOException e) {
            Platform.runLater(() -> AlertNotification.errorConnection("Errore nel richiedere nuove emails: " + e.getMessage()));
        }
    }

    @FXML
    private void handleNewMessage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/writeMail-view.fxml"));
            Parent root = loader.load();

            WriteMailController writeMailController = loader.getController();
            writeMailController.setUserMail(clientStorage.getUserEmail());

            Stage newStage = new Stage();
            newStage.setTitle("Mail Client - Write New Email");
            newStage.setScene(new Scene(root));
            newStage.show();
        } catch (IOException e) {
            Platform.runLater(() -> AlertNotification.errorConnection("Impossibile Aprire Finestra per mandare email."));
        }
    }

    @FXML
    private void handleReply() {
        Email selectedEmail = inboxListView.getSelectionModel().getSelectedItem();
        if (selectedEmail == null) {
            AlertNotification.emptyField("Nessuna email selezionata");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/writeMail-view.fxml"));
            Parent root = loader.load();

            // Ottieni il controller della nuova vista
            WriteMailController writeMailController = loader.getController();
            writeMailController.setUserMail(clientStorage.getUserEmail());
            String replyRecipient = selectedEmail.getSender();
            String replySubject = "Re: " + selectedEmail.getSubject();
            String originalMessage = String.format("""
                    
                    --- Messaggio originale ---
                    Da: %s
                    Oggetto: %s
                    Testo: %s""", selectedEmail.getSender(), selectedEmail.getSubject(), selectedEmail.getBody());

            writeMailController.initFields(replyRecipient, replySubject, originalMessage);

            Stage newStage = new Stage();
            newStage.setTitle("Rispondi al messaggio");
            newStage.setScene(new Scene(root));
            newStage.show();
        } catch (IOException e) {
            AlertNotification.showResponsetoSendMessage("Errore", "Errore nel caricamento della vista di risposta.");
        }
    }

    @FXML
    private void handleReplyAll() {
        Email selectedEmail = inboxListView.getSelectionModel().getSelectedItem();
        if (selectedEmail == null) {
            AlertNotification.emptyField("Nessuna email selezionata");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/writeMail-view.fxml"));
            Parent root = loader.load();

            // Ottieni il controller della nuova vista
            WriteMailController writeMailController = loader.getController();
            writeMailController.setUserMail(clientStorage.getUserEmail());

            String replyRecipients = String.join("|", selectedEmail.getRecipients());
            if (!replyRecipients.contains(selectedEmail.getSender())) {
                replyRecipients = String.join("|", replyRecipients, selectedEmail.getSender());
            }
            String replySubject = "Re: " + selectedEmail.getSubject();
            String originalMessage = String.format("""
                    
                    --- Messaggio originale ---
                    Da: %s
                    A: %s
                    Oggetto: %s
                    Testo: %s""", selectedEmail.getSender(), selectedEmail.getRecipientsAsString(), selectedEmail.getSubject(), selectedEmail.getBody());

            writeMailController.initFields(replyRecipients, replySubject, originalMessage);

            Stage newStage = new Stage();
            newStage.setTitle("Rispondi al messaggio");
            newStage.setScene(new Scene(root));
            newStage.show();
        } catch (IOException e) {
            AlertNotification.showResponsetoSendMessage("Errore", "Errore nel caricamento della vista di risposta.");
        }
    }

    @FXML
    private void handleDeleteEmail() {
        Email selectedEmail = inboxListView.getSelectionModel().getSelectedItem();
        if (selectedEmail == null) {
            AlertNotification.emptyField("Nessuna email selezionata");
            return;
        }

        // Thread per gestire l'eliminazione dal server
        new Thread(() -> {
            try {
                Socket deleteMailSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                openSockets.add(deleteMailSocket);

                BufferedReader in = new BufferedReader(new InputStreamReader(deleteMailSocket.getInputStream()));
                PrintWriter out = new PrintWriter(deleteMailSocket.getOutputStream(), true);

                String emailId = selectedEmail.getId();
                String request = String.join("|", "DELETE_EMAIL", clientStorage.getUserEmail(), emailId);
                out.println(request);

                String response = in.readLine();
                try {
                    openSockets.remove(deleteMailSocket);
                    deleteMailSocket.close();
                } catch (IOException e) {
                    Platform.runLater(() -> AlertNotification.errorConnection("Errore nella chiusura socket per richiesta Eliminazione email: " + e.getMessage()));
                }
                boolean success = response.contains("SUCCESS");
                Platform.runLater(() -> {
                    clientStorage.removeEmail(emailId);
                });
                Platform.runLater(() -> {
                    if (success) {
                        Platform.runLater(() -> AlertNotification.showResponsetoSendMessage("Email eliminata", "L'email è stata eliminata con successo."));
                    } else {
                        Platform.runLater(() -> AlertNotification.showResponsetoSendMessage("Errore", "Errore durante l'eliminazione dell'email."));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertNotification.errorConnection("Errore di comunicazione con il server."));
            }
        }).start();
    }

    @FXML
    private void handleForwardEmail() {
        // Ottieni l'email selezionata dalla ListView
        Email selectedEmail = inboxListView.getSelectionModel().getSelectedItem();
        if (selectedEmail == null) {
            AlertNotification.emptyField("Nessuna email selezionata");
            return;
        }
        try {
            // Carica la vista WriteMail
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/writeMail-view.fxml"));
            Parent root = loader.load();

            // Ottieni il controller della nuova vista
            WriteMailController writeMailController = loader.getController();
            writeMailController.setUserMail(clientStorage.getUserEmail());

            // Pre-compila i campi della nuova vista
            String forwardSubject = "Fwd: " + selectedEmail.getSubject(); // Oggetto con prefisso "Fwd: "
            String originalMessage = String.format("""
                    
                    --- Messaggio originale ---
                    Da: %s
                    A: %s
                    Oggetto: %s
                    Testo: %s""", selectedEmail.getSender(), selectedEmail.getRecipientsAsString(), selectedEmail.getSubject(), selectedEmail.getBody());


            writeMailController.initFields("", forwardSubject, originalMessage); // Il destinatario è vuoto

            // Cambia scena o mostra un nuovo stage
            Stage newStage = new Stage();
            newStage.setScene(new Scene(root));
            newStage.setTitle("Inoltra messaggio");
            newStage.show();
        } catch (IOException e) {
            Platform.runLater(() -> AlertNotification.showResponsetoSendMessage("Errore", "Errore nel caricamento della vista di inoltro."));
        }
    }

    @FXML
    private void handleLogout() {
        safeClosing();
        // Chiude la finestra attuale
        Stage currentStage = (Stage) menuButton.getScene().getWindow();
        currentStage.close();

        // Ritorna alla schermata di login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/prog3/client/login-view.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(root));
            loginStage.show();
        } catch (IOException e) {
            AlertNotification.showResponsetoSendMessage("Errore", "Errore nel caricamento della schermata di login.");
        }
    }

    @FXML
    private void updateConnectionStatus() {
        openSockets.addListener((ListChangeListener<Socket>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Platform.runLater(() -> {
                        statoConnessione.setText("Connesso");
                        statoConnessione.setStyle("-fx-text-fill: green;");
                    });
                }
                if (change.wasRemoved()) {
                    Platform.runLater(() -> {
                        statoConnessione.setText("Non connesso");
                        statoConnessione.setStyle("-fx-text-fill: red;");
                    });
                }
            }
        });
    }

    private static Email reconstructSingleEmail(String email) {
        String[] emailParts = email.split("\\|");
        String id = emailParts[0];
        String sender = emailParts[1];
        List<String> receivers = Arrays.asList(emailParts[2].split("#"));
        String subject = emailParts[3];
        String body = emailParts[4].replace("\\n", "\n").replace("\\|", "|");
        String cleanedTimestamp = emailParts[5].replace("'", "").trim();
        LocalDateTime timestamp = LocalDateTime.parse(cleanedTimestamp);

        return new Email(id, sender, receivers, subject, body, timestamp);
    }

    private void safeClosing() {
        Platform.runLater(() -> {
            Stage stage = (Stage) menuButton.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (emailChecker != null && !emailChecker.isShutdown()) {
                    emailChecker.close();
                }
                // Close all sockets
                synchronized (openSockets) {
                    for (Socket socket : openSockets) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing socket: " + e.getMessage());
                        }
                    }
                    openSockets.clear();
                }
            });
        });
    }
}

