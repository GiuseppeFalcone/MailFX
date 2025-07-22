package com.prog3.server.controller;

import com.prog3.server.model.*;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;
import java.net.ServerSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ServerController {
    @FXML
    private TextArea logDetailsArea;
    @FXML
    private ListView<String> logListView;
    @FXML
    private Button startServerBtn;
    @FXML
    private Button stopServerBtn;
    @FXML
    private Button reloadStorageBtn;

    private final ObservableList<String> logEntries = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private boolean running;
    private ServerStorage serverStorage;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    /**
     * This method initializes essential dependencies for ServerController Class.
     * ServerStorage is the main Model Class, to which the path of the permanent storage file "storage.csv" is passed
     * Listeners are added to each user mailbox, when a new mail is added or removed from the mailbox the log is updated
     * Cached Thread Pool is created to handle each new client requests.
     */
    @FXML
    private void initialize() {
        initView();
        initServerStorage();
        addListenersToMailboxes();
        safeClosing();
    }

    private void initServerStorage() {
        try {
            serverStorage = new ServerStorage("src/main/java/com/prog3/server/model/data/storage.csv");
        } catch (Exception e) {
            appendLog("Error initializing server storage. Please try again." + e.getMessage());
            reloadStorageBtn.setDisable(false);
        }
    }

    /**
     * Initiate ListView items and adds listeners to it
     */
    private void initView() {
        logListView.setItems(logEntries);
        // Add a listener to update the TextArea when a log entry is selected
        logListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            logDetailsArea.setText(newValue != null ? newValue : "");
        });
        startServerBtn.setDisable(false);
        stopServerBtn.setDisable(true);
        reloadStorageBtn.setDisable(true);
    }

    /**
     * It starts the server to accept new connections, and distributes new requests from clients on different threads
     */
    @FXML
    protected void startServer() {
        startServerBtn.setDisable(true);
        stopServerBtn.setDisable(false);
        appendLog("Starting server...");
        appendLog("Users: " + serverStorage.listUsers());

        if (threadPool == null || threadPool.isShutdown() || threadPool.isTerminated()) {
            threadPool = Executors.newCachedThreadPool(); // Ricrea il threadPool
        }

        new Thread(() -> {
            try {
                appendLog("Waiting for connections...");
                serverSocket = new ServerSocket(8082);      // Opens the socket for connections
                running = true;
                appendLog("Server started on port 8082");

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();        // accepts new connections
                        appendLog("Opened connection");

                        // starts new thread to run singleClientController instance
                        threadPool.submit(new SingleClientController(clientSocket, serverStorage, this));
                    } catch (IOException ioException) {
                        // if the server is still accepting connections, and an IOException is thrown, the log is updated
                        if (running) {
                            appendLog("Error accepting client connection: " + ioException.getMessage());
                        }
                    }
                }
            } catch (Exception exception) {
                appendLog("Error: " + exception.getMessage());
            } finally {
                stopThreadPool();
            }
        }).start();
    }

    /**
     * It closes the serverSocket and stops the running threads that handle the single client request
     */
    @FXML
    protected void stopServer() {
        appendLog("Stopping server...");
        running = false; // Setting the flag to false so that the loop stops

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Close the server socket
            }
        } catch (IOException e) {
            appendLog("Error stopping the server: " + e.getMessage());
        } finally {
            stopThreadPool();
            appendLog("Server stopped successfully.");
            startServerBtn.setDisable(false);
            stopServerBtn.setDisable(true);
        }
    }

    /**
     * This method shutdown the threadPool threads that are running instances of SingleClientController class
     */
    private void stopThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

    @FXML
    private void reloadStorage() {
        try {
            serverStorage.reloadFromFile();
        } catch (Exception e) {
            appendLog("Error reloading storage: " + e.getMessage());
        }
    }


    private void addListenersToMailboxes() {
        for (User user : serverStorage.getUsers().values()) {
            listenToMailbox(user);
        }
    }

    /**
     * Method to add Listeners to the mailbox of each user passed as parameter, when it is updated, the log will be updated
     *
     * @param user given user, which mailbox will be listened by the serverController
     */
    private void listenToMailbox(User user) {
        ObservableList<Email> userObservableList = user.getUserMailboxObservableList();

        Platform.runLater(() -> {
            userObservableList.addListener((ListChangeListener<? super Email>) (change) -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(email ->
                                appendLog("Email added to " + user.getEmail() + "'s inbox: " + email.toString()));
                    }
                    if (change.wasRemoved()) {
                        change.getRemoved().forEach(email ->
                                appendLog("Email removed from " + user.getEmail() + "'s inbox: " + email.toString()));
                    }
                }
            });
        });
    }

    /**
     * Method that puts on the logArea the String passed as parameter
     *
     * @param message String passed that will show on the text Area
     */
    public void appendLog(String message) {
        if (logEntries.size() > 10000) {
            logEntries.removeFirst();
        }
        // Adding time to the message
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("[%s] %s", timestamp, message);

        // Add the log entry to the ListView
        Platform.runLater(() -> {
            logEntries.add(logEntry);
            logListView.scrollTo(logEntries.size() - 1);
        });
    }

    private void safeClosing() {
        // Add a shutdown hook for server cleanup
        Platform.runLater(() -> {
            stopServerBtn.getScene().getWindow().setOnCloseRequest(event -> {
                stopServer(); // Stop the server
            });
        });
    }
}

class SingleClientController implements Runnable {
    private final Socket clientSocket;      // Socket for connection to the client
    private final ServerStorage serverStorage;
    private final ServerController serverController;

    /**
     * Constructor for the SingleClientController class
     *
     * @param clientSocket     the socket through which the client is connected
     * @param serverStorage    the main Model class
     * @param serverController the main Controller class
     */
    public SingleClientController(Socket clientSocket, ServerStorage serverStorage, ServerController serverController) {
        this.clientSocket = clientSocket;
        this.serverStorage = serverStorage;
        this.serverController = serverController;

    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String request; // any request is passed as a String

            while ((request = in.readLine()) != null) {
                serverController.appendLog("Received: " + request);
                handleRequest(request, out);
            }
        } catch (IOException e) {
            serverController.appendLog("Client connection error: " + e.getMessage());
        } finally {
            try {
                serverController.appendLog("Connection closed");
                clientSocket.close();
            } catch (IOException e) {
                serverController.appendLog("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Method that handles any request and dispatches them via a switch construct if the client mail is in the server
     * storage map.
     *
     * @param requestString incoming request string
     * @param out           the PrintWriter to send response to the client
     */
    private void handleRequest(String requestString, PrintWriter out) {
        try {
            String[] requestParts = requestString.split("\\|");

            if (requestParts.length < 2) {
                throw new Exception("Invalid request: " + requestString + " from: " + clientSocket.getLocalAddress());
            }

            String requestType = requestParts[0];
            String clientMail = requestParts[1];

            boolean authorized = isAuthorized(clientMail);
            // Handle request based on type
            if (authorized) {
                switch (requestType) {
                    case "LOGIN":
                        handleLogin(clientMail, out);
                        break;
                    case "GET_ALL_MESSAGES":
                        handleGetAllMessages(clientMail, out);
                        break;
                    case "GET_NEW_MESSAGES":
                        handleGetNewMessages(clientMail, requestParts, out);
                        break;
                    case "SEND_EMAIL":
                        handleSendEmail(clientMail, requestParts, out);
                        break;
                    case "DELETE_EMAIL":
                        handleDeleteEmail(clientMail, requestParts, out);
                        break;
                    default:
                        serverController.appendLog("Invalid request type: " + requestType);
                        out.println(createResponse("ERROR", "Unknown request type"));
                        break;
                }
            } else {
                serverController.appendLog("REQUEST BY NON REGISTERED CLIENT: " + clientMail);
                out.println(createResponse("ERROR", "Client not authorized"));
            }
        } catch (Exception e) {
            serverController.appendLog("Error handling request: " + e.getMessage());
            out.println(createResponse("ERROR", "Invalid request format"));
        }
    }

    /**
     * This method handles the login request by clients, and since the client mail has been already checked, it can only
     * be a successful request
     *
     * @param clientMail the mail of the requesting client
     * @param out        the output socket stream
     */
    private void handleLogin(String clientMail, PrintWriter out) {
        serverController.appendLog("SUCCESSFUL LOGIN Request by: " + clientMail);
        out.println(createResponse("SUCCESS", "Login successful"));
    }

    private void handleGetAllMessages(String clientMail, PrintWriter out) {
        serverController.appendLog("Handling GET_ALL_MESSAGES from: " + clientMail);
        try {
            String allUserEmails = serverStorage.getAllUserEmails(clientMail);
            if (allUserEmails == null) {
                throw new Exception("Error in retrieving all messages of: " + clientMail);
            } else if (allUserEmails.isEmpty()) {
                serverController.appendLog("Sending 0 messages to: " + clientMail);
                out.println(createResponse("SUCCESS", "No messages found"));
            } else {
                serverController.appendLog("Sending all messages to: " + clientMail + ": " + allUserEmails);
                out.println(createResponse("SUCCESS", allUserEmails));
            }
        } catch (Exception e) {
            serverController.appendLog("Error handling GET_ALL_MESSAGES request from client: " + clientMail + "Error: " + e.getMessage());
            out.println(createResponse("ERROR", "Error handling GET_ALL_MESSAGES request"));
        }
    }

    private void handleGetNewMessages(String clientMail, String[] requestArray, PrintWriter out) {
        try {
            serverController.appendLog("Handling GET_NEW_MESSAGES from: " + clientMail);
            String fromDate = requestArray[2];
            String newMessages = serverStorage.getNewUserEmails(clientMail, fromDate);

            if (newMessages == null) {
                throw new Exception("Error in retrieving new messages for: " + clientMail);
            } else if (newMessages.isEmpty()) {
                serverController.appendLog("No New Messages for: " + clientMail);
                out.println(createResponse("SUCCESS", "No messages found"));
            } else {
                serverController.appendLog("Sending New Messages to: " + clientMail);
                out.println(createResponse("SUCCESS", newMessages));
            }
        } catch (Exception e) {
            serverController.appendLog("Error handling GET_NEW_MESSAGES request from client: " + clientMail + "Error: " + e.getMessage());
            out.println(createResponse("ERROR", "Invalid date format"));
        }
    }

    private void handleSendEmail(String clientMail, String[] requestArray, PrintWriter out) {
        try {
            serverController.appendLog("Handling SEND_EMAIL from: " + clientMail + " to: " + requestArray[2]);
            List<String> recipients = Arrays.asList(requestArray[2].split("#"));
            String subject = requestArray[3];
            String body = requestArray[4];
            LocalDateTime date = LocalDateTime.parse(requestArray[5]);

            boolean success = true;
            for (String recipient : recipients) success &= serverStorage.userExists(recipient);

            if (success) {
                for (String recipient : recipients) {
                    Email newEmail = new Email(clientMail, recipients, subject, body, date);
                    serverStorage.addEmailToMailbox(recipient, newEmail);
                }
                serverController.appendLog("Email from: " + clientMail);
                out.println(createResponse("SUCCESS", "Email sent"));
            } else {
                serverController.appendLog("Error in sending email from: " + clientMail + ", one or more recipients are not in the users list");
                out.println(createResponse("ERROR", "Email not sent, One ore more recipients are not in the users list"));
            }
        } catch (Exception e) {
            serverController.appendLog("Error sending email: " + e.getMessage());
            out.println(createResponse("ERROR", "Failed to send email"));
        }
    }

    private void handleDeleteEmail(String clientMail, String[] requestArray, PrintWriter out) {
        try {
            serverController.appendLog("Handling DELETE_EMAIL from: " + clientMail);
            String emailId = requestArray[2];
            boolean removed = serverStorage.removeEmailFromMailbox(clientMail, emailId);

            if (removed) {
                serverController.appendLog("Successfully deleted email from: " + clientMail);
                out.println(createResponse("SUCCESS", "Email deleted successfully"));
            } else {
                throw new Exception("Email Id not found!");
            }
        } catch (IllegalArgumentException exception) {
            serverController.appendLog("Error deleting email from client with mail" + clientMail + ", Wrong emailId: " + requestArray[2] + ", " + exception.getMessage());
            out.println(createResponse("ERROR", "Invalid emailId"));
        } catch (Exception exception) {
            serverController.appendLog("Error deleting email: " + exception.getMessage());
            out.println(createResponse("ERROR", "Failed to delete email: " + exception.getMessage()));
        }
    }

    private boolean isAuthorized(String clientMail) {
        return serverStorage.userExists(clientMail);
    }

    private String createResponse(String status, String message) {
        return String.join("|", status, message);
    }
}
